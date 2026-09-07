package org.quyq.gwsu.common.authentication.oauth.frontend;

import org.quyq.gwsu.common.security.enums.AccountType;

/**
 * 管理人员 OAuth 授权交互页面。
 *
 * @author Quyq
 */
public class ManagerOAuthAuthorizationViewProvider extends AbstractOAuthAuthorizationViewProvider {

    private static final String LOGIN_PAGE_PATH = "/sub-system/oauth2/login";

    private static final String CONSENT_PAGE_PATH = "/sub-system/oauth2/loginConsent";

    public ManagerOAuthAuthorizationViewProvider(OAuthFrontendEndpointResolver endpointResolver) {
        super(endpointResolver);
    }

    @Override
    public AccountType accountType() {
        return AccountType.MANAGER;
    }

    @Override
    protected String loginPagePath() {
        return LOGIN_PAGE_PATH;
    }

    @Override
    protected String consentPagePath() {
        return CONSENT_PAGE_PATH;
    }

}
