package org.quyq.gwsu.common.ai.skill.dynamic.repository;

import org.quyq.gwsu.common.ai.skill.dynamic.SkillVisibilityPolicy;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogEntry;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.RuntimeContextSkillRepository;
import io.agentscope.harness.agent.skill.LazyResourceCapable;
import io.agentscope.harness.agent.skill.SkillResources;

import java.util.List;

public final class RedisDynamicAgentSkillRepository
        implements RuntimeContextSkillRepository, LazyResourceCapable {

    private final SkillCatalogHolder catalogHolder;
    private final SkillVisibilityPolicy visibilityPolicy;
    private final DynamicSkillResourceService resourceService;

    public RedisDynamicAgentSkillRepository(
            SkillCatalogHolder catalogHolder,
            SkillVisibilityPolicy visibilityPolicy,
            DynamicSkillResourceService resourceService) {
        this.catalogHolder = catalogHolder;
        this.visibilityPolicy = visibilityPolicy;
        this.resourceService = resourceService;
    }

    @Override
    public List<AgentSkill> getAllSkills() {
        return getAllSkills(RuntimeContext.empty());
    }

    @Override
    public List<AgentSkill> getAllSkills(RuntimeContext context) {
        return catalogHolder.current().activeSkills().stream()
                .filter(entry -> visibilityPolicy.isVisible(entry.manifest(), context))
                .map(SkillCatalogEntry::agentSkill)
                .toList();
    }

    @Override
    public AgentSkill getSkill(String name) {
        return catalogHolder.current().requiredSkill(name).agentSkill();
    }

    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return new AgentSkillRepositoryInfo("dynamic", "redis-dynamic-skills", false);
    }

    @Override
    public List<String> getAllSkillNames() {
        return catalogHolder.current().activeSkills().stream()
                .map(entry -> entry.agentSkill().getName())
                .toList();
    }

    @Override
    public boolean save(List<AgentSkill> skills, boolean overwrite) {
        return false;
    }

    @Override
    public boolean delete(String skillId) {
        return false;
    }

    @Override
    public boolean skillExists(String skillId) {
        return catalogHolder.current().skills().containsKey(skillId);
    }

    @Override
    public String getSource() {
        return "redis-dynamic-skills";
    }

    @Override
    public void setWriteable(boolean writeable) {
        // 注册中心仓库只读。
    }

    @Override
    public boolean isWriteable() {
        return false;
    }

    @Override
    public SkillResources resourcesFor(String skillName, RuntimeContext context) {
        SkillCatalogEntry entry = catalogHolder.current().requiredSkill(skillName);
        return new DynamicSkillResources(entry, context, resourceService);
    }
}
