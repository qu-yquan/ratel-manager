package org.quyq.gwsu.common.log.interceptor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.log.constants.LogInfoConstants;
import org.quyq.gwsu.common.log.utils.LogIdUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogIdPropagationInterceptorTest {

    private final LogIdPropagationInterceptor interceptor = new LogIdPropagationInterceptor();

    @AfterEach
    void clearLogId() {
        LogIdUtils.clear();
    }

    @Test
    void shouldUseCurrentLogIdForRestClientRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(LogInfoConstants.HEADER_PARENT_LOG_ID, "upstream-log-id");
        LogIdUtils.setLogId("current-log-id");

        interceptor.intercept(headers);

        assertEquals("current-log-id", headers.getFirst(LogInfoConstants.HEADER_PARENT_LOG_ID));
    }

    @Test
    void shouldUseCurrentLogIdForWebClientRequest() {
        LogIdUtils.setLogId("current-log-id");
        ClientRequest.Builder requestBuilder = ClientRequest
                .create(org.springframework.http.HttpMethod.GET, URI.create("http://gwsu-system/test"))
                .header(LogInfoConstants.HEADER_PARENT_LOG_ID, "upstream-log-id");

        interceptor.filter(requestBuilder);

        ClientRequest request = requestBuilder.build();
        assertEquals(
                "current-log-id",
                request.headers().getFirst(LogInfoConstants.HEADER_PARENT_LOG_ID));
    }

    @Test
    void shouldNotForwardStaleParentLogIdWithoutCurrentLog() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(LogInfoConstants.HEADER_PARENT_LOG_ID, "upstream-log-id");

        interceptor.intercept(headers);

        assertNull(headers.getFirst(LogInfoConstants.HEADER_PARENT_LOG_ID));
    }

    @Test
    void shouldExcludeParentLogIdFromGenericHeaderPropagation() {
        assertTrue(CoreConstants.Headers.REQUEST_IGNORE_HEADER.contains(
                LogInfoConstants.HEADER_PARENT_LOG_ID));
    }
}
