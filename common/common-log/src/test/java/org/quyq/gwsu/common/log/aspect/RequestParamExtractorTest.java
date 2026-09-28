package org.quyq.gwsu.common.log.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestParamExtractorTest {

    private final RequestParamExtractor extractor = new RequestParamExtractor();

    @Test
    void shouldExtractQueryPathAndBodySeparately() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of(
                "keyword", new String[]{"ratel"},
                "tag", new String[]{"java", "spring"}));
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("id", "1001"));

        Method method = SampleController.class.getDeclaredMethod(
                "save", String.class, String.class, SampleBody.class);
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[]{
                "1001", "ratel", new SampleBody("日志", true)});

        JsonNode result = extractor.getRequestParam(request, invocation);

        assertEquals("ratel", result.get("query").get("keyword").asString());
        assertEquals(2, result.get("query").get("tag").size());
        assertEquals("1001", result.get("path").get("id").asString());
        assertEquals("日志", result.get("body").get("name").asString());
        assertTrue(result.get("body").get("enabled").asBoolean());
        assertFalse(extractor.isEmpty(result));
    }

    @Test
    void shouldReturnStableEmptyStructureWithoutParameters() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of());

        Method method = SampleController.class.getDeclaredMethod("empty");
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);
        when(invocation.getArguments()).thenReturn(new Object[0]);

        JsonNode result = extractor.getRequestParam(request, invocation);

        assertTrue(result.has("query"));
        assertTrue(result.has("path"));
        assertTrue(result.has("body"));
        assertTrue(extractor.isEmpty(result));
    }

    private static class SampleController {

        @SuppressWarnings("unused")
        void save(
                @PathVariable("id") String id,
                @RequestParam("keyword") String keyword,
                @RequestBody SampleBody body) {
        }

        @SuppressWarnings("unused")
        void empty() {
        }
    }

    private record SampleBody(String name, boolean enabled) {
    }
}
