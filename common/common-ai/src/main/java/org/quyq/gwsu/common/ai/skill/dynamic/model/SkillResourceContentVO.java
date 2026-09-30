package org.quyq.gwsu.common.ai.skill.dynamic.model;

public record SkillResourceContentVO(
        String path,
        String mediaType,
        boolean binary,
        String encoding,
        String content,
        String digest
) {
}
