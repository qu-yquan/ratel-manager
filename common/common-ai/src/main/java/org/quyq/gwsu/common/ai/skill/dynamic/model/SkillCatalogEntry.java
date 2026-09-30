package org.quyq.gwsu.common.ai.skill.dynamic.model;

import io.agentscope.core.skill.AgentSkill;

public record SkillCatalogEntry(
        SkillRegistrationManifest manifest,
        AgentSkill agentSkill
) {
}
