package org.quyq.gwsu.common.ai.skill.dynamic.repository;

import org.quyq.gwsu.common.ai.skill.dynamic.SkillResourceContext;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogEntry;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceManifest;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.skill.SkillResources;

import java.util.List;
import java.util.Optional;

final class DynamicSkillResources implements SkillResources {

    private final SkillCatalogEntry entry;
    private final RuntimeContext runtimeContext;
    private final DynamicSkillResourceService resourceService;

    DynamicSkillResources(
            SkillCatalogEntry entry,
            RuntimeContext runtimeContext,
            DynamicSkillResourceService resourceService) {
        this.entry = entry;
        this.runtimeContext = runtimeContext;
        this.resourceService = resourceService;
    }

    @Override
    public Optional<String> read(String path) {
        return resourceService.readText(entry, path, new SkillResourceContext(runtimeContext));
    }

    @Override
    public Optional<byte[]> readBinary(String path) {
        return resourceService.readBinary(entry, path, new SkillResourceContext(runtimeContext));
    }

    @Override
    public List<String> list() {
        return entry.manifest().resources().stream()
                .map(SkillResourceManifest::path)
                .toList();
    }
}
