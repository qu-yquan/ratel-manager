package org.quyq.gwsu.common.ai.skill.dynamic;

import java.util.Objects;
import java.util.function.Supplier;

public record SkillDefinition(
        String skillId,
        String description,
        String version,
        Supplier<String> contentLoader,
        String requiredPermission
) {
    public SkillDefinition {
        Objects.requireNonNull(skillId, "skillId 不能为空");
        Objects.requireNonNull(description, "description 不能为空");
        Objects.requireNonNull(version, "version 不能为空");
        Objects.requireNonNull(contentLoader, "contentLoader 不能为空");
    }
}
