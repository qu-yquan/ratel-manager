package org.quyq.gwsu.common.authentication.oauth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** 匹配未携带用户码的设备验证页面入口。 */
public class OAuthDeviceVerificationEntryRequestMatcher implements RequestMatcher {

    private final RequestMatcher pathMatcher;

    public OAuthDeviceVerificationEntryRequestMatcher(String deviceVerificationPath) {
        this.pathMatcher = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.GET, deviceVerificationPath);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return pathMatcher.matches(request)
                && request.getParameter(OAuth2ParameterNames.USER_CODE) == null;
    }
}
