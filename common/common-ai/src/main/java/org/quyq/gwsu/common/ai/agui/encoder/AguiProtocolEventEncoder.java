package org.quyq.gwsu.common.ai.agui.encoder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.util.JacksonJsonCodec;
import io.agentscope.core.util.JsonCodec;

/**
 * 符合 AG-UI 1.0 可选字段语义的事件编码器。
 *
 * <p>AgentScope 2.0.4 的默认编码器会把嵌套对象中的可选空属性编码为 {@code null}，
 * 但 AG-UI 1.0 的运行时 schema 要求这些字段不存在或为对应类型。这里仅调整属性包含策略，
 * 事件模型及事件转换仍使用 AgentScope 官方实现。</p>
 */
public final class AguiProtocolEventEncoder {

    private static final JsonCodec JSON_CODEC = createJsonCodec();

    public String encodeToJson(AguiEvent event) {
        // 与官方 AguiEventEncoder 保持一致，保留 data: 后的前导空格。
        return " " + JSON_CODEC.toJson(event);
    }

    private static JsonCodec createJsonCodec() {
        ObjectMapper objectMapper = new JacksonJsonCodec().getObjectMapper();
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(
                JsonInclude.Include.NON_NULL,
                JsonInclude.Include.ALWAYS));
        return new JacksonJsonCodec(objectMapper);
    }
}
