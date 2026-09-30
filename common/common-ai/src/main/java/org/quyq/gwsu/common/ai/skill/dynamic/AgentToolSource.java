package org.quyq.gwsu.common.ai.skill.dynamic;

import io.agentscope.core.tool.AgentTool;

import java.util.Objects;
import java.util.Set;

/**
 * AgentScope 工具来源。
 *
 * <p>该抽象不隶属于 Skill，可同时供动态 Skill、独立工具和 MCP Endpoint 使用。</p>
 */
public sealed interface AgentToolSource {

    record AnnotatedBean(Object bean, Set<String> enabledTools) implements AgentToolSource {
        public AnnotatedBean {
            Objects.requireNonNull(bean, "bean 不能为空");
            enabledTools = enabledTools == null ? Set.of() : Set.copyOf(enabledTools);
        }

        public AnnotatedBean(Object bean) {
            this(bean, Set.of());
        }
    }

    record AgentToolInstance(AgentTool tool) implements AgentToolSource {
        public AgentToolInstance {
            Objects.requireNonNull(tool, "tool 不能为空");
        }
    }
}
