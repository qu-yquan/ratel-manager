package org.quyq.gwsu.common.authentication.filter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.utils.AuthenticationTokenUtils;
import org.springframework.util.StringUtils;

/**
 * 从 Servlet 请求中解析认证 token。
 *
 * @author Quyq
 */
public final class RequestAuthenticationTokenResolver {

    private RequestAuthenticationTokenResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String headerToken = resolveHeaderToken(request);
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        return resolveCookieToken(request);
    }

    private static String resolveHeaderToken(HttpServletRequest request) {
        return AuthenticationTokenUtils.resolve(request.getHeader(CoreConstants.Headers.HTTP_HEADER_TOKEN_KEY));
    }

    private static String resolveCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SecurityConstants.Authentication.AUTH_INFO_KEY_PREFIX.equals(cookie.getName())
                    && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
