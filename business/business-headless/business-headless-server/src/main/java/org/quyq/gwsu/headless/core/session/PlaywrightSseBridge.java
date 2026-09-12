package org.quyq.gwsu.headless.core.session;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Playwright 页面与 Java 之间的 SSE 流桥接器。
 *
 * <p>桥接器在页面脚本运行前包装 {@code window.fetch}，复制目标 SSE 响应的
 * {@code ReadableStream} 并逐事件回调 Java。原始响应保持不变，仍由前端正常消费。</p>
 */
@Slf4j
final class PlaywrightSseBridge implements AutoCloseable {

    private static final String EVENT_CALLBACK_NAME = "__gwsuHeadlessSseEvent";
    private static final String SIGNAL_CALLBACK_NAME = "__gwsuHeadlessSseSignal";
    private static final String INTERCEPTOR_RESOURCE = "headless/sse-fetch-interceptor.js";

    private final List<AutoCloseable> registrations = new ArrayList<>();

    PlaywrightSseBridge(Page page, Listener listener) {
        Objects.requireNonNull(page, "page cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        try {
            registrations.add(page.exposeFunction(EVENT_CALLBACK_NAME, args -> {
                if (args.length < 2) {
                    log.warn("[HeadlessSSE] 页面事件回调参数不完整");
                    return null;
                }
                listener.onEvent(stringArg(args, 0), stringArg(args, 1));
                return null;
            }));
            registrations.add(page.exposeFunction(SIGNAL_CALLBACK_NAME, args -> {
                if (args.length < 6) {
                    log.warn("[HeadlessSSE] 页面生命周期回调参数不完整");
                    return null;
                }
                listener.onSignal(new StreamSignal(
                        stringArg(args, 0),
                        SignalType.from(stringArg(args, 1)),
                        stringArg(args, 2),
                        stringArg(args, 3),
                        intArg(args, 4),
                        stringArg(args, 5)
                ));
                return null;
            }));
            registrations.add(page.addInitScript(loadInterceptorScript()));
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    private static String loadInterceptorScript() {
        ClassLoader classLoader = PlaywrightSseBridge.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(INTERCEPTOR_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("找不到 Playwright SSE 拦截脚本: " + INTERCEPTOR_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取 Playwright SSE 拦截脚本失败", e);
        }
    }

    private static String stringArg(Object[] args, int index) {
        Object value = args[index];
        return value == null ? "" : value.toString();
    }

    private static int intArg(Object[] args, int index) {
        Object value = args[index];
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(stringArg(args, index));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public void close() {
        for (int index = registrations.size() - 1; index >= 0; index--) {
            try {
                registrations.get(index).close();
            } catch (Exception e) {
                log.debug("释放 Playwright SSE 桥接资源失败: {}", e.getMessage());
            }
        }
        registrations.clear();
    }

    interface Listener {

        void onEvent(String streamId, String eventJson);

        void onSignal(StreamSignal signal);
    }

    enum SignalType {
        OPEN,
        END,
        ERROR,
        UNKNOWN;

        static SignalType from(String value) {
            try {
                return SignalType.valueOf(value);
            } catch (IllegalArgumentException | NullPointerException e) {
                return UNKNOWN;
            }
        }
    }

    record StreamSignal(
            String streamId,
            SignalType type,
            String threadId,
            String runId,
            int status,
            String message
    ) {
    }
}
