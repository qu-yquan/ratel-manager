package org.quyq.gwsu.common.ai.mcp.context;

import org.quyq.gwsu.common.ai.mcp.config.McpProperties;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.utils.SecurityUtils;

import org.springframework.web.servlet.function.ServerRequest;

import java.util.Objects;

/**
 * 从已认证请求中读取用户，并从调用方指定的请求头读取 Agent 会话标识。
 */
public final class DefaultMcpRequestContextExtractor implements McpRequestContextExtractor {

    private final SecurityUtils securityUtils;
    private final McpProperties properties;

    public DefaultMcpRequestContextExtractor(SecurityUtils securityUtils, McpProperties properties) {
        this.securityUtils = Objects.requireNonNull(securityUtils, "securityUtils 不能为空");
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
    }

    @Override
    public McpRequestContext extract(ServerRequest request) {
        String userId = securityUtils.userInfo()
                .map(UserInfo::getUserId)
                .orElse(null);
        String sessionId = request.headers().firstHeader(properties.sessionIdHeader());
        return new McpRequestContext(userId, sessionId);
    }
}
