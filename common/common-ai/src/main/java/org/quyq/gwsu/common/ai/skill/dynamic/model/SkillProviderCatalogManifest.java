package org.quyq.gwsu.common.ai.skill.dynamic.model;

import java.util.List;

public record SkillProviderCatalogManifest(
        String applicationName,
        String catalogRevision,
        String toolkitRevision,
        List<SkillRegistrationManifest> registrations,
        List<StandaloneToolManifest> standaloneTools
) {
    public SkillProviderCatalogManifest {
        registrations = registrations == null ? List.of() : List.copyOf(registrations);
        standaloneTools = standaloneTools == null ? List.of() : List.copyOf(standaloneTools);
    }
}
