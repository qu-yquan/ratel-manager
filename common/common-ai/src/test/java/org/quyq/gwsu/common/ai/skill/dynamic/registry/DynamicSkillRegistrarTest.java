package org.quyq.gwsu.common.ai.skill.dynamic.registry;

import org.quyq.gwsu.common.ai.skill.dynamic.AgentSkillProvider;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillProviderCatalogManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.LocalSkillResourceRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.AgentScopeToolNormalizer;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.LocalSkillToolRegistry;

import io.agentscope.core.tool.Tool;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicSkillRegistrarTest {

    @Test
    void shouldPublishQualifiedSkillIdAndStandaloneToolsFromUnifiedProvider() throws Exception {
        String applicationName = "gwsu-kit";
        AgentSkillProvider provider = builder -> builder
                .skill("time_help", skill -> skill
                        .description("说明如何查询当前时间")
                        .content("# 时间查询\n使用时间工具查询当前时间。"))
                .tool(new ClockTool());
        ProjectUtils projectUtils = mock(ProjectUtils.class);
        when(projectUtils.getApplicationName()).thenReturn(applicationName);
        RedisSkillRegistry redisRegistry = mock(RedisSkillRegistry.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        DynamicSkillRegistrar registrar = new DynamicSkillRegistrar(
                List.of(provider),
                new SkillProviderIdentityResolver(projectUtils),
                new AgentScopeToolNormalizer(),
                new LocalSkillToolRegistry(),
                new LocalSkillResourceRegistry(),
                redisRegistry,
                SkillRegistryProperties.defaults(),
                objectMapper);

        registrar.rebuildAndPublish();

        ArgumentCaptor<SkillProviderCatalogManifest> catalogCaptor =
                ArgumentCaptor.forClass(SkillProviderCatalogManifest.class);
        verify(redisRegistry).publish(
                any(), catalogCaptor.capture(), anyMap(),
                any(Duration.class), any(Duration.class), anyBoolean());
        SkillProviderCatalogManifest catalog = catalogCaptor.getValue();
        assertEquals(applicationName, catalog.applicationName());
        assertEquals(1, catalog.registrations().size());
        assertEquals("gwsu-kit.time_help", catalog.registrations().getFirst().skillId());
        assertEquals(1, catalog.standaloneTools().size());
        assertEquals("gwsu-kit_Clock_CurrentTime",
                catalog.standaloneTools().getFirst().tool().exposedName());
    }

    private static final class ClockTool {

        @Tool(name = "CurrentTime", description = "获取当前时间", readOnly = true)
        public Mono<String> currentTime() {
            return Mono.just("now");
        }
    }
}
