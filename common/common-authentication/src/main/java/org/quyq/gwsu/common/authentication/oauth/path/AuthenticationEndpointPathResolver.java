package org.quyq.gwsu.common.authentication.oauth.path;

import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.springframework.util.StringUtils;

/**
 * 认证端点路径解析器。
 *
 * @author Quyq
 */
public class AuthenticationEndpointPathResolver {

    public String resolve(String path) {
        String normalized = normalize(path);
        if (DeployUtils.isSingle()) {
            return "/" + SecurityConstants.Authentication.AUTH_SERVER_PREFIX + normalized;
        }
        return normalized;
    }

    private String normalize(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

}
