package org.quyq.gwsu.common.ai.skill.dynamic;

import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRegistrationManifest;

import io.agentscope.core.agent.RuntimeContext;

@FunctionalInterface
public interface SkillVisibilityPolicy {

    boolean isVisible(SkillRegistrationManifest manifest, RuntimeContext context);
}
