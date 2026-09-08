package org.quyq.gwsu.common.authentication.oauth.security;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.authentication.oauth.domain.OAuthUserSessionSnapshot;
import org.quyq.gwsu.common.security.domain.Subject;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * 将 Ratel 登录主体桥接为 Spring Security OAuth Principal。
 *
 * @author Quyq
 */
public class CustomOAuthAuthenticatedPrincipal implements OAuth2AuthenticatedPrincipal {

    @Getter
    private final UserInfo userInfo;

    @Getter
    private final Subject<?> subject;

    @Getter
    private final OAuthUserSessionSnapshot userSessionSnapshot;

    private final Collection<? extends GrantedAuthority> authorities;

    public CustomOAuthAuthenticatedPrincipal(
            UserInfo userInfo,
            Subject<?> subject,
            OAuthUserSessionSnapshot userSessionSnapshot,
            Collection<? extends GrantedAuthority> authorities) {
        this.userInfo = userInfo;
        this.subject = subject;
        this.userSessionSnapshot = userSessionSnapshot;
        this.authorities = authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @NonNull String getName() {
        return Optional.ofNullable(userInfo.getUserName())
                .orElse(userInfo.getUserId());
    }

}
