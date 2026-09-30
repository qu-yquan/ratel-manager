package org.quyq.gwsu.common.ai.skill.dynamic;

import org.quyq.gwsu.common.ai.skill.dynamic.tool.SkillToolNaming;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;

import io.agentscope.core.tool.AgentTool;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 业务动态 AI 能力构建器。
 *
 * <p>通过类似 Spring Security 的 lambda DSL，在一个 Provider 中统一声明 Skill 和独立工具。</p>
 */
public final class AgentSkillBuilder {

    private final String applicationName;
    private final List<SkillRegistration> skills = new ArrayList<>();
    private final List<AgentToolSource> tools = new ArrayList<>();

    public AgentSkillBuilder(String applicationName) {
        this.applicationName = AssertUtils.hasText(
                applicationName, CommonErrorCode.E00000,
                "applicationName 不能为空").trim();
    }

    public AgentSkillBuilder skill(
            String skillId,
            Consumer<SkillBuilder> customizer) {
        AssertUtils.notNull(customizer, CommonErrorCode.E00000,
                "Skill 配置器不能为空：{}", skillId);
        SkillBuilder builder = new SkillBuilder(
                SkillToolNaming.qualifySkillId(applicationName, skillId));
        customizer.accept(builder);
        skills.add(builder.build());
        return this;
    }

    public AgentSkillBuilder tool(Object toolBean) {
        tools.add(annotatedTool(toolBean, Set.of()));
        return this;
    }

    public AgentSkillBuilder tool(Object toolBean, String... enabledTools) {
        tools.add(annotatedTool(toolBean, normalizeToolNames(enabledTools)));
        return this;
    }

    public AgentSkillBuilder tool(AgentTool tool) {
        tools.add(agentTool(tool));
        return this;
    }

    public Configuration build() {
        return new Configuration(skills, tools);
    }

    private static Set<String> normalizeToolNames(String[] enabledTools) {
        if (enabledTools == null || enabledTools.length == 0) {
            return Set.of();
        }
        Set<String> toolNames = Arrays.stream(enabledTools)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
        AssertUtils.notEmpty(toolNames, CommonErrorCode.E00000,
                "工具白名单不能全部为空");
        return toolNames;
    }

    private static AgentToolSource.AnnotatedBean annotatedTool(
            Object toolBean,
            Set<String> enabledTools) {
        return new AgentToolSource.AnnotatedBean(
                AssertUtils.notNull(toolBean, CommonErrorCode.E00000,
                        "@Tool Bean 不能为空"),
                enabledTools);
    }

    private static AgentToolSource.AgentToolInstance agentTool(AgentTool tool) {
        return new AgentToolSource.AgentToolInstance(
                AssertUtils.notNull(tool, CommonErrorCode.E00000,
                        "AgentTool 不能为空"));
    }

    public record Configuration(
            List<SkillRegistration> skills,
            List<AgentToolSource> tools
    ) {
        public Configuration {
            skills = List.copyOf(skills);
            tools = List.copyOf(tools);
        }
    }

    public static final class SkillBuilder {

        private final String skillId;
        private final List<AgentToolSource> tools = new ArrayList<>();

        private String description;
        private String version = "1.0.0";
        private Supplier<String> contentLoader;
        private String requiredPermission;
        private SkillResourceProvider resourceProvider = SkillResourceProvider.empty();

        private SkillBuilder(String skillId) {
            this.skillId = skillId;
        }

        public SkillBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SkillBuilder version(String version) {
            this.version = version;
            return this;
        }

        public SkillBuilder content(String content) {
            this.contentLoader = () -> content;
            return this;
        }

        public SkillBuilder content(Supplier<String> contentLoader) {
            this.contentLoader = contentLoader;
            return this;
        }

        public SkillBuilder requiredPermission(String requiredPermission) {
            this.requiredPermission = requiredPermission;
            return this;
        }

        public SkillBuilder tool(Object toolBean) {
            tools.add(annotatedTool(toolBean, Set.of()));
            return this;
        }

        public SkillBuilder tool(Object toolBean, String... enabledTools) {
            tools.add(annotatedTool(toolBean, normalizeToolNames(enabledTools)));
            return this;
        }

        public SkillBuilder tool(AgentTool tool) {
            tools.add(agentTool(tool));
            return this;
        }

        public SkillBuilder resources(SkillResourceProvider resourceProvider) {
            this.resourceProvider = AssertUtils.notNull(
                    resourceProvider, CommonErrorCode.E00000,
                    "Skill 资源提供者不能为空：{}", skillId);
            return this;
        }

        private SkillRegistration build() {
            AssertUtils.hasText(skillId, CommonErrorCode.E00000,
                    "skillId 不能为空");
            AssertUtils.hasText(description, CommonErrorCode.E00000,
                    "Skill description 不能为空：{}", skillId);
            AssertUtils.hasText(version, CommonErrorCode.E00000,
                    "Skill version 不能为空：{}", skillId);
            AssertUtils.notNull(contentLoader, CommonErrorCode.E00000,
                    "Skill content 不能为空：{}", skillId);
            return new SkillRegistration(
                    new SkillDefinition(
                            skillId.trim(), description.trim(), version.trim(),
                            contentLoader, requiredPermission),
                    tools,
                    resourceProvider);
        }
    }
}
