package org.quyq.gwsu.common.authentication.oauth.token;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import org.quyq.gwsu.common.authentication.dataresource.DataResourceScopeManager;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientInfoProvider;
import org.quyq.gwsu.common.authentication.oauth.client.OAuthClientAccountTypeResolver;
import org.quyq.gwsu.common.authentication.oauth.domain.OAuthUserSessionSnapshot;
import org.quyq.gwsu.common.authentication.oauth.store.CustomOAuth2AuthorizationService;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.api.oauth.OAuthScopeConstants;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthGrantType;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 将 OAuth access token 对应的登录态写入 Sa-Token Session。
 *
 * @author Quyq
 */
public class CustomOAuthSubjectWriter {

    private final OAuth2AuthorizationService authorizationService;

    private final OAuthClientInfoProvider clientInfoProvider;

    private final OAuthClientAccountTypeResolver accountTypeResolver;

    public CustomOAuthSubjectWriter(
            OAuth2AuthorizationService authorizationService,
            OAuthClientInfoProvider clientInfoProvider,
            OAuthClientAccountTypeResolver accountTypeResolver) {
        this.authorizationService = authorizationService;
        this.clientInfoProvider = clientInfoProvider;
        this.accountTypeResolver = accountTypeResolver;
    }

    public void write(
            RegisteredClient registeredClient,
            OAuth2AccessToken accessToken,
            String requestedGrantType) {
        OAuth2Authorization authorization = authorizationService.findByToken(
                accessToken.getTokenValue(),
                OAuth2TokenType.ACCESS_TOKEN);
        OAuthClientInfoVO clientInfo = clientInfo(registeredClient);
        clientInfo.setAuthorizedScopes(accessToken.getScopes());
        Subject<ClientInfo> subject = new Subject<>(clientInfo);

        Optional<Subject<UserInfo>> userSubject = authUserSubject(authorization);
        userSubject.ifPresent(authSubject -> {
            if (inheritsUserPermissions(accessToken.getScopes())) {
                subject.setRoles(authSubject.getRoles());
            }
            subject.setDataScope(authSubject.getDataScope());
            subject.setAuthUser(authSubject.userInfo().orElse(null));
        });

        StpLogic stpLogic = SaManager.getStpLogic(accountTypeResolver.resolve(registeredClient).name(), true);
        Object loginId = stpLogic.getLoginIdByToken(accessToken.getTokenValue());
        Assert.notNull(loginId, "OAuth access token login session cannot be found");
        SaSession accountSession = stpLogic.getSessionByLoginId(loginId);
        Assert.notNull(accountSession, "OAuth account session cannot be found");
        accountSession.set(SecurityConstants.Session.SESSION_SUBJECT_INFO_KEY, subject);

        SaSession tokenSession = stpLogic.getTokenSessionByToken(accessToken.getTokenValue(), true);
        Optional<OAuthUserSessionSnapshot> userSessionSnapshot = userSessionSnapshot(authorization);
        userSessionSnapshot.ifPresentOrElse(
                snapshot -> snapshot.writeTo(tokenSession),
                () -> writeFallbackUserSession(subject, tokenSession));
        tokenSession.set(SecurityConstants.Session.SESSION_USER_VISITOR_TYPE, VisitorType.CLIENT);
        tokenSession.set(
                SecurityConstants.Session.SESSION_USER_LOGIN_TYPE,
                oauthLoginType(requestedGrantType, authorization));
    }

    static boolean inheritsUserPermissions(Set<String> scopes) {
        return scopes != null && scopes.contains(OAuthScopeConstants.INHERIT_USER_PERMISSIONS);
    }

    static String oauthLoginType(String requestedGrantType, OAuth2Authorization authorization) {
        String currentGrantType = readableGrantType(requestedGrantType);
        String originalGrantType = Optional.ofNullable(authorization)
                .map(OAuth2Authorization::getAuthorizationGrantType)
                .map(AuthorizationGrantType::getValue)
                .map(CustomOAuthSubjectWriter::readableGrantType)
                .orElse(null);
        String grantType = StringUtils.hasText(currentGrantType) ? currentGrantType : originalGrantType;
        Assert.hasText(grantType, "OAuth authorization grant type cannot be found");
        if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(currentGrantType)
                && StringUtils.hasText(originalGrantType)
                && !currentGrantType.equals(originalGrantType)) {
            return "oauth2:" + originalGrantType + ":" + currentGrantType;
        }
        return "oauth2:" + grantType;
    }

    private static String readableGrantType(String grantType) {
        if (!StringUtils.hasText(grantType)) {
            return null;
        }
        return OAuthGrantType.DEVICE_CODE.getValue().equals(grantType) ? "device_code" : grantType;
    }

    private OAuthClientInfoVO clientInfo(RegisteredClient registeredClient) {
        OAuthClientInfoVO clientInfo = clientInfoProvider.getByClientId(registeredClient.getClientId());
        Assert.notNull(clientInfo, "OAuth client configuration cannot be found");
        return OAuthClientInfoVO.subjectInfo(clientInfo);
    }


    private Optional<Subject<UserInfo>> authUserSubject(OAuth2Authorization authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(authorization.getAttribute(CustomOAuth2AuthorizationService.RATEL_SUBJECT_ATTRIBUTE));
    }

    private Optional<OAuthUserSessionSnapshot> userSessionSnapshot(OAuth2Authorization authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(authorization.getAttribute(
                CustomOAuth2AuthorizationService.RATEL_USER_SESSION_SNAPSHOT_ATTRIBUTE));
    }

    private void writeFallbackUserSession(Subject<ClientInfo> subject, SaSession tokenSession) {
        subject.userInfo().ifPresent(user -> {
            List<WorkspaceInfo> workspaceList = DataResourceScopeManager.workspaceList(user);
            WorkspaceInfo workspace = workspaceList.getFirst();
            tokenSession.set(SecurityConstants.Session.SESSION_CURR_WORKSPACE, workspace);
            tokenSession.set(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE,
                    DataResourceScopeManager.dataResource(workspace, user, subject.getDataScope()));
        });
    }

}
