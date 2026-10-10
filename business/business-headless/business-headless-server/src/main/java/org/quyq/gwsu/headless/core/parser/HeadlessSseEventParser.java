package org.quyq.gwsu.headless.core.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.microsoft.playwright.Response;
import lombok.extern.slf4j.Slf4j;
import io.agentscope.core.agui.event.AguiEvent;

import java.util.Map;

/**
 * SSE 事件解析器
 *
 * 将 SSE 原始 JSON 解析为 AguiEvent 各子类型，
 * 与后端 AguiController 发送事件的序列化格式对称。
 *
 * JS 拦截器消费 ReadableStream 后，每解析到一个完整 JSON，
 * 通过 exposeFunction 传入 Java，由本解析器反序列化为 AguiEvent 子类型。
 */
@Slf4j
public class HeadlessSseEventParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析单个 SSE 事件 JSON 为 AguiEvent 子类型
     * 依赖 AguiEvent 上的 Jackson 多态注解自动根据 type 反序列化
     */
    public AguiEvent parseEvent(String eventJson) {
        try {
            return objectMapper.readValue(eventJson, AguiEvent.class);
        } catch (InvalidTypeIdException e) {
            try {
                Map<String, Object> raw = objectMapper.readValue(
                        eventJson,
                        new TypeReference<Map<String, Object>>() {
                        });
                if (raw == null) {
                    return null;
                }

                // 未知事件类型时退化为 Raw，避免上层因协议扩展而立即失效
                return new AguiEvent.Raw(
                        str(raw, "threadId"),
                        str(raw, "runId"),
                        raw
                );
            } catch (Exception ex) {
                log.warn("SSE 未知事件兜底解析失败: {}", ex.getMessage());
                return new AguiEvent.Raw("", "", eventJson);
            }
        } catch (Exception e) {
            log.warn("SSE 事件 JSON 解析失败: {}", e.getMessage());
            return new AguiEvent.Raw("", "", eventJson);
        }
    }

    /**
     * 判断 HTTP 响应是否为 SSE 流
     */
    public static boolean isSseResponse(Response response) {
        String contentType = response.headers().get("content-type");
        return contentType != null && contentType.contains("text/event-stream");
    }

    private static String str(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        return v != null ? v.toString() : "";
    }
}
