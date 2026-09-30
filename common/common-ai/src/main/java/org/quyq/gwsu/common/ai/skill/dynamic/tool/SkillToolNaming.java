package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;

import org.springframework.util.ClassUtils;

public final class SkillToolNaming {

    private SkillToolNaming() {
    }

    public static String qualifySkillId(String applicationName, String businessSkillId) {
        String normalizedApplicationName = AssertUtils.hasText(
                applicationName, CommonErrorCode.E00000,
                "applicationName 不能为空").trim();
        String normalizedBusinessSkillId = AssertUtils.hasText(
                businessSkillId, CommonErrorCode.E00000,
                "业务 skillId 不能为空").trim();
        AssertUtils.isFalse(normalizedBusinessSkillId.startsWith(
                        normalizedApplicationName + "."),
                CommonErrorCode.E00000,
                "业务 skillId 不需要包含 applicationName 前缀：{}",
                normalizedBusinessSkillId);
        return normalizedApplicationName + "." + normalizedBusinessSkillId;
    }

    public static void validateSkillId(String applicationName, String skillId) {
        AssertUtils.hasText(applicationName, CommonErrorCode.E00000,
                "applicationName 不能为空");
        AssertUtils.isTrue(skillId != null
                        && skillId.startsWith(applicationName + ".")
                        && skillId.length() > applicationName.length() + 1,
                CommonErrorCode.E00000,
                "skillId 必须使用 {applicationName}.{businessName} 格式：{}", skillId);
    }

    public static String classShortName(Object owner) {
        String simpleName = ClassUtils.getUserClass(owner).getSimpleName();
        if (simpleName.endsWith("AgentTool")) {
            return simpleName.substring(0, simpleName.length() - "AgentTool".length());
        }
        if (simpleName.endsWith("Tool")) {
            return simpleName.substring(0, simpleName.length() - "Tool".length());
        }
        return simpleName;
    }

    public static String exposedName(String applicationName, String classShortName, String originalName) {
        AssertUtils.hasText(applicationName, CommonErrorCode.E00000,
                "工具命名的 applicationName 不能为空");
        AssertUtils.hasText(classShortName, CommonErrorCode.E00000,
                "工具命名的类名不能为空");
        AssertUtils.hasText(originalName, CommonErrorCode.E00000,
                "工具名不能为空");
        return "%s_%s_%s".formatted(applicationName, classShortName, originalName);
    }
}
