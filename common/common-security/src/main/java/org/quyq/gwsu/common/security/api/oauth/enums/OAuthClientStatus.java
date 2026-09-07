package org.quyq.gwsu.common.security.api.oauth.enums;

import lombok.Getter;

/**
 * OAuth 客户端状态。
 *
 * @author Quyq
 */
@Getter
public enum OAuthClientStatus {

    ENABLED("启用"),
    DISABLED("禁用");

    private final String name;

    OAuthClientStatus(String name) {
        this.name = name;
    }

}
