package org.quyq.gwsu.common.authentication.oauth.frontend;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 授权交互页面公共地址构建逻辑。
 *
 * @author Quyq
 */
public abstract class AbstractOAuthAuthorizationViewProvider implements OAuthAuthorizationViewProvider {

    private static final String REDIRECT_PARAMETER = "redirect";

    private static final String AUTHORIZE_URI_PARAMETER = "authorize_uri";

    private final OAuthFrontendEndpointResolver endpointResolver;

    protected AbstractOAuthAuthorizationViewProvider(OAuthFrontendEndpointResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    @Override
    public final String loginPageUrl(String authorizeUrl) {
        return endpointResolver.frontendUrl(loginPagePath(), Map.of(REDIRECT_PARAMETER, List.of(authorizeUrl)));
    }

    @Override
    public final String consentPageUrl(Map<String, List<String>> queryParams, String authorizeUri) {
        Map<String, List<String>> params = new LinkedHashMap<>(queryParams);
        params.put(AUTHORIZE_URI_PARAMETER, List.of(authorizeUri));
        return endpointResolver.frontendUrl(consentPagePath(), params);
    }

    protected abstract String loginPagePath();

    protected abstract String consentPagePath();

}
