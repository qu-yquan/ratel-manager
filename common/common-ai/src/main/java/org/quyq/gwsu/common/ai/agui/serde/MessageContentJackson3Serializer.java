package org.quyq.gwsu.common.ai.agui.serde;

import io.agentscope.core.agui.model.InputContent;
import io.agentscope.core.agui.model.MessageContent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/** AgentScope AG-UI {@link MessageContent} 的 Jackson 3 序列化器。 */
public final class MessageContentJackson3Serializer extends ValueSerializer<MessageContent> {

    @Override
    public void serialize(
            MessageContent value,
            JsonGenerator generator,
            SerializationContext context) throws JacksonException {
        if (value instanceof MessageContent.Text text) {
            generator.writeString(text.value());
            return;
        }
        if (value instanceof MessageContent.Blocks blocks) {
            generator.writeStartArray(value, blocks.parts().size());
            for (InputContent part : blocks.parts()) {
                context.writeValue(generator, part);
            }
            generator.writeEndArray();
            return;
        }
        context.reportBadDefinition(
                MessageContent.class,
                "不支持的 AG-UI message content 类型: " + value.getClass().getName(),
                null);
    }
}
