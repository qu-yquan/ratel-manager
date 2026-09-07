package org.quyq.gwsu.common.security.api.oauth.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * OAuth 客户端认证方式。
 *
 * @author Quyq
 */
@Getter
public enum OAuthClientAuthenticationMethod {

    NONE("none", "无客户端密钥"),
    CLIENT_SECRET_BASIC("client_secret_basic", "Basic认证"),
    CLIENT_SECRET_POST("client_secret_post", "表单密钥认证");

    private final String value;

    private final String name;

    OAuthClientAuthenticationMethod(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public static OAuthClientAuthenticationMethod from(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value) || item.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的OAuth客户端认证方式: " + value));
    }

}
