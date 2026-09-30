package org.quyq.gwsu.common.ai.skill.dynamic.runtime;

import io.agentscope.core.agent.Agent;

import java.util.List;

public record AgentRuntimeSnapshot(
        String toolkitRevision,
        Agent agent,
        List<AutoCloseable> resources
) {
    public AgentRuntimeSnapshot {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    public AgentRuntimeSnapshot(String toolkitRevision, Agent agent) {
        this(toolkitRevision, agent, List.of());
    }
}
