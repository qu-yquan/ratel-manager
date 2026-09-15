package org.quyq.gwsu.log.login.task;

import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.exceptions.CacheException;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.log.login.monitor.LoginTokenMonitor;
import org.quyq.gwsu.log.login.service.ILogLoginService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 扫描登录 Token，并结束已被动失效的认证会话。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "dtt.log.login-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoginTokenExpirationTask {

    private static final String TASK_LOCK = "log:login:token-expiration-task";

    private final CacheUtils cacheUtils;

    private final SecurityUtils securityUtils;

    private final LoginTokenMonitor tokenMonitor;

    private final ILogLoginService loginLogService;

    private final int batchSize;

    private final long scanDelay;

    public LoginTokenExpirationTask(
            CacheUtils cacheUtils,
            SecurityUtils securityUtils,
            LoginTokenMonitor tokenMonitor,
            ILogLoginService loginLogService,
            @Value("${dtt.log.login-log.token-scan-batch-size:200}") int batchSize,
            @Value("${dtt.log.login-log.token-scan-delay:60000}") long scanDelay) {
        this.cacheUtils = cacheUtils;
        this.securityUtils = securityUtils;
        this.tokenMonitor = tokenMonitor;
        this.loginLogService = loginLogService;
        this.batchSize = batchSize;
        this.scanDelay = scanDelay;
    }

    @Scheduled(
            initialDelayString = "${dtt.log.login-log.token-scan-initial-delay:60000}",
            fixedDelayString = "${dtt.log.login-log.token-scan-delay:60000}")
    public void scanExpiredTokens() {
        try {
            cacheUtils.executeWithLock(TASK_LOCK, 0, -1, TimeUnit.SECONDS, this::scanBatch);
        } catch (CacheException exception) {
            log.debug("未获取登录 Token 失效扫描任务锁，本轮扫描已跳过");
        } catch (Exception exception) {
            log.warn("登录 Token 失效扫描任务执行异常，本轮扫描已跳过，异常类型：{}",
                    exception.getClass().getSimpleName());
        }
    }

    void scanBatch() {
        Set<String> tokens = tokenMonitor.getBatch(batchSize);
        long now = System.currentTimeMillis();
        for (String token : tokens) {
            if (!tokenMonitor.isDue(token, now)) {
                continue;
            }
            try {
                String authorizationId = tokenMonitor.getAuthorizationId(token);
                if (!StringUtils.hasText(authorizationId)) {
                    tokenMonitor.remove(token);
                    log.warn("登录 Token 监控元数据不存在，已清理无效索引");
                    continue;
                }
                if (securityUtils.getSubject(token).isPresent()) {
                    tokenMonitor.postpone(token, now + scanDelay);
                    continue;
                }
                if (loginLogService.finishExpiredSession(authorizationId)) {
                    tokenMonitor.remove(token);
                } else {
                    tokenMonitor.postpone(token, now + scanDelay);
                }
            } catch (Exception exception) {
                tokenMonitor.postpone(token, now + scanDelay);
                log.warn("单个登录 Token 失效检查异常，将在下个周期重试，异常类型：{}",
                        exception.getClass().getSimpleName());
            }
        }
    }
}
