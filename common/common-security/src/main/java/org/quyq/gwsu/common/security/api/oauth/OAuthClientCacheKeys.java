package org.quyq.gwsu.common.security.api.oauth;

public final class OAuthClientCacheKeys {
    private static final String ID_PREFIX = "authentication:oauth2:client:id:";
    private static final String CLIENT_ID_PREFIX = "authentication:oauth2:client:client-id:";

    private OAuthClientCacheKeys() {
    }

    public static String byId(String id) {
        return ID_PREFIX + id;
    }

    public static String byClientId(String clientId) {
        return CLIENT_ID_PREFIX + clientId;
    }
}
