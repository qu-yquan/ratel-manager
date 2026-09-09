package org.quyq.gwsu.common.authentication.oauth.response;

import jakarta.servlet.http.HttpServletResponse;
import org.quyq.gwsu.common.core.domain.R;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** OAuth2 JSON 响应统一写入器。 */
public class OAuth2ResponseWriter {

    private final ObjectMapper objectMapper;

    public OAuth2ResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeSuccess(HttpServletResponse response, Object data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        write(response, R.ok(data));
    }

    public void writeError(HttpServletResponse response, int status, String message, Object data) throws IOException {
        response.setStatus(status);
        write(response, new R<>(status, message, data, null));
    }

    public String successJson(Object data) {
        return objectMapper.writeValueAsString(R.ok(data));
    }

    public String errorJson(int status, String message, Object data) {
        return objectMapper.writeValueAsString(new R<>(status, message, data, null));
    }

    private void write(HttpServletResponse response, Object body) throws IOException {
        byte[] responseBody = objectMapper.writeValueAsBytes(body);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setContentLength(responseBody.length);
        response.getOutputStream().write(responseBody);
    }
}
