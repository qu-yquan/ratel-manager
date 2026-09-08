package org.quyq.gwsu.common.authentication.oauth.store;

import org.quyq.gwsu.common.authentication.oauth.security.CustomOAuthAuthenticatedPrincipal;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

import java.security.Principal;
import java.util.Optional;

/**
 * Spring Authorization Server 授权数据服务适配。
 *
 * @author Quyq
 */
public class CustomOAuth2AuthorizationService implements OAuth2AuthorizationService {

    public static final String RATEL_SUBJECT_ATTRIBUTE = "ratel:subject";

    public static final String RATEL_USER_SESSION_SNAPSHOT_ATTRIBUTE = "ratel:user-session-snapshot";

    private final RatelOAuth2AuthorizationStore store;

    private final SecurityUtils securityUtils;

    public CustomOAuth2AuthorizationService(RatelOAuth2AuthorizationStore store, SecurityUtils securityUtils) {
        this.store = store;
        this.securityUtils = securityUtils;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        store.save(withCurrentSubject(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        store.remove(authorization);
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return store.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return store.findByToken(token);
    }

    private OAuth2Authorization withCurrentSubject(OAuth2Authorization authorization) {
        if (authorization.getAttribute(RATEL_SUBJECT_ATTRIBUTE) != null
                && authorization.getAttribute(RATEL_USER_SESSION_SNAPSHOT_ATTRIBUTE) != null) {
            return authorization;
        }
        OAuth2Authorization.Builder builder = OAuth2Authorization.from(authorization);
        if (authorization.getAttribute(RATEL_SUBJECT_ATTRIBUTE) == null) {
            currentSubject(authorization)
                    .filter(subject -> subject.userInfo().isPresent())
                    .ifPresent(subject -> builder.attribute(RATEL_SUBJECT_ATTRIBUTE, subject));
        }
        if (authorization.getAttribute(RATEL_USER_SESSION_SNAPSHOT_ATTRIBUTE) == null) {
            currentPrincipal(authorization)
                    .map(CustomOAuthAuthenticatedPrincipal::getUserSessionSnapshot)
                    .ifPresent(snapshot -> builder.attribute(RATEL_USER_SESSION_SNAPSHOT_ATTRIBUTE, snapshot));
        }
        return builder.build();
    }

    private Optional<Subject<?>> currentSubject(OAuth2Authorization authorization) {
        Optional<Subject<?>> principalSubject = currentPrincipal(authorization)
                .map(CustomOAuthAuthenticatedPrincipal::getSubject);
        if (principalSubject.isPresent()) {
            return principalSubject;
        }
        return securityUtils.getSubject().map(subject -> subject);
    }

    private Optional<CustomOAuthAuthenticatedPrincipal> currentPrincipal(OAuth2Authorization authorization) {
        Object principalAttribute = authorization.getAttribute(Principal.class.getName());
        if (principalAttribute instanceof Authentication authentication
                && authentication.getPrincipal() instanceof CustomOAuthAuthenticatedPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

}
