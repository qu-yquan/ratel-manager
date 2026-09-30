package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import org.quyq.gwsu.common.ai.skill.dynamic.model.RegisteredToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogEntry;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogSnapshot;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderIdentity;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRegistrationManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.StandaloneToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillProviderIdentityResolver;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.SkillCatalogHolder;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DynamicSkillToolBinder {

    private final SkillCatalogHolder catalogHolder;
    private final SkillProviderIdentity identity;
    private final LocalSkillToolRegistry localToolRegistry;
    private final RemoteSkillToolInvoker remoteInvoker;

    public DynamicSkillToolBinder(
            SkillCatalogHolder catalogHolder,
            SkillProviderIdentityResolver identityResolver,
            LocalSkillToolRegistry localToolRegistry,
            RemoteSkillToolInvoker remoteInvoker) {
        this.catalogHolder = catalogHolder;
        this.identity = identityResolver.identity();
        this.localToolRegistry = localToolRegistry;
        this.remoteInvoker = remoteInvoker;
    }

    public String bind(Toolkit toolkit) {
        SkillCatalogSnapshot snapshot = catalogHolder.current();
        Map<String, AgentTool> registered = new LinkedHashMap<>();
        for (StandaloneToolManifest standalone : snapshot.standaloneTools().values()) {
            RegisteredToolManifest tool = standalone.tool();
            AgentTool agentTool = createTool(
                    standalone.applicationName(), null, tool);
            register(toolkit, registered, tool, agentTool);
        }
        for (SkillCatalogEntry entry : snapshot.activeSkills()) {
            SkillRegistrationManifest registration = entry.manifest();
            String groupName = "skill:" + registration.skillId();
            toolkit.createSkillToolGroup(
                    groupName,
                    "Tools for " + registration.skillId(),
                    false,
                    registration.skillId());
            for (RegisteredToolManifest tool : registration.tools()) {
                AgentTool agentTool = registered.get(tool.exposedName());
                if (agentTool == null) {
                    if (toolkit.getToolNames().contains(tool.exposedName())) {
                        throw new IllegalStateException("Agent Toolkit 工具名冲突：" + tool.exposedName());
                    }
                    agentTool = isLocal(registration.applicationName())
                            ? localToolRegistry.requiredTool(tool.exposedName())
                            : new RemoteRegisteredAgentTool(
                                    registration.applicationName(), registration.skillId(),
                                    tool, remoteInvoker);
                    toolkit.registration().agentTool(agentTool).apply();
                    registered.put(tool.exposedName(), agentTool);
                }
                toolkit.addToolToGroup(groupName, tool.exposedName());
            }
        }
        return snapshot.toolkitRevision();
    }

    private AgentTool createTool(
            String applicationName,
            String skillId,
            RegisteredToolManifest tool) {
        return isLocal(applicationName)
                ? localToolRegistry.requiredTool(tool.exposedName())
                : new RemoteRegisteredAgentTool(
                        applicationName, skillId, tool, remoteInvoker);
    }

    private void register(
            Toolkit toolkit,
            Map<String, AgentTool> registered,
            RegisteredToolManifest tool,
            AgentTool agentTool) {
        if (toolkit.getToolNames().contains(tool.exposedName())) {
            throw new IllegalStateException("Agent Toolkit 工具名冲突：" + tool.exposedName());
        }
        toolkit.registration().agentTool(agentTool).apply();
        registered.put(tool.exposedName(), agentTool);
    }

    private boolean isLocal(String applicationName) {
        return Objects.equals(applicationName, identity.applicationName());
    }
}
