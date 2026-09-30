package org.quyq.gwsu.common.ai.skill.dynamic.model;

public record SkillInstanceHeartbeat(
        String applicationName,
        String instanceId,
        String catalogRevision,
        String toolkitRevision,
        long startedAt,
        long heartbeatAt
) {
}
