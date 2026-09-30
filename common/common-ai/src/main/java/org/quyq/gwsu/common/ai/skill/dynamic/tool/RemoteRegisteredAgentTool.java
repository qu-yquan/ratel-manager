package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import org.quyq.gwsu.common.ai.skill.dynamic.model.RegisteredToolManifest;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.util.Map;

final class RemoteRegisteredAgentTool implements AgentTool {

    private final String applicationName;
    private final String skillId;
    private final RegisteredToolManifest tool;
    private final RemoteSkillToolInvoker invoker;

    RemoteRegisteredAgentTool(
            String applicationName,
            String skillId,
            RegisteredToolManifest tool,
            RemoteSkillToolInvoker invoker) {
        this.applicationName = applicationName;
        this.skillId = skillId;
        this.tool = tool;
        this.invoker = invoker;
    }

    @Override
    public String getName() {
        return tool.exposedName();
    }

    @Override
    public String getDescription() {
        return tool.description();
    }

    @Override
    public Map<String, Object> getParameters() {
        return tool.inputSchema();
    }

    @Override
    public Boolean getStrict() {
        return tool.strict();
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return tool.outputSchema();
    }

    @Override
    public boolean isReadOnly() {
        return tool.readOnly();
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return invoker.invoke(applicationName, skillId, tool, param);
    }
}
