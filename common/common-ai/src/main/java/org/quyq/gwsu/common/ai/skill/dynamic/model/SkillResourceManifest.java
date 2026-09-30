package org.quyq.gwsu.common.ai.skill.dynamic.model;

public record SkillResourceManifest(
        String path,
        String mediaType,
        long size,
        String digest,
        boolean binary
) {
}
