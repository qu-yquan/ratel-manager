package org.quyq.gwsu.headless.graph;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;

import java.util.Map;

/**
 * 无头浏览器 AG-UI 事件工厂。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeadlessAguiEvents {

    public static AguiEvent.Custom status(String threadId, String runId, HeadlessAgentStatus status) {
        return new AguiEvent.Custom(threadId, runId, "status", Map.of("status", status.name()));
    }

    public static AguiEvent.Raw raw(String threadId, String runId, String message) {
        return new AguiEvent.Raw(threadId, runId, Map.of(
                "source", "headless",
                "message", message == null ? "" : message));
    }

    public static AguiEvent.Raw raw(String threadId, String runId, String code, String message) {
        return new AguiEvent.Raw(threadId, runId, Map.of(
                "source", "headless",
                "code", code,
                "message", message == null ? "" : message));
    }
}
