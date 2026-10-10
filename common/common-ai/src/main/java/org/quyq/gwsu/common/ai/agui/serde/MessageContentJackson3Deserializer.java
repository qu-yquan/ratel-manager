package org.quyq.gwsu.common.ai.agui.serde;

import io.agentscope.core.agui.model.InputContent;
import io.agentscope.core.agui.model.MessageContent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.List;

/**
 * AgentScope AG-UI {@link MessageContent} 的 Jackson 3 反序列化器。
 *
 * <p>AgentScope 2.0.4 自带的实现基于 Jackson 2 databind，Spring Boot 4 使用 Jackson 3，
 * 因此需要在应用侧注册等价实现。</p>
 */
public final class MessageContentJackson3Deserializer extends ValueDeserializer<MessageContent> {

    private static final TypeReference<List<InputContent>> INPUT_CONTENT_LIST =
            new TypeReference<>() {
            };

    @Override
    public MessageContent deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return new MessageContent.Text(parser.getValueAsString());
        }
        if (token == JsonToken.START_ARRAY) {
            return new MessageContent.Blocks(parser.readValueAs(INPUT_CONTENT_LIST));
        }
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        return context.reportInputMismatch(
                MessageContent.class,
                "AG-UI message content 仅支持字符串、内容块数组或 null，实际 token: %s",
                token);
    }
}
