package org.quyq.gwsu.headless.core;

import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.config.HeadlessBrowserConfiguration;
import org.quyq.gwsu.headless.core.pool.BrowserContextPool;
import org.quyq.gwsu.headless.core.pool.SessionWrapper;
import org.quyq.gwsu.headless.core.session.HeadlessAccessSession;
import org.quyq.gwsu.headless.core.session.HeadlessBrowserSession;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 无头浏览器管理器（分布式版本）
 * <p>
 * 职责：
 * 1. 管理 BrowserContextPool（预创建浏览器上下文池）
 * 2. 通过 Redis 存储分布式会话状态（token、threadId）
 * 3. 通过分布式锁防止同一用户并发访问
 * 4. 每次调用从池中借用 BrowserContext，用完销毁并补充
 * 5. 登录方式：有 session 时追加 token/threadId，无 session 时 certification 登录
 * <p>
 * 交互方式：
 * - sendMessage：发送普通聊天消息，SSE 流实时推送事件给 listener
 * - approval：提交人工审批结果，通过前端隐藏表单提交
 * - userAnswer：提交用户问题回答，通过前端隐藏表单提交
 */
@Slf4j
public class HeadlessBrowserManager implements AutoCloseable {

    private final HeadlessBrowserConfiguration config;
    private final BrowserContextPool contextPool;
    private final CacheUtils cacheUtils;

    public HeadlessBrowserManager(HeadlessBrowserConfiguration config,
                                  BrowserContextPool contextPool,
                                  CacheUtils cacheUtils) {
        this.config = config;
        this.contextPool = contextPool;
        this.cacheUtils = cacheUtils;

        log.info("HeadlessBrowserManager 初始化完成: maxContexts={}, minIdle={}, headless={}, sessionTtlHours={}",
                config.getMaxContexts(), config.getMinIdle(), config.isHeadless(), config.getSessionTtlHours());
    }

    /**
     * 发送聊天消息
     * <p>
     * 流程：获取分布式锁 → 借用 BrowserContext → 认证登录 → 发送消息 → 收集 SSE 事件 → 保存会话
     * <p>
     * 当 SSE 流中出现 HUMAN_APPROVAL 或 AskUserQuestion 事件时，流会结束，
     * 调用方需根据 listener 回调决定后续操作（调用 approval() 或 userAnswer()）
     *
     * @param userId   用户 ID
     * @param message  消息内容
     * @param listener 事件监听器
     * @return SSE 事件列表
     */
    public void sendMessage(SubjectInfo userId, HeadlessDTO message, HeadlessAgentListener listener) {
        executeWithSession(userId, "发送消息", session -> session.sendMessage(message, listener));
    }

    /**
     * 提交人工审批结果
     * <p>
     * 审批时 SSE 流已断开，此方法独立发起操作：
     * 登录恢复会话 → 等待聊天就绪 → 填充审批隐藏表单 → 点击提交 → 监听后续 SSE 事件
     *
     * @param userId       用户 ID
     * @param approved     是否批准
     * @param rejectReason 拒绝原因（批准时可为 null）
     * @param listener     事件监听器，接收提交后的 SSE 事件
     */
    public void approval(SubjectInfo userId, boolean approved, String rejectReason, HeadlessAgentListener listener) {
        executeWithSession(userId, "审批", session -> {
            session.submitApproval(approved, rejectReason, listener);
            return null;
        });
    }

    /**
     * 提交用户问题回答
     * <p>
     * 回答问题时 SSE 流已断开，此方法独立发起操作：
     * 登录恢复会话 → 等待聊天就绪 → 填充回答隐藏表单 → 点击提交 → 监听后续 SSE 事件
     *
     * @param userId     用户 ID
     * @param toolCallId 工具调用 ID，用于关联 AskUserQuestion 工具调用
     * @param answers    问题答案，key 为问题文本，value 为用户回答
     * @param listener   事件监听器，接收提交后的 SSE 事件
     */
    public void userAnswer(SubjectInfo userId, String toolCallId, Map<String, String> answers, HeadlessAgentListener listener) {
        executeWithSession(userId, "回答问题", session -> {
            session.submitUserAnswer(toolCallId, answers, listener);
            return null;
        });
    }

    // ==================== 公共会话执行模板 ====================

