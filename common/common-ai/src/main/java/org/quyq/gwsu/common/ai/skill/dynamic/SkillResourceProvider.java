package org.quyq.gwsu.common.ai.skill.dynamic;

import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillResourceManifest;

import java.util.List;
import java.util.Optional;

public interface SkillResourceProvider {

    List<SkillResourceManifest> list();

    Optional<String> readText(String relativePath, SkillResourceContext context);

    Optional<byte[]> readBinary(String relativePath, SkillResourceContext context);

    static SkillResourceProvider empty() {
        return EmptySkillResourceProvider.INSTANCE;
    }

    enum EmptySkillResourceProvider implements SkillResourceProvider {
        INSTANCE;

        @Override
        public List<SkillResourceManifest> list() {
            return List.of();
        }

        @Override
        public Optional<String> readText(String relativePath, SkillResourceContext context) {
            return Optional.empty();
        }

        @Override
        public Optional<byte[]> readBinary(String relativePath, SkillResourceContext context) {
            return Optional.empty();
        }
    }
}
