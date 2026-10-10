package org.quyq.gwsu.common.ai.agui.serde;

import io.agentscope.core.agui.model.MessageContent;
import tools.jackson.databind.module.SimpleModule;

/** 为 AgentScope 2.0.4 AG-UI 模型补充 Jackson 3 支持。 */
public final class AgentscopeAguiJackson3Module extends SimpleModule {

    public AgentscopeAguiJackson3Module() {
        super("agentscope-agui-jackson3");
        addDeserializer(MessageContent.class, new MessageContentJackson3Deserializer());
        addSerializer(MessageContent.class, new MessageContentJackson3Serializer());
    }
}
