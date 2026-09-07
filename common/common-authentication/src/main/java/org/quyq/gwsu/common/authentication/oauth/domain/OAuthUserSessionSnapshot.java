package org.quyq.gwsu.common.authentication.oauth.domain;

import cn.dev33.satoken.session.SaSession;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.security.constants.SecurityConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 授权时的用户 TokenSession 快照。
 *
 * @author Quyq
 */
public record OAuthUserSessionSnapshot(
        WorkspaceInfo workspace,
        Map<String, List<?>> dataResource) {

    public OAuthUserSessionSnapshot {
        dataResource = copyDataResource(dataResource);
    }

    public void writeTo(SaSession tokenSession) {
        if (workspace != null) {
            tokenSession.set(SecurityConstants.Session.SESSION_CURR_WORKSPACE, workspace);
        }
        if (dataResource != null) {
            tokenSession.set(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE, dataResource);
        }
    }

    private static Map<String, List<?>> copyDataResource(Map<String, List<?>> source) {
        if (source == null) {
            return null;
        }
        Map<String, List<?>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(
                key,
                value == null ? null : Collections.unmodifiableList(new ArrayList<>(value))));
        return Collections.unmodifiableMap(copy);
    }

}
