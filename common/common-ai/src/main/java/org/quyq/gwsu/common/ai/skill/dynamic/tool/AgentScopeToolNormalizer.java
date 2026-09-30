package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import org.quyq.gwsu.common.ai.skill.dynamic.AgentToolSource;
import org.quyq.gwsu.common.ai.skill.dynamic.model.ApprovalPolicy;
import org.quyq.gwsu.common.ai.skill.dynamic.model.NormalizedSkillTool;
import org.quyq.gwsu.common.ai.skill.dynamic.model.RegisteredToolManifest;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AgentScopeToolNormalizer {

    public List<NormalizedSkillTool> normalize(String applicationName, AgentToolSource source) {
        return switch (source) {
            case AgentToolSource.AgentToolInstance value -> List.of(normalizeAgentTool(
                    applicationName, value.tool(), value.tool()));
            case AgentToolSource.AnnotatedBean value -> normalizeAnnotatedBean(applicationName, value);
        };
    }

    private List<NormalizedSkillTool> normalizeAnnotatedBean(
            String applicationName,
            AgentToolSource.AnnotatedBean source) {
        Toolkit temporaryToolkit = new Toolkit();
        temporaryToolkit.registerTool(source.bean());
        Set<String> enabledTools = source.enabledTools();
        List<NormalizedSkillTool> result = new ArrayList<>();
        temporaryToolkit.getToolNames().stream()
                .filter(name -> enabledTools.isEmpty() || enabledTools.contains(name))
                .sorted(Comparator.naturalOrder())
                .forEach(name -> result.add(normalizeAgentTool(
                        applicationName, source.bean(), temporaryToolkit.getTool(name))));
        if (!enabledTools.isEmpty()) {
            Set<String> actualNames = result.stream()
                    .map(NormalizedSkillTool::originalName)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            if (!actualNames.containsAll(enabledTools)) {
                throw new IllegalArgumentException("enabledTools 中存在未找到的 @Tool：" + enabledTools);
            }
        }
        return List.copyOf(result);
    }

    private NormalizedSkillTool normalizeAgentTool(String applicationName, Object owner, AgentTool tool) {
        String classShortName = SkillToolNaming.classShortName(owner);
        String exposedName = SkillToolNaming.exposedName(applicationName, classShortName, tool.getName());
        AgentTool renamedTool = new RenamedAgentTool(exposedName, tool);
        RegisteredToolManifest manifest = new RegisteredToolManifest(
                tool.getName(),
                exposedName,
                tool.getDescription(),
                tool.getParameters(),
                tool.getOutputSchema(),
                tool.getStrict(),
                tool.isReadOnly(),
                tool.isReadOnly() ? ApprovalPolicy.NONE : ApprovalPolicy.REQUIRED);
        return new NormalizedSkillTool(tool.getName(), exposedName, classShortName, renamedTool, manifest);
    }
}
