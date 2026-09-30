package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderIdentity;

final class SkillRegistryKeys {

    static final String INSTANCES = "ai:skill:instances";
    static final String EVENT = "ai:skill:event";

    private SkillRegistryKeys() {
    }

    static String instanceMember(SkillProviderIdentity identity) {
        return identity.applicationName() + ":" + identity.instanceId();
    }

    static String instance(String member) {
        return "ai:skill:instance:" + member;
    }

    static String provider(String applicationName, String catalogRevision) {
        return "ai:skill:provider:%s:%s".formatted(applicationName, catalogRevision);
    }

    static String content(String applicationName, String skillId, String version) {
        return "ai:skill:content:%s:%s:%s".formatted(applicationName, skillId, version);
    }
}
