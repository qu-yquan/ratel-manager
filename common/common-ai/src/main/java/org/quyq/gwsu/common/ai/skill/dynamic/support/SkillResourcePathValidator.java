package org.quyq.gwsu.common.ai.skill.dynamic.support;

import org.springframework.util.StringUtils;

import java.nio.file.Path;

public final class SkillResourcePathValidator {

    private SkillResourcePathValidator() {
    }

    public static String validate(String path) {
        if (!StringUtils.hasText(path) || path.startsWith("/") || path.contains("\\")) {
            throw new IllegalArgumentException("非法动态技能资源路径：" + path);
        }
        Path normalized = Path.of(path).normalize();
        if (normalized.isAbsolute() || normalized.startsWith("..") || !normalized.toString().equals(path)) {
            throw new IllegalArgumentException("非法动态技能资源路径：" + path);
        }
        return path;
    }
}
