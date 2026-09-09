package org.quyq.gwsu.common.authentication.oauth.response;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** JWKS 端点统一响应包装。 */
public class OAuth2JwkSetResponseWrappingFilter extends OncePerRequestFilter {

    private final String jwkSetEndpoint;
    private final ObjectMapper objectMapper;
    private final OAuth2ResponseWriter responseWriter;

    public OAuth2JwkSetResponseWrappingFilter(
            String jwkSetEndpoint,
            ObjectMapper objectMapper,
            OAuth2ResponseWriter responseWriter) {
        this.jwkSetEndpoint = jwkSetEndpoint;
        this.objectMapper = objectMapper;
        this.responseWriter = responseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !jwkSetEndpoint.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);

        byte[] originalBody = wrapper.getContentAsByteArray();
        if (originalBody.length == 0 || !isJson(wrapper.getContentType())) {
            wrapper.copyBodyToResponse();
            return;
        }

        Object data = objectMapper.readValue(originalBody, Object.class);
        if (data instanceof Map<?, ?> map && map.containsKey("code") && map.containsKey("data")) {
            wrapper.copyBodyToResponse();
            return;
        }

        int status = wrapper.getStatus();
        String body = status >= 200 && status < 300
                ? responseWriter.successJson(data)
                : responseWriter.errorJson(status, errorMessage(data), data);
        wrapper.resetBuffer();
        wrapper.setCharacterEncoding(StandardCharsets.UTF_8.name());
        wrapper.setContentType(MediaType.APPLICATION_JSON_VALUE);
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        wrapper.setContentLength(responseBody.length);
        wrapper.getOutputStream().write(responseBody);
        wrapper.copyBodyToResponse();
    }

    private boolean isJson(String contentType) {
        return contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    private String errorMessage(Object data) {
        if (data instanceof Map<?, ?> map) {
            Object description = map.get("error_description");
            if (description != null) {
                return description.toString();
            }
            Object error = map.get("error");
            if (error != null) {
                return error.toString();
            }
        }
        return "OAuth2 request failed";
    }
}
