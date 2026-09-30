package org.quyq.gwsu.common.ai.config;

import org.quyq.gwsu.common.ai.skill.dynamic.web.DynamicSkillInternalHandler;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicSkillInternalRoutesTest {

    @Test
    void shouldExposeInternalEndpointsThroughRouterFunction() throws Exception {
        DynamicSkillInternalHandler handler = mock(DynamicSkillInternalHandler.class);
        when(handler.invoke(any(ServerRequest.class)))
                .thenReturn(ServerResponse.noContent().build());
        when(handler.readResource(any(ServerRequest.class)))
                .thenReturn(ServerResponse.noContent().build());
        DynamicSkillConfiguration configuration = new DynamicSkillConfiguration();
        MockMvc mockMvc = MockMvcBuilders.routerFunctions(
                        configuration.dynamicSkillInternalRoutes(handler))
                .build();

        mockMvc.perform(post("/internal/ai/tools/invoke")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/internal/ai/skills/resources/read")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(handler).invoke(any(ServerRequest.class));
        verify(handler).readResource(any(ServerRequest.class));
    }

    @Test
    void handlerShouldNotBeCollectedAsRestController() {
        assertFalse(AnnotatedElementUtils.hasAnnotation(
                DynamicSkillInternalHandler.class, RestController.class));
    }
}
