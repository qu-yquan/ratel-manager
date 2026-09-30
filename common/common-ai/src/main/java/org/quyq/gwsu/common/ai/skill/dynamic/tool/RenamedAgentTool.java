package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

final class RenamedAgentTool implements AgentTool {

    private final String name;
    private final AgentTool delegate;

    RenamedAgentTool(String name, AgentTool delegate) {
        this.name = Objects.requireNonNull(name);
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public Map<String, Object> getParameters() {
        return delegate.getParameters();
    }

    @Override
    public Boolean getStrict() {
        return delegate.getStrict();
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return delegate.getOutputSchema();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return delegate.callAsync(param)
                .map(result -> result.withIdAndName(result.getId(), name));
    }
}
