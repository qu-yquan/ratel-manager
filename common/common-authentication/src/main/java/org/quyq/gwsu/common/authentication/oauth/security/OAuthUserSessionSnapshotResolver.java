package org.quyq.gwsu.common.authentication.oauth.security;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import cn.hutool.jwt.JWTException;
import cn.hutool.jwt.JWTUtil;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.authentication.oauth.domain.OAuthUserSessionSnapshot;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 从当前用户 token 读取 OAuth 授权所需的 TokenSession 快照。
 *
 * @author Quyq
 */
public class OAuthUserSessionSnapshotResolver {

    private final SecurityUtils securityUtils;

    public OAuthUserSessionSnapshotResolver(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    public Optional<OAuthUserSessionSnapshot> resolve(String token) {
        String normalizedToken = securityUtils.normalizeToken(token);
        if (!StringUtils.hasText(normalizedToken)) {
            return Optional.empty();
        }
        try {
            String loginType = JWTUtil.parseToken(normalizedToken)
                    .getPayloads()
                    .getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY);
            if (!StringUtils.hasText(loginType)) {
                return Optional.empty();
            }
            StpLogic stpLogic = SaManager.getStpLogic(AccountType.fromString(loginType).name(), true);
            SaSession tokenSession = stpLogic.getTokenSessionByToken(normalizedToken, false);
            return snapshot(tokenSession);
        } catch (JWTException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Optional<OAuthUserSessionSnapshot> snapshot(SaSession tokenSession) {
        if (tokenSession == null) {
            return Optional.empty();
        }
        Object workspaceValue = tokenSession.get(SecurityConstants.Session.SESSION_CURR_WORKSPACE);
        Object dataResourceValue = tokenSession.get(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE);
        if (!(workspaceValue instanceof WorkspaceInfo workspace)) {
            return Optional.empty();
        }
        return dataResource(dataResourceValue)
                .map(dataResource -> new OAuthUserSessionSnapshot(workspace, dataResource));
    }

    private Optional<Map<String, List<?>>> dataResource(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Optional.empty();
        }
        Map<String, List<?>> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof List<?> values)) {
                return Optional.empty();
            }
            target.put(key, values);
        }
        return Optional.of(target);
    }

}
