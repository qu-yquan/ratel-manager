package org.quyq.gwsu.common.ai.skill.dynamic.registry;

public record DynamicSkillToolkitChangedEvent(
        String previousRevision,
        String currentRevision
) {
}
