package org.quyq.gwsu.common.ai.mcp.config;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".ai.mcp")
public record McpProperties(
        Boolean enabled,
        String basePath,
        Duration requestTimeout,
        String sessionIdHeader
) {

    public McpProperties {
        enabled = enabled == null ? Boolean.TRUE : enabled;
        basePath = normalizeBasePath(basePath);
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
        sessionIdHeader = StringUtils.hasText(sessionIdHeader)
                ? sessionIdHeader : "X-Agent-Session-Id";
        AssertUtils.isTrue(!requestTimeout.isNegative() && !requestTimeout.isZero(),
                CommonErrorCode.E00000,
                CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".ai.mcp.request-timeout 必须大于 0");
    }

    public String endpointPath(String endpointId) {
        return basePath + "/" + endpointId;
    }

    public static McpProperties defaults() {
        return new McpProperties(Boolean.TRUE, null, null, null);
    }

    private static String normalizeBasePath(String value) {
        String path = StringUtils.hasText(value) ? value.trim() : "/mcp";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
