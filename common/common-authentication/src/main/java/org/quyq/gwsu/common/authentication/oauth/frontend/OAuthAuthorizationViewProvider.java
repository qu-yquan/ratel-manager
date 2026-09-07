package org.quyq.gwsu.common.authentication.oauth.frontend;

import org.quyq.gwsu.common.security.enums.AccountType;

import java.util.List;
import java.util.Map;

/**
 * OAuth 授权交互页面提供者。
 *
 * @author Quyq
 */
public interface OAuthAuthorizationViewProvider {

    AccountType accountType();

    String loginPageUrl(String authorizeUrl);

    String consentPageUrl(Map<String, List<String>> queryParams, String authorizeUri);

}
