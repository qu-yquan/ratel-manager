package org.quyq.gwsu.headless.core.session;

import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.core.HeadlessAgentListener;
import org.quyq.gwsu.headless.core.parser.HeadlessSseEventParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 无头浏览器用户会话（分布式版本）
 * <p>
 * 核心变更：
 * - 不再持有 userId 作为长期标识
 * - 每次使用时由 HeadlessBrowserManager 创建和销毁
 * - authenticate() 支持双模式：certification 登录 / token 快速恢复
 * - 支持从浏览器提取 token 和 threadId，保存到 Redis
 * <p>
 * SSE 事件接收策略：
 * 1. 页面加载前注入 fetch 拦截器，仅镜像 agent/run 的 SSE 响应
 * 2. 浏览器通过 ReadableStream 实时解析 SSE，并由 exposeFunction 回调 Java
 * 3. Java 持续驱动 Playwright 消息循环，按序分发 AguiEvent 给 listener
 * 4. 收到 RunFinished、流异常或超时后结束本次等待
 * <p>
 * 审批/提问交互策略：
 * - 审批和回答问题由 HeadlessBrowserManager 的 approval()/userAnswer() 独立发起
 * - 通过前端隐藏表单提交，无需操作可见 UI 元素
 * - submitApproval()/submitUserAnswer() 填充隐藏表单并触发提交
 */
@Slf4j
public class HeadlessBrowserSession implements AutoCloseable {

    private static final String SSE_URL_PATTERN = "/brain/run/copilotKit";
    private static final long HEADLESS_REQUEST_TIMEOUT_MS = 30_000;
    private static final long PLAYWRIGHT_EVENT_PUMP_INTERVAL_MS = 100;

    private final BrowserContext context;
    private final Page page;
    private final HeadlessSseEventParser parser = new HeadlessSseEventParser();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlaywrightSseBridge sseBridge;

    /**
     * SSE 等待超时（毫秒），从配置注入
     */
    private final long sseTimeoutMs;

    /**
     * sendMessage 互斥锁，保证同一 Session 不会并发调用
     */
    private final ReentrantLock sendLock = new ReentrantLock();

    /**
     * 串行化同一会话的 Playwright Page 调用。
     * <p>
     * SSE 回调会触发录屏和截图；Playwright Java 不支持这些操作与消息发送并发执行。
     * 录屏帧也由 SSE 事件泵在持锁期间采集，避免跨线程调用 Page。
     */
    private final ReentrantLock pageOperationLock = new ReentrantLock(true);

    /**
     * 当前活跃的事件收集器（每次 sendMessage 重建）
     */
    private final AtomicReference<SseEventCollector> currentEventCollector = new AtomicReference<>();

    /**
     * 当前活跃的监听器（每次 sendMessage 更新）
     */
    private final AtomicReference<HeadlessAgentListener> currentListener = new AtomicReference<>();

    /**
     * 页面操作包装器（供 listener 在事件回调中操作浏览器）
     */
    private final HeadlessPageWrapper pageWrapper;

    /**
     * toolCallId → toolCallName 映射
     */
    private final ConcurrentHashMap<String, String> toolCallNameMap = new ConcurrentHashMap<>();
    /**
     * toolCallId → 累积的 TOOL_CALL_ARGS delta
     */
    private final ConcurrentHashMap<String, StringBuilder> toolCallArgsBuffer = new ConcurrentHashMap<>();

    /**
     * 当前 SSE 会话的 threadId，从流元数据或事件中提取
     */
    private volatile String currentThreadId = null;

    private volatile boolean closed = false;

    /**
     * token 是否已失效标记（由 SSE 请求返回 401 时设置）
     */
    private volatile boolean tokenExpired = false;

