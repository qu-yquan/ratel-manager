package org.quyq.gwsu.common.ai.config;

import org.quyq.gwsu.common.ai.skill.dynamic.model.RegisteredToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillInstanceHeartbeat;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderCatalogManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderIdentity;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRegistrationManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceContentVO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceReadDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRuntimeContextDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillToolInvokeDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.StandaloneToolManifest;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public final class DynamicSkillRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        register(hints,
                RegisteredToolManifest.class,
                SkillInstanceHeartbeat.class,
                SkillProviderCatalogManifest.class,
                SkillProviderIdentity.class,
                SkillRegistrationManifest.class,
                SkillResourceContentVO.class,
                SkillResourceManifest.class,
                SkillResourceReadDTO.class,
                SkillRuntimeContextDTO.class,
                SkillToolInvokeDTO.class,
                StandaloneToolManifest.class);
    }

    private void register(RuntimeHints hints, Class<?>... types) {
        for (Class<?> type : types) {
            hints.reflection().registerType(type,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.ACCESS_DECLARED_FIELDS);
        }
    }
}
