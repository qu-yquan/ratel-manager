package org.quyq.gwsu.common.ai.skill.dynamic.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SkillCatalogSnapshot(
        String catalogRevision,
        String toolkitRevision,
        Map<String, SkillCatalogEntry> skills,
        Map<String, StandaloneToolManifest> standaloneTools
) {
    public SkillCatalogSnapshot {
        skills = Map.copyOf(new LinkedHashMap<>(skills));
        standaloneTools = Map.copyOf(new LinkedHashMap<>(standaloneTools));
    }

    public static SkillCatalogSnapshot empty() {
        return new SkillCatalogSnapshot("", "", Map.of(), Map.of());
    }

    public List<SkillCatalogEntry> activeSkills() {
        return List.copyOf(skills.values());
    }

    public SkillCatalogEntry requiredSkill(String skillId) {
        SkillCatalogEntry entry = skills.get(skillId);
        if (entry == null) {
            throw new IllegalStateException("动态 Skill 不存在或已下线：" + skillId);
        }
        return entry;
    }

    public StandaloneToolManifest requiredStandaloneTool(String toolName) {
        StandaloneToolManifest tool = standaloneTools.get(toolName);
        if (tool == null) {
            throw new IllegalStateException("独立动态工具不存在或已下线：" + toolName);
        }
        return tool;
    }
}
