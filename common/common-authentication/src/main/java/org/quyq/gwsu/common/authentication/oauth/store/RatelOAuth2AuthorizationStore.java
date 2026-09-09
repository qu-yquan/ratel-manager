package org.quyq.gwsu.common.authentication.oauth.store;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

/**
 * OAuth 授权数据存储接口。
 *
 * @author Quyq
 */
public interface RatelOAuth2AuthorizationStore {

    void save(OAuth2Authorization authorization);

    void remove(OAuth2Authorization authorization);

    OAuth2Authorization findById(String id);

    OAuth2Authorization findByToken(String tokenValue, OAuth2TokenType tokenType);

}
