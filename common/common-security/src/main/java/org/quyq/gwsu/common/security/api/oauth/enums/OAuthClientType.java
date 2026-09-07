package org.quyq.gwsu.common.security.api.oauth.enums;

import lombok.Getter;

/**
 * OAuth 客户端类型。
 *
 * @author Quyq
 */
@Getter
public enum OAuthClientType {

    CONFIDENTIAL("机密服务端应用"),
    PUBLIC("公共客户端"),
    SPA("单页应用"),
    MOBILE("移动端应用"),
    DESKTOP("桌面应用");

    private final String name;

    OAuthClientType(String name) {
        this.name = name;
    }

}
