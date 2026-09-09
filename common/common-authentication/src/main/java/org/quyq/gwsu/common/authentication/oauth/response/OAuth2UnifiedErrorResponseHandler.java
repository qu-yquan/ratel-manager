package org.quyq.gwsu.common.authentication.oauth.response;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ErrorAuthenticationFailureHandler;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/** OAuth2 认证异常统一响应处理器。 */
public class OAuth2UnifiedErrorResponseHandler implements AuthenticationFailureHandler {

    private final OAuth2ResponseWriter responseWriter;
    private final ObjectMapper objectMapper;
    private final AuthenticationFailureHandler delegate = new OAuth2ErrorAuthenticationFailureHandler();

    public OAuth2UnifiedErrorResponseHandler(OAuth2ResponseWriter responseWriter, ObjectMapper objectMapper) {
        this.responseWriter = responseWriter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        delegate.onAuthenticationFailure(request, wrapper, exception);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.readValue(wrapper.getContentAsByteArray(), Map.class);
        int status = OAuth2ErrorCodes.INVALID_CLIENT.equals(data.get("error"))
                ? HttpServletResponse.SC_UNAUTHORIZED
                : wrapper.getStatus();
        if (status == HttpServletResponse.SC_UNAUTHORIZED
                && !wrapper.containsHeader(HttpHeaders.WWW_AUTHENTICATE)) {
            wrapper.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic");
        }
        Object description = data.get("error_description");
        String message = description == null ? String.valueOf(data.get("error")) : description.toString();
        wrapper.resetBuffer();
        responseWriter.writeError(wrapper, status, message, data);
        wrapper.copyBodyToResponse();
    }
}
