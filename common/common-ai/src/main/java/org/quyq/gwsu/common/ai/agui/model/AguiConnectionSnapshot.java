package org.quyq.gwsu.common.ai.agui.model;

import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;

import java.util.List;
import java.util.Map;

/**
 * AG-UI 线程连接快照。
 *
 * <p>{@code agent/connect} 使用该对象一次性恢复消息、前端共享状态、待处理
 * interrupt 以及业务自定义事件。业务模块负责构造快照，common-ai 只负责按照
 * AG-UI 协议顺序发送。</p>
 */
public record AguiConnectionSnapshot(
        List<AguiMessage> messages,
        Map<String, Object> state,
        List<AguiEvent.Interrupt> interrupts,
        List<AguiEvent.Custom> customEvents) {

    public AguiConnectionSnapshot {
        messages = messages == null ? List.of() : List.copyOf(messages);
        state = state == null ? Map.of() : Map.copyOf(state);
        interrupts = interrupts == null ? List.of() : List.copyOf(interrupts);
        customEvents = customEvents == null ? List.of() : List.copyOf(customEvents);
    }

    public static AguiConnectionSnapshot empty() {
        return new AguiConnectionSnapshot(List.of(), Map.of(), List.of(), List.of());
    }
}