    public HeadlessBrowserSession(BrowserContext context, long sseTimeoutMs) {
        this.context = context;
        this.sseTimeoutMs = sseTimeoutMs;

        // 1. 创建 Page
        this.page = context.newPage();

        // 2. 创建页面操作包装器
        this.pageWrapper = new HeadlessPageWrapper(context, page, pageOperationLock);

        // 3. 页面脚本执行前安装 SSE 流桥接器
        this.sseBridge = new PlaywrightSseBridge(page, new PlaywrightSseBridge.Listener() {
            @Override
            public void onEvent(String streamId, String eventJson) {
                handleBrowserSseEvent(streamId, eventJson);
            }

            @Override
            public void onSignal(PlaywrightSseBridge.StreamSignal signal) {
                handleBrowserSseSignal(signal);
            }
        });

        // 3.1 监听 SSE 请求的响应，检测 401 标记 token 失效
        page.onResponse(response -> {
            if (response.url().contains(SSE_URL_PATTERN) && response.status() == 401) {
                this.tokenExpired = true;
                log.warn("[HeadlessSSE] 检测到 401 响应，token 已失效");
            }
        });

        // 4. 转发浏览器控制台日志（调试用）
        page.onConsoleMessage(msg -> {
            String text = msg.text();
            if (text != null && text.startsWith("[Headless")) {
                log.info("[BrowserConsole] {}", text);
            }
        });
    }

    /**
     * 获取 BrowserContext，供 Manager 归还到池
     */
    public BrowserContext getBrowserContext() {
        return context;
    }

