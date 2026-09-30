package org.quyq.gwsu.common.ai.skill.dynamic.model;

import io.agentscope.core.tool.AgentTool;

public record NormalizedSkillTool(
        String originalName,
        String exposedName,
        String ownerClassShortName,
        AgentTool localTool,
        RegisteredToolManifest manifest
) {
}
