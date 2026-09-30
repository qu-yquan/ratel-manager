package org.quyq.gwsu.common.ai.skill.dynamic.tool;

import org.quyq.gwsu.common.ai.skill.dynamic.AgentToolSource;
import org.quyq.gwsu.common.ai.skill.dynamic.model.NormalizedSkillTool;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogSnapshot;
import org.quyq.gwsu.common.ai.skill.dynamic.model.StandaloneToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillProviderIdentityResolver;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.SkillCatalogHolder;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicSkillToolBinderTest {

    @Test
    void shouldBindStandaloneToolByApplicationName() {
        String applicationName = "gwsu-application";
        NormalizedSkillTool normalized = new AgentScopeToolNormalizer()
                .normalize(applicationName, new AgentToolSource.AnnotatedBean(new ClockTool()))
                .getFirst();
        LocalSkillToolRegistry localRegistry = new LocalSkillToolRegistry();
        localRegistry.replaceProviderTools(
                applicationName, Map.of(normalized.exposedName(), normalized.localTool()));
        SkillCatalogHolder catalogHolder = new SkillCatalogHolder();
        catalogHolder.replace(new SkillCatalogSnapshot(
                "catalog-revision",
                "toolkit-revision",
                Map.of(),
                Map.of(normalized.exposedName(), new StandaloneToolManifest(
                        applicationName, normalized.manifest()))));
        ProjectUtils projectUtils = mock(ProjectUtils.class);
        when(projectUtils.getApplicationName()).thenReturn(applicationName);
        DynamicSkillToolBinder binder = new DynamicSkillToolBinder(
                catalogHolder,
                new SkillProviderIdentityResolver(projectUtils),
                localRegistry,
                mock(RemoteSkillToolInvoker.class));
        Toolkit toolkit = new Toolkit();

        assertEquals("toolkit-revision", binder.bind(toolkit));
        assertTrue(toolkit.getToolNames().contains("gwsu-application_Clock_CurrentTime"));
    }

    private static final class ClockTool {

        @Tool(name = "CurrentTime", description = "获取当前时间", readOnly = true)
        public Mono<String> currentTime() {
            return Mono.just("now");
        }
    }
}
