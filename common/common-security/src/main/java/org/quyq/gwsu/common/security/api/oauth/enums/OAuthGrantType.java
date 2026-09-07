package org.quyq.gwsu.common.security.api.oauth.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * OAuth 授权模式。
 *
 * @author Quyq
 */
@Getter
public enum OAuthGrantType {

    AUTHORIZATION_CODE("authorization_code", "授权码模式"),
    CLIENT_CREDENTIALS("client_credentials", "客户端凭证模式"),
    REFRESH_TOKEN("refresh_token", "刷新令牌"),
    DEVICE_CODE("urn:ietf:params:oauth:grant-type:device_code", "设备码模式");

    private final String value;

    private final String name;

    OAuthGrantType(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public static OAuthGrantType from(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value) || item.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的OAuth授权模式: " + value));
    }

}
