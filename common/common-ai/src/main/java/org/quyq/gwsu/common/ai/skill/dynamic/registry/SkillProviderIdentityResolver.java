package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderIdentity;

import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class SkillProviderIdentityResolver {

    private final SkillProviderIdentity identity;

    public SkillProviderIdentityResolver(ProjectUtils projectUtils) {
        String applicationName = projectUtils.getApplicationName();
        if (!StringUtils.hasText(applicationName)) {
            throw new IllegalStateException("spring.application.name 不能为空，无法注册动态技能");
        }
        this.identity = new SkillProviderIdentity(
                applicationName,
                UUID.randomUUID().toString());
    }

    public SkillProviderIdentity identity() {
        return identity;
    }
}
