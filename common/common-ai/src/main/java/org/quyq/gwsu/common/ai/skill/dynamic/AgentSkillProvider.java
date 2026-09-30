package org.quyq.gwsu.common.ai.skill.dynamic;

/**
 * 业务动态 AI 能力提供者。应用名称由 common-ai 统一读取，无需业务重复声明。
 */
@FunctionalInterface
public interface AgentSkillProvider {

    /**
     * 使用统一构建器声明 Skill 和独立工具。
     */
    void configure(AgentSkillBuilder builder);
}
