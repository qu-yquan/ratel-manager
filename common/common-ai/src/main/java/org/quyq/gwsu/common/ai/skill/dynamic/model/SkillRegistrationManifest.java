package org.quyq.gwsu.common.ai.skill.dynamic.model;

import java.util.List;

public record SkillRegistrationManifest(
        String applicationName,
        String skillId,
        String description,
        String version,
        String contentKey,
        String requiredPermission,
        List<SkillResourceManifest> resources,
        List<RegisteredToolManifest> tools
) {
    public SkillRegistrationManifest {
        resources = resources == null ? List.of() : List.copyOf(resources);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
