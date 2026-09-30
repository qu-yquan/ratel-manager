package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import io.agentscope.core.tool.AgentTool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalSkillToolRegistry {

    private final ConcurrentHashMap<String, AgentTool> tools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, AgentTool>> providerTools = new ConcurrentHashMap<>();

    public synchronized void replaceProviderTools(String applicationName, Map<String, AgentTool> replacements) {
        Map<String, AgentTool> oldTools = providerTools.remove(applicationName);
        if (oldTools != null) {
            oldTools.forEach(tools::remove);
        }
        replacements.forEach((name, tool) -> {
            AgentTool previous = tools.putIfAbsent(name, tool);
            if (previous != null && previous != tool) {
                throw new IllegalStateException("动态技能工具名冲突：" + name);
            }
        });
        providerTools.put(applicationName, Map.copyOf(replacements));
    }

    public AgentTool requiredTool(String exposedToolName) {
        AgentTool tool = tools.get(exposedToolName);
        if (tool == null) {
            throw new IllegalStateException("本地动态技能工具不存在：" + exposedToolName);
        }
        return tool;
    }
}