    /**
     * 公共会话执行模板（缓存复用版本）
     * <p>
     * 流程：
     * 1. 获取分布式会话 HeadlessAccessSession
     * 2. 无 threadId → 直接创建新 Session（跳过缓存查找）
     * 3. 有 threadId → 以 userId:threadId 为缓存键查找缓存 Session
     * - 命中且 IDLE → 复用，直接执行操作（跳过认证）
     * - 未命中 → 从 idleQueue 借用 BrowserContext，创建新 Session，认证后执行
     * 4. 操作中发现 401 → 重新认证后重试
     * 5. 操作完成 → release() 释放运行时资源 → 缓存 Session（以 userId:threadId 为键）
     */
    private <T> T executeWithSession(SubjectInfo userId, String actionName, SessionAction<T> action) {
        HeadlessBrowserSession session = null;
        boolean fromCache = false;
        String cacheKey = null;

        try {
            // 1. 获取分布式会话，决定缓存策略
            HeadlessAccessSession accessSession = getAccessSession(userId);

            // 2. 有 threadId → 尝试缓存复用；无 threadId → 直接创建新 Session
            if (accessSession != null && accessSession.threadId() != null) {
                cacheKey = buildCacheKey(userId, accessSession.threadId());
                SessionWrapper wrapper = contextPool.getCachedSession(cacheKey);
                if (wrapper != null) {
                    session = wrapper.getSession();
                    fromCache = true;
                    log.debug("{}复用缓存Session: cacheKey={}", actionName, cacheKey);
                }
            }

            // 3. 缓存未命中，创建新 Session
            if (session == null) {
                var ctx = contextPool.borrowIdle(config.getBorrowTimeoutSeconds(), TimeUnit.SECONDS);
                contextPool.incrementTotal();
                session = new HeadlessBrowserSession(ctx, config.getSseTimeoutMs());

                // 认证
                if (accessSession == null) {
                    accessSession = getAccessSession(userId);
                }
                String loginUrl = buildLoginUrl(userId, accessSession);
                try {
                    session.authenticate(loginUrl);
                } catch (Exception e) {
                    log.error("{}登录失败: userId={}", actionName, userId, e);
                    throw new RuntimeException(actionName + "登录失败: userId=" + userId, e);
                }
            }

            // 4. 执行操作
            T result = action.execute(session);

            // 5. 检测 401，token 失效则重新认证后重试
            if (session.isTokenExpired()) {
                log.warn("{}检测到token失效(401)，重新认证: userId={}", actionName, userId);
                String certificationUrl = buildCertificationLoginUrl(userId);
                try {
                    session.authenticate(certificationUrl);
                } catch (Exception e) {
                    log.error("{}重新认证失败: userId={}", actionName, userId, e);
                    throw new RuntimeException(actionName + "重新认证失败: userId=" + userId, e);
                }
                // 重试操作
                result = action.execute(session);
            }

            // 6. 从浏览器提取会话信息并保存到 Redis，同时更新 cacheKey
            HeadlessAccessSession savedSession = saveAccessSession(userId, session, accessSession);
            if (savedSession != null && savedSession.threadId() != null) {
                cacheKey = buildCacheKey(userId, savedSession.threadId());
            }

            return result;

        } catch (Exception e) {
            // 操作异常，如果是从缓存获取的，移除缓存并销毁
            if (fromCache && session != null) {
                log.warn("{}缓存Session操作异常，移除缓存: cacheKey={}", actionName, cacheKey, e);
                contextPool.removeCachedSession(cacheKey);
                session = null;  // 标记已处理，finally 中不再处理
            }
            throw e;
        } finally {
            if (session != null) {
                if (fromCache) {
                    // 缓存复用：释放运行时资源，标记 IDLE，放回缓存
                    try {
                        session.release();
                    } catch (Exception e) {
                        log.warn("释放缓存Session异常: cacheKey={}", cacheKey, e);
                    }
                    contextPool.markIdle(cacheKey);
                } else {
                    // 新建 Session：release 后放入缓存，标记为 IDLE
                    try {
                        session.release();
                        if (cacheKey != null && contextPool.cacheSession(cacheKey, session)) {
                            contextPool.markIdle(cacheKey);
                        }
                    } catch (Exception e) {
                        log.warn("缓存Session异常，销毁: cacheKey={}", cacheKey, e);
                        if (cacheKey != null) {
                            contextPool.removeCachedSession(cacheKey);
                        }
                    }
                }
            }
        }
    }

    /**
     * 会话操作回调接口
     */
    @FunctionalInterface
    private interface SessionAction<T> {
        T execute(HeadlessBrowserSession session);
    }

    // ==================== 分布式会话管理 ====================

    /**
     * 从 Redis 读取分布式会话
     */
    public HeadlessAccessSession getAccessSession(SubjectInfo userId) {
        String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + getSubjectSign(userId);
        return cacheUtils.withRebel(() -> {
            Map<String, String> map = cacheUtils.hGetAll(key, String.class);
            return HeadlessAccessSession.fromMap(map);
        });
    }

    private String getSubjectSign(SubjectInfo subjectInfo) {
        return StringUtils.hasText(subjectInfo.sign()) ? subjectInfo.sign() + ":" + subjectInfo.userId() : subjectInfo.userId();
    }

