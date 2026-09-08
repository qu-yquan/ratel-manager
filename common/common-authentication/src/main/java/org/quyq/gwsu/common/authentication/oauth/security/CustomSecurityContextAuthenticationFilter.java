package org.quyq.gwsu.common.authentication.oauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.filter.RequestAuthenticationTokenResolver;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * OAuth 授权端点复用 Ratel 已登录用户。
 *
 * @author Quyq
 */
@Slf4j
public class CustomSecurityContextAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityUtils securityUtils;

    private final OAuthUserSessionSnapshotResolver userSessionSnapshotResolver;

    public CustomSecurityContextAuthenticationFilter(
            SecurityUtils securityUtils,
            OAuthUserSessionSnapshotResolver userSessionSnapshotResolver) {
        this.securityUtils = securityUtils;
        this.userSessionSnapshotResolver = userSessionSnapshotResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (isUnauthenticated(currentAuthentication)) {
            String token = RequestAuthenticationTokenResolver.resolve(request);
            Optional<Subject<Visitor>> subject = securityUtils.getSubject(token);
            Optional<UserInfo> userInfo = subject.flatMap(Subject::userInfo);
            if (subject.isPresent() && userInfo.isPresent()) {
                authenticate(subject.get(), userInfo.get(), token);
            } else {
                log.warn(
                        "[OAuth2] 未能建立用户认证上下文: path={}, headerPresent={}, cookiePresent={}, "
                                + "tokenResolved={}, subjectResolved={}, userResolved={}",
                        request.getRequestURI(),
                        StringUtils.hasText(request.getHeader(CoreConstants.Headers.HTTP_HEADER_TOKEN_KEY)),
                        hasAuthenticationCookie(request),
                        StringUtils.hasText(token),
                        subject.isPresent(),
                        userInfo.isPresent());
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean hasAuthenticationCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (var cookie : request.getCookies()) {
            if (SecurityConstants.Authentication.AUTH_INFO_KEY_PREFIX.equals(cookie.getName())
                    && StringUtils.hasText(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnauthenticated(Authentication authentication) {
        return authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated();
    }

    private void authenticate(Subject<?> subject, UserInfo userInfo, String token) {
        List<SimpleGrantedAuthority> authorities = CollectionUtils.isEmpty(subject.getRoles())
                ? Collections.emptyList()
                : subject.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        CustomOAuthAuthenticatedPrincipal principal = new CustomOAuthAuthenticatedPrincipal(
                userInfo,
                subject,
                userSessionSnapshotResolver.resolve(token).orElse(null),
                authorities);
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