    /**
     * 从浏览器 localStorage 提取 token
     */
    public String extractTokenFromBrowser() {
        try {
            Object tokenObj = page.evaluate(
                    "() => { try { const raw = localStorage.getItem('gwsu_token'); if (!raw) return null; const parsed = JSON.parse(raw); return parsed.token || null; } catch { return null; } }"
            );
            return tokenObj != null ? tokenObj.toString() : null;
        } catch (Exception e) {
            log.debug("从浏览器提取 token 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前 SSE 会话的 threadId
     */
    public String extractThreadId() {
        return currentThreadId;
    }

    /**
     * 认证登录
     * <p>
     * 流程：
     * 1. URL 含 token 参数 → 先导航到带 token 的登录页面
     * - 通过 page.onResponse 监听网络请求，检测 /system/manager/current 返回 401
     * - 如果 401 → 清除无效 token，重新导航到只有 certification 的 URL 回退登录
     * - 如果正常 → 等待 data-headless-login-status 变为 success
     * 2. URL 无 token 参数 → 直接 certification 登录
     */
    public void authenticate(String loginUrl) {
        this.tokenExpired = false;  // 重置 token 失效标记
        log.info("开始无头浏览器认证: loginUrl={}", loginUrl);

        boolean hasToken = loginUrl.contains("token=");

        if (hasToken) {
            // 有 token：先尝试 token 恢复，如果 401 则回退 certification
            authenticateWithTokenFallback(loginUrl);
        } else {
            // 无 token：直接 certification 登录
            authenticateDirect(loginUrl);
        }
    }

    /**
     * 带 token 的认证流程，检测 401 后自动回退 certification 登录
     */
    private void authenticateWithTokenFallback(String loginUrl) {
        log.info("检测到 token 参数，尝试 token 恢复会话");

        // 注册响应监听器，检测 fetchCurrentUserInfo 返回 401
        final boolean[] tokenExpired = {false};
        Consumer<Response> responseListener = response -> {
            String rUrl = response.url();
            // 检测获取当前用户信息的接口返回 401
            if (rUrl.contains("/%s/manager/current".formatted(SecurityConstants.Authentication.AUTH_SERVER_PREFIX)) && response.status() == 401) {
                log.warn("[HeadlessAuth] 检测到 token 失效 (401): {}", rUrl);
                tokenExpired[0] = true;
            }
        };
        page.onResponse(responseListener);

        try {
            // 导航到带 token 的登录页面
            page.navigate(loginUrl);
            page.waitForLoadState();

            // 等待前端设置 data-headless-login-status 属性
            page.waitForFunction(
                    "() => document.body && document.body.getAttribute('data-headless-login-status') !== null",
                    null, new Page.WaitForFunctionOptions().setTimeout(30_000));

            // 检查是否 token 失效
            if (tokenExpired[0]) {
                log.trace("[HeadlessAuth] token 已失效，回退到 certification 登录");
                // 清除浏览器中无效的 token
                page.evaluate("() => { localStorage.removeItem('gwsu_token'); localStorage.removeItem('gwsu_isLoggedIn'); }");
                // 构造移除 token 但保留 threadId 的 URL
                String fallbackUrl = stripTokenParam(loginUrl);
                // 回退到 certification 登录
                authenticateDirect(fallbackUrl);
                return;
            }

            // token 有效，检查登录状态
            Object statusObj = page.evaluate("document.body.getAttribute('data-headless-login-status')");
            String status = statusObj != null ? statusObj.toString() : "";
            if ("success".equals(status)) {
                log.trace("token 恢复会话成功");
                return;
            }

            // 其他错误状态（非 401），也回退 certification
            log.warn("[HeadlessAuth] 登录状态异常(status={}), 回退到 certification 登录", status);
            page.evaluate("() => { localStorage.removeItem('gwsu_token'); localStorage.removeItem('gwsu_isLoggedIn'); }");
            String fallbackUrl = stripTokenParam(loginUrl);
            authenticateDirect(fallbackUrl);

        } catch (PlaywrightException e) {
            // 超时也可能是因为 token 失效导致页面跳转
            if (tokenExpired[0]) {
                log.trace("[HeadlessAuth] 超时且 token 已失效，回退到 certification 登录");
                try {
                    page.evaluate("() => { localStorage.removeItem('gwsu_token'); localStorage.removeItem('gwsu_isLoggedIn'); }");
                } catch (Exception ignored) {
                }
                String fallbackUrl = stripTokenParam(loginUrl);
                authenticateDirect(fallbackUrl);
            } else {
                log.error("无头浏览器 token 恢复超时", e);
                throw new RuntimeException("无头浏览器登录超时", e);
            }
        } finally {
            // 移除响应监听器（避免影响后续请求）
            try {
                page.offResponse(responseListener);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 直接 certification 登录（无 token 参数）
     */
    private void authenticateDirect(String loginUrl) {
        log.trace("开始 certification 登录: loginUrl={}", loginUrl);

        page.navigate(loginUrl);
        page.waitForLoadState();

        try {
            page.waitForFunction(
                    "() => document.body && document.body.getAttribute('data-headless-login-status') !== null",
                    null, new Page.WaitForFunctionOptions().setTimeout(30_000));
        } catch (PlaywrightException e) {
            log.error("无头浏览器 certification 登录超时", e);
            throw new RuntimeException("无头浏览器登录超时", e);
        }

        Object statusObj = page.evaluate("document.body.getAttribute('data-headless-login-status')");
        String status = statusObj != null ? statusObj.toString() : "";
        if (!"success".equals(status)) {
            log.error("无头浏览器 certification 登录失败: status={}", status);
            throw new RuntimeException("无头浏览器登录失败: " + status);
        }
        log.trace("certification 登录成功");
    }

    /**
     * 从 URL 中移除 token 参数，保留 certification 和 threadId
     */
    private String stripTokenParam(String url) {
        try {
            var uri = new java.net.URI(url);
            String query = uri.getQuery();
            if (query == null) return url;

            StringBuilder newQuery = new StringBuilder();
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && !"token".equals(kv[0])) {
                    if (newQuery.length() > 0) newQuery.append("&");
                    newQuery.append(param);
                }
            }

            return new java.net.URI(uri.getScheme(), uri.getAuthority(), uri.getPath(),
                    !newQuery.isEmpty() ? newQuery.toString() : null, uri.getFragment()).toString();
        } catch (Exception e) {
            // 解析失败，返回原 URL
            log.warn("解析 URL 失败，返回原 URL: {}", e.getMessage());
            return url;
        }
    }

    // ==================== 发送消息 ====================

    public List<AguiEvent> sendMessage(HeadlessDTO message, HeadlessAgentListener listener) {
        sendLock.lock();
        try {
            toolCallNameMap.clear();
            toolCallArgsBuffer.clear();

            SseEventCollector collector = new SseEventCollector();
            currentEventCollector.set(collector);
            currentListener.set(listener);

            try {
                triggerAssistant(message, collector);
            } catch (Exception e) {
                collector.signalError(e);
                notifyListenerError(e);
                return collector.getEvents();
            }

            awaitSseCompletion(collector);
            return collector.getEvents();
        } finally {
            sendLock.unlock();
        }
    }

    // ==================== 审批/回答问题（通过隐藏表单提交） ====================

    /**
     * 通过隐藏表单提交审批结果，并监听后续 SSE 事件
     * <p>
     * 流程：等待聊天就绪 → 显示表单+填充值 → locator.click() 点击按钮 → 隐藏表单 → 等待 SSE 流完成
     * <p>
     * 关键：使用 page.locator().click()（Playwright 原生 click）而非 page.evaluate() 中的 JS click，
     * 让 Playwright 在点击和请求发起阶段持续调度浏览器事件。
     * <p>
     * 按钮需要处于可见状态才能被 locator.click() 点击，因此通过 page.evaluate() 设置
     * data-headless-forms-visible 属性让 HeadlessSubmitBar 组件显示，点击后自动隐藏。
     *
     * @param approved     是否批准
     * @param rejectReason 拒绝原因（批准时为 null）
     * @param listener     事件监听器，接收提交后的 SSE 事件
     */
    public void submitApproval(boolean approved, String rejectReason, HeadlessAgentListener listener) {
        sendLock.lock();
        try {
            toolCallNameMap.clear();
            toolCallArgsBuffer.clear();

            SseEventCollector collector = new SseEventCollector();
            currentEventCollector.set(collector);
            currentListener.set(listener);

            String result = approved ? "APPROVED" : "REJECTED";
            String reason = rejectReason != null ? rejectReason : "";

            pageOperationLock.lock();
            try {
                // 等待前端聊天就绪
                page.waitForFunction(
                        "() => document.body.getAttribute('data-headless-chat-ready') === 'true'",
                        null, new Page.WaitForFunctionOptions().setTimeout(30_000));
                log.debug("前端聊天已就绪，开始提交审批");

                // 1. 显示 HeadlessSubmitBar 组件 + 填充表单值
                page.evaluate("args => {" +
                        "  document.body.setAttribute('data-headless-forms-visible', 'true');" +
                        "  var r = document.querySelector('[data-testid=\"headless-approval-result\"]');" +
                        "  var re = document.querySelector('[data-testid=\"headless-approval-reject-reason\"]');" +
                        "  if (r) r.value = args[0];" +
                        "  if (re) re.value = args[1];" +
                        "}", new Object[]{result, reason});

                activateSseCollector(collector);
                // click 返回前不能让截图或录屏线程并发操作同一个 Playwright Page。
                page.locator("[data-testid='headless-approval-submit']").click();
            } finally {
                pageOperationLock.unlock();
            }

            log.info("审批结果已提交: result={}, hasRejectReason={}", result, !reason.isEmpty());

            // 等待后续 SSE 流完成
            awaitSseCompletion(collector);
        } catch (Exception e) {
            log.error("提交审批失败", e);
            throw new RuntimeException("提交审批失败", e);
        } finally {
            // 确保隐藏表单（无论成功失败）
            pageOperationLock.lock();
            try {
                try {
                    page.evaluate("() => document.body.removeAttribute('data-headless-forms-visible')");
                } catch (Exception ignored) {
                }
            } finally {
                pageOperationLock.unlock();
                sendLock.unlock();
            }
        }
    }

    /**
     * 通过隐藏表单提交用户回答，并监听后续 SSE 事件
     * <p>
     * 流程：等待聊天就绪 → 显示表单+填充值 → locator.click() 点击按钮 → 隐藏表单 → 等待 SSE 流完成
     * <p>
     * 同 submitApproval，使用 Playwright 原生 locator.click() 驱动请求发起阶段的浏览器事件。
     *
     * @param toolCallId 工具调用 ID，用于关联 AskUserQuestion 工具调用
     * @param answers    问题答案，key 为问题文本，value 为用户回答
     * @param listener   事件监听器，接收提交后的 SSE 事件
     */
    public void submitUserAnswer(String toolCallId, Map<String, String> answers, HeadlessAgentListener listener) {
        sendLock.lock();
        try {
            toolCallNameMap.clear();
            toolCallArgsBuffer.clear();

            SseEventCollector collector = new SseEventCollector();
            currentEventCollector.set(collector);
            currentListener.set(listener);

            String answersJson = objectMapper.writeValueAsString(answers);

            pageOperationLock.lock();
            try {
                // 等待前端聊天就绪
                page.waitForFunction(
                        "() => document.body.getAttribute('data-headless-chat-ready') === 'true'",
                        null, new Page.WaitForFunctionOptions().setTimeout(30_000));
                log.debug("前端聊天已就绪，开始提交用户回答");

                // 1. 显示 HeadlessSubmitBar 组件 + 填充表单值
                page.evaluate("args => {" +
                        "  document.body.setAttribute('data-headless-forms-visible', 'true');" +
                        "  var a = document.querySelector('[data-testid=\"headless-question-answers\"]');" +
                        "  var t = document.querySelector('[data-testid=\"headless-question-tool-call-id\"]');" +
                        "  if (a) a.value = args[0];" +
                        "  if (t) t.value = args[1];" +
                        "}", new Object[]{answersJson, toolCallId});

                activateSseCollector(collector);
                // click 返回前不能让截图或录屏线程并发操作同一个 Playwright Page。
                page.locator("[data-testid='headless-question-submit']").click();
            } finally {
                pageOperationLock.unlock();
            }

            log.info("用户回答已提交: toolCallId={}", toolCallId);

            // 等待后续 SSE 流完成
            awaitSseCompletion(collector);
        } catch (Exception e) {
            log.error("提交用户回答失败", e);
            throw new RuntimeException("提交用户回答失败", e);
        } finally {
            // 确保隐藏表单（无论成功失败）
            pageOperationLock.lock();
            try {
                try {
                    page.evaluate("() => document.body.removeAttribute('data-headless-forms-visible')");
                } catch (Exception ignored) {
                }
            } finally {
                pageOperationLock.unlock();
                sendLock.unlock();
            }
        }
    }

    public void ensureHomePage(String baseUrl) {
        String currentUrl = page.url();
        if (currentUrl == null || currentUrl.isEmpty() || currentUrl.equals("about:blank")) {
            page.navigate(baseUrl);
            page.waitForLoadState();
        }
    }

    /**
     * 释放运行时资源，将 Session 置为可复用状态（不关闭浏览器）
     * <p>
     * 缓存复用时调用此方法而非 close()，保留 BrowserContext 和 Page，
     * 仅清理本次 SSE 回调等运行时状态。
     */
    public void release() {
        if (pageWrapper.isRecording()) {
            pageWrapper.discardRecording();
        }
        toolCallNameMap.clear();
        toolCallArgsBuffer.clear();
        currentEventCollector.set(null);
        currentListener.set(null);
        tokenExpired = false;

        // 关闭可能残留的多余标签页，防止内存泄漏
        try {
            for (Page p : context.pages()) {
                if (p != this.page && !p.isClosed()) {
                    log.debug("关闭残留标签页: {}", p.url());
                    p.close();
                }
            }
        } catch (Exception e) {
            log.warn("清理残留标签页失败: {}", e.getMessage());
        }

        log.debug("Session 已释放运行时资源，可复用: threadId={}", currentThreadId);
    }

    /**
     * 检查 token 是否已失效（SSE 请求返回 401）
     */
    public boolean isTokenExpired() {
        return tokenExpired;
    }

    /**
     * 重置 token 失效标记（重新认证后调用）
     */
    public void resetTokenExpired() {
        this.tokenExpired = false;
    }

    @Override
    public void close() {
        closed = true;
        pageWrapper.markClosed();
        toolCallNameMap.clear();
        toolCallArgsBuffer.clear();
        currentEventCollector.set(null);
        currentListener.set(null);
        sseBridge.close();
        try {
            if (page != null && !page.isClosed()) page.close();
        } catch (Exception e) {
            log.warn("关闭Page异常", e);
        }
        try {
            if (context != null) context.close();
        } catch (Exception e) {
            log.warn("关闭Context异常", e);
        }
    }

    // ==================== 内部实现 ====================

    private void notifyListenerError(Throwable error) {
        HeadlessAgentListener listener = currentListener.get();
        if (listener != null) {
            try {
                listener.onError(error, pageWrapper);
            } catch (Exception e) {
                log.warn("通知监听器错误失败", e);
            }
        }
    }

    private void handleBrowserSseEvent(String streamId, String eventJson) {
        SseEventCollector collector = currentEventCollector.get();
        if (closed || collector == null || collector.isTerminal() || !collector.isActiveStream(streamId)) {
            log.trace("[HeadlessSSE] 忽略非当前流事件: streamId={}", streamId);
            return;
        }

        AguiEvent event = parser.parseEvent(eventJson);
        if (event == null) {
            return;
        }
        if (hasText(event.getThreadId())) {
            currentThreadId = event.getThreadId();
        }
        handleSseEvent(event);
    }

    private void handleBrowserSseSignal(PlaywrightSseBridge.StreamSignal signal) {
        SseEventCollector collector = currentEventCollector.get();
        if (closed || collector == null) {
            return;
        }

        switch (signal.type()) {
            case OPEN -> {
                if (!collector.bindStream(signal.streamId())) {
                    log.trace("[HeadlessSSE] 忽略非当前流打开信号: streamId={}", signal.streamId());
                    return;
                }
                if (hasText(signal.threadId())) {
                    currentThreadId = signal.threadId();
                }
                log.debug("[HeadlessSSE] 开始接收浏览器 SSE 流: streamId={}, threadId={}, runId={}",
                        signal.streamId(), signal.threadId(), signal.runId());
            }
            case END -> {
                if (!collector.isActiveStream(signal.streamId())) {
                    return;
                }
                if (!collector.isTerminal()) {
                    signalCollectorError(collector,
                            new IllegalStateException("SSE 流已结束，但未收到 RunFinished 事件"));
                }
            }
            case ERROR -> {
                if (!collector.bindStream(signal.streamId())) {
                    return;
                }
                String message = hasText(signal.message()) ? signal.message() : "未知 SSE 流错误";
                IllegalStateException error = new IllegalStateException(message);
                if (signal.status() == 401) {
                    tokenExpired = true;
                    collector.signalRetryableError(error);
                    log.warn("[HeadlessSSE] SSE 请求返回 401，等待重新认证后重试");
                } else {
                    signalCollectorError(collector, error);
                }
            }
            case UNKNOWN -> log.warn("[HeadlessSSE] 收到未知流状态: streamId={}", signal.streamId());
        }
    }

    private void signalCollectorError(SseEventCollector collector, Throwable error) {
        if (collector.signalError(error)) {
            notifyListenerError(error);
        }
    }

    /**
     * 持续驱动 Playwright 消息循环，使浏览器中 ReadableStream 的分块回调能够实时进入 Java。
     */
    private void awaitSseCompletion(SseEventCollector collector) {
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(sseTimeoutMs);

        while (!collector.isTerminal()) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                signalCollectorError(collector, new TimeoutError("SSE 流接收超时: " + sseTimeoutMs + "ms"));
                break;
            }

            long remainingMs = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
            Throwable pumpError = null;
            boolean recording = pageWrapper.isRecording();

            pageOperationLock.lock();
            try {
                if (recording) {
                    pageWrapper.captureRecordingFrameIfDue();
                    page.waitForTimeout(Math.min(PLAYWRIGHT_EVENT_PUMP_INTERVAL_MS, remainingMs));
                } else {
                    page.waitForCondition(
                            () -> collector.isTerminal() || pageWrapper.isRecording(),
                            new Page.WaitForConditionOptions().setTimeout(remainingMs));
                }
            } catch (TimeoutError e) {
                if (!collector.isTerminal()) {
                    pumpError = new TimeoutError("SSE 流接收超时: " + sseTimeoutMs + "ms", e);
                }
            } catch (PlaywrightException e) {
                if (!collector.isTerminal()) {
                    pumpError = new IllegalStateException("Playwright SSE 事件循环异常", e);
                }
            } finally {
                pageOperationLock.unlock();
            }

            if (pumpError != null) {
                signalCollectorError(collector, pumpError);
                break;
            }

        }

        Throwable error = collector.getError();
        if (error != null && !collector.isRetryableError()) {
            if (error instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("SSE 流接收失败", error);
        }
    }

    private void activateSseCollector(SseEventCollector collector) {
        page.evaluate("operationId => window.__GWSU_HEADLESS_SSE_OPERATION_ID__ = operationId",
                collector.operationId());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void handleSseEvent(AguiEvent event) {
        if (closed) {
            log.debug("[HeadlessSSE] Session已关闭，忽略事件: {}", event.getClass().getSimpleName());
            return;
        }
        SseEventCollector collector = currentEventCollector.get();
        HeadlessAgentListener listener = currentListener.get();
        if (collector != null) collector.addEvent(event);

        try {
            if (listener == null) {
                return;
            }

            listener.onEvent(event, pageWrapper);

            switch (event) {
                case AguiEvent.RunStarted e -> listener.onRunStarted(e, pageWrapper);
                case AguiEvent.RunFinished e -> listener.onRunFinished(e, pageWrapper);
                case AguiEvent.TextMessageStart e -> listener.onTextMessageStart(e, pageWrapper);
                case AguiEvent.TextMessageContent e -> listener.onTextMessageContent(e.delta(), pageWrapper);
                case AguiEvent.TextMessageEnd e -> listener.onTextMessageEnd(e, pageWrapper);
                case AguiEvent.ToolCallStart e -> {
                    listener.onToolCallStart(e, pageWrapper);
                    toolCallNameMap.put(e.toolCallId(), e.toolCallName());
                    if ("AskUserQuestion".equals(e.toolCallName()))
                        toolCallArgsBuffer.put(e.toolCallId(), new StringBuilder());
                }
                case AguiEvent.ToolCallArgs e -> {
                    listener.onToolCallArgs(e, pageWrapper);
                    StringBuilder argsBuf = toolCallArgsBuffer.get(e.toolCallId());
                    if (argsBuf != null) argsBuf.append(e.delta());
                }
                case AguiEvent.ToolCallEnd e -> {
                    listener.onToolCallEnd(e, pageWrapper);
                    if ("AskUserQuestion".equals(toolCallNameMap.get(e.toolCallId()))) {
                        notifyAskUserQuestion(e.toolCallId());
                    }
                    toolCallNameMap.remove(e.toolCallId());
                    toolCallArgsBuffer.remove(e.toolCallId());
                }
                case AguiEvent.ToolCallResult e -> listener.onToolCallResult(e, pageWrapper);
                case AguiEvent.StateSnapshot e -> listener.onStateSnapshot(e, pageWrapper);
                case AguiEvent.StateDelta e -> listener.onStateDelta(e, pageWrapper);
                case AguiEvent.Custom e -> {
                    String name = e.name();
                    if ("HUMAN_APPROVAL".equals(name)) listener.onHumanApproval(e, pageWrapper);
                    else if ("TOOL_EXECUTE".equals(name)) listener.onToolExecute(e, pageWrapper);
                    else if ("AGENT_OUTPUT".equals(name) || "AGENT_OUTPUT_END".equals(name))
                        listener.onAgentOutput(e, pageWrapper);
                    else listener.onCustomEvent(e, pageWrapper);
                }
                default -> {
                }
            }
        } catch (Exception e) {
            log.error("headless -->事件输出异常", e);
            notifyListenerError(e);
        } finally {
            // RunFinished 的 listener 回调全部执行完毕后，才通知 collector 完成
            // 这确保等待结束时 onRunFinished 已经执行完，session.close() 安全
            if (event instanceof AguiEvent.RunFinished && collector != null) {
                collector.signalCompletion();
            }
        }
    }

    /**
     * 解析 AskUserQuestion 参数并通知监听器
     */
    @SuppressWarnings("unchecked")
    private void notifyAskUserQuestion(String toolCallId) {
        StringBuilder argsBuf = toolCallArgsBuffer.get(toolCallId);

        if (argsBuf != null && !argsBuf.isEmpty()) {
            JsonNode jsonNode = objectMapper.readTree(argsBuf.toString());

            JsonNode questionItem = jsonNode.get("questions");

            if(Objects.isNull(questionItem) || !questionItem.isArray()){
                return;
            }
            List<AskUserQuestionTool.QuestionParam> obj = objectMapper.readValue(questionItem.toString(), new TypeReference<List<AskUserQuestionTool.QuestionParam>>() {
            });

            HeadlessAgentListener listener = currentListener.get();
            if (listener != null) listener.onAskUserQuestion(currentThreadId, toolCallId, obj, pageWrapper);

        }



    }

    private void triggerAssistant(HeadlessDTO message, SseEventCollector collector) {
        pageOperationLock.lock();
        try {
            // 等待前端聊天就绪（历史消息回显完成）
            page.waitForFunction(
                    "() => document.body.getAttribute('data-headless-chat-ready') === 'true' && !!window.__GWSU_HEADLESS_CHAT__ && typeof window.__GWSU_HEADLESS_CHAT__.send === 'function'",
                    null, new Page.WaitForFunctionOptions().setTimeout(HEADLESS_REQUEST_TIMEOUT_MS));
            log.debug("前端聊天已就绪，开始发送消息");

            activateSseCollector(collector);
            dispatchHeadlessMessage(page, objectMapper.convertValue(message, new TypeReference<Map<String, Object>>() {
            }));
            log.debug("助手消息已发送: text={}, resourceCount={}",
                    message.text(),
                    message.resources() == null ? 0 : message.resources().size());
        } catch (Exception e) {
            log.error("触发助手失败", e);
            throw new RuntimeException("触发助手失败", e);
        } finally {
            pageOperationLock.unlock();
        }
    }

    /**
     * 发起前端 Agent 调用，并等待 agent/run 收到响应头。
     * <p>
     * 等待响应头可确保注入的 fetch 拦截器已获取并克隆 SSE 响应；
     * 后续流式分块由 {@link #awaitSseCompletion(SseEventCollector)} 持续驱动。
     */
    static void dispatchHeadlessMessage(Page page, Object payload) {
        page.waitForResponse(response -> isAgentRunRequest(response.request()),
                new Page.WaitForResponseOptions().setTimeout(HEADLESS_REQUEST_TIMEOUT_MS),
                () -> page.evaluate("""
                        payload => {
                            const bridge = window.__GWSU_HEADLESS_CHAT__;
                            if (!bridge || typeof bridge.send !== 'function') {
                                throw new Error('Headless chat bridge is not available');
                            }
                            bridge.send(payload);
                        }
                        """, payload));
    }

    private static boolean isAgentRunRequest(Request request) {
        String postData = request.postData();
        return request.url().contains(SSE_URL_PATTERN)
                && postData != null
                && postData.contains("agent/run");
    }

    // ==================== 事件收集器 ====================

    static class SseEventCollector {
        private final List<AguiEvent> events = Collections.synchronizedList(new ArrayList<>());
        private final String operationId = UUID.randomUUID().toString();
        private volatile boolean terminal;
        private volatile Throwable error;
        private volatile boolean retryableError;
        private volatile String streamId;

        /**
         * 添加事件到列表（不改变终态）
         * <p>
         * 终态由 {@link HeadlessBrowserSession#handleSseEvent} 在
         * listener 回调全部执行完毕后显式调用 {@link #signalCompletion} 触发，
         * 确保等待结束时回调已全部完成，session.close() 安全。
         */
        void addEvent(AguiEvent event) {
            events.add(event);
        }

        /**
         * RunFinished 回调执行完毕后，由 handleSseEvent 调用
         */
        synchronized boolean signalCompletion() {
            if (terminal) {
                return false;
            }
            terminal = true;
            return true;
        }

        synchronized boolean signalError(Throwable t) {
            if (terminal) {
                return false;
            }
            this.error = t;
            this.terminal = true;
            return true;
        }

        synchronized boolean signalRetryableError(Throwable t) {
            if (terminal) {
                return false;
            }
            this.error = t;
            this.retryableError = true;
            this.terminal = true;
            return true;
        }

        synchronized boolean bindStream(String candidateStreamId) {
            if (terminal || !belongsToOperation(candidateStreamId)) {
                return false;
            }
            if (streamId == null) {
                streamId = candidateStreamId;
            }
            return streamId.equals(candidateStreamId);
        }

        boolean isActiveStream(String candidateStreamId) {
            String activeStreamId = streamId;
            return activeStreamId != null && activeStreamId.equals(candidateStreamId);
        }

        private boolean belongsToOperation(String candidateStreamId) {
            return candidateStreamId != null && candidateStreamId.startsWith(operationId + ":");
        }

        String operationId() {
            return operationId;
        }

        boolean isTerminal() {
            return terminal;
        }

        List<AguiEvent> getEvents() {
            synchronized (events) {
                return List.copyOf(events);
            }
        }

        Throwable getError() {
            return error;
        }

        boolean isRetryableError() {
            return retryableError;
        }
    }
}