    /**
     * 从浏览器提取会话信息并保存到 Redis
     *
     * @return 保存后的 HeadlessAccessSession，用于构建缓存键
     */
    private HeadlessAccessSession saveAccessSession(SubjectInfo userId, HeadlessBrowserSession session, HeadlessAccessSession previousSession) {
        try {
            String token = session.extractTokenFromBrowser();
            String threadId = session.extractThreadId();

            if (token == null && previousSession != null) token = previousSession.token();
            if (threadId == null && previousSession != null) threadId = previousSession.threadId();

            if (token == null) {
                log.warn("无法从浏览器提取 token，跳过保存 session: userId={}", userId.userId());
                return previousSession;
            }

            HeadlessAccessSession newSession = new HeadlessAccessSession(userId.userId(), token, threadId);
            String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + getSubjectSign(userId);

            cacheUtils.withRebel(() -> {
                cacheUtils.hSetAll(key, newSession.toMap());
                cacheUtils.expire(key, config.getSessionTtlHours(), TimeUnit.HOURS);
                return null;
            });

            log.debug("保存分布式会话: userId={}, threadId={}", userId, threadId);
            return newSession;
        } catch (Exception e) {
            log.warn("保存分布式会话失败: userId={}", userId, e);
            return previousSession;
        }
    }

    /**
     * 构造登录 URL
     * - 无 session: certification 登录
     * - 有 session: 追加 token 和 threadId
     */
    private String buildLoginUrl(SubjectInfo userId, HeadlessAccessSession accessSession) {
        String certificationKey = UUID.randomUUID().toString();

        cacheUtils.withRebel(() -> {
            cacheUtils.set(
                    SecurityConstants.Authentication.HEADLESS_LOGIN_CERTIFICATION_CACHE_PREFIX + certificationKey,
                    userId.userId(),
                    2, TimeUnit.MINUTES
            );
            return null;
        });

        StringBuilder url = new StringBuilder(buildLoginUrl());
        url.append("?certification=").append(certificationKey);

        if (accessSession != null && accessSession.token() != null) {
            url.append("&token=").append(accessSession.token());
            if (accessSession.threadId() != null) {
                url.append("&threadId=").append(accessSession.threadId());
            }
        }

        return url.toString();
    }

    private String buildLoginUrl(){
        if(config.getLoginUrl().startsWith("http")){
            return config.getLoginUrl();
        }
        BaseProjectInfoProperties baseProjectInfoProperties = ConfigInfoUtils.getByObject(BaseProjectInfoProperties.CONFIG_KEY, BaseProjectInfoProperties.class);
        return baseProjectInfoProperties.viewBaseUrl() + config.getLoginUrl();
    }


    /**
     * 构造仅 certification 的登录 URL（用于 token 失效后的回退登录）
     */
    private String buildCertificationLoginUrl(SubjectInfo userId) {
        String certificationKey = UUID.randomUUID().toString();

        cacheUtils.withRebel(() -> {
            cacheUtils.set(
                    SecurityConstants.Authentication.HEADLESS_LOGIN_CERTIFICATION_CACHE_PREFIX + certificationKey,
                    userId.userId(),
                    2, TimeUnit.MINUTES
            );
            return null;
        });

        return buildLoginUrl() + "?certification=" + certificationKey;
    }

    /**
     * 构建缓存键：userId:threadId
     * <p>
     * 同一用户不同 threadId 对应不同的缓存 Session，避免会话串扰
     */
    private String buildCacheKey(SubjectInfo userId, String threadId) {
        return getSubjectSign(userId) + ":" + threadId;
    }

    /**
     * 开启新会话
     * <p>
     * 清除 Redis 中的 threadId，同时从缓存中移除旧 Session（强制下次新建）
     *
     * @param userId 用户 ID
     */
    public void newSession(SubjectInfo userId) {
        // 先移除旧缓存 Session（需要旧 threadId 构建缓存键）
        HeadlessAccessSession accessSession = getAccessSession(userId);
        if (accessSession != null && accessSession.threadId() != null) {
            contextPool.removeCachedSession(buildCacheKey(userId, accessSession.threadId()));
        }

        String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + getSubjectSign(userId);
        cacheUtils.withRebel(() -> {
            cacheUtils.hDelete(key, HeadlessAccessSession.HASH_KEY_THREAD_ID);
            return null;
        });
        log.debug("已清除用户 threadId 和缓存 Session，下次将创建新会话: userId={}", userId);
    }

    public void newSession(SubjectInfo userId, String threadId) {
        // 先移除旧缓存 Session（需要旧 threadId 构建缓存键）
        HeadlessAccessSession accessSession = getAccessSession(userId);
        if (accessSession != null && accessSession.threadId() != null) {
            contextPool.removeCachedSession(buildCacheKey(userId, accessSession.threadId()));
        }

        String key = SecurityConstants.Authentication.HEADLESS_ACCESS_SESSION_PREFIX + getSubjectSign(userId);
        cacheUtils.withRebel(() -> {
            cacheUtils.hSet(key, HeadlessAccessSession.HASH_KEY_THREAD_ID, threadId);
            return null;
        });
        log.debug("已设置新会话threadID并清除缓存Session: userId={}", userId);
    }


    /**
     * 获取池中空闲数量
     */
    public int idleContextCount() {
        return contextPool.idleCount();
    }

    @Override
    public void close() {
        log.info("HeadlessBrowserManager 关闭中...");
        contextPool.close();
        log.info("HeadlessBrowserManager 已关闭");
    }
}
