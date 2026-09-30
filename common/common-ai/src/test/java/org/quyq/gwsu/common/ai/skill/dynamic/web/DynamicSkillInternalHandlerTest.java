package org.quyq.gwsu.common.ai.skill.dynamic.web;

import org.quyq.gwsu.common.ai.skill.dynamic.model.ApprovalPolicy;
import org.quyq.gwsu.common.ai.skill.dynamic.model.RegisteredToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillCatalogSnapshot;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillRuntimeContextDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.SkillToolInvokeDTO;
import org.quyq.gwsu.common.ai.skill.dynamic.model.StandaloneToolManifest;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.LocalSkillResourceRegistry;
import org.quyq.gwsu.common.ai.skill.dynamic.repository.SkillCatalogHolder;
import org.quyq.gwsu.common.ai.skill.dynamic.tool.LocalSkillToolRegistry;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicSkillInternalHandlerTest {

    @Test
    void shouldInvokeRegisteredStandaloneToolWithoutSkillId() throws Exception {
        String toolName = "gwsu-kit_Clock_CurrentTime";
        RegisteredToolManifest toolManifest = new RegisteredToolManifest(
                "CurrentTime", toolName, "获取当前时间",
                Map.of(), Map.of(), null, true, ApprovalPolicy.NONE);
        SkillCatalogHolder catalogHolder = new SkillCatalogHolder();
        catalogHolder.replace(new SkillCatalogSnapshot(
                "catalog-revision", "toolkit-revision", Map.of(),
                Map.of(toolName, new StandaloneToolManifest("gwsu-kit", toolManifest))));
        AgentTool tool = mock(AgentTool.class);
        when(tool.callAsync(any())).thenReturn(Mono.just(mock(ToolResultBlock.class)));
        LocalSkillToolRegistry toolRegistry = new LocalSkillToolRegistry();
        toolRegistry.replaceProviderTools("gwsu-kit", Map.of(toolName, tool));
        DynamicSkillInternalHandler handler = new DynamicSkillInternalHandler(
                toolRegistry, new LocalSkillResourceRegistry(), catalogHolder);
        ServerRequest request = mock(ServerRequest.class);
        when(request.body(SkillToolInvokeDTO.class)).thenReturn(new SkillToolInvokeDTO(
                null, toolName, "call-1", Map.of(), SkillRuntimeContextDTO.empty()));

        ServerResponse response = handler.invoke(request);

        assertEquals(HttpStatus.OK, response.statusCode());
        verify(tool).callAsync(any());
    }
}
