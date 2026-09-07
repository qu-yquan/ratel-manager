package org.quyq.gwsu.common.security.utils;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.springframework.util.StringUtils;

/**
 * 认证请求头 token 解析工具。
 *
 * @author Quyq
 */
public final class AuthenticationTokenUtils {

    private AuthenticationTokenUtils() {
    }

    public static String resolve(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        if (authorization.regionMatches(true, 0, CoreConstants.Headers.TOKEN_PREFIX, 0,
                CoreConstants.Headers.TOKEN_PREFIX.length())) {
            return authorization.substring(CoreConstants.Headers.TOKEN_PREFIX.length());
        }
        if (authorization.startsWith(SecurityConstants.Authentication.API_KEY_PREFIX)) {
            return authorization;
        }
        if (authorization.indexOf(' ') >= 0) {
            return null;
        }
        return authorization;
    }
}
