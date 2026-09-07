package org.quyq.gwsu.common.authentication.oauth;

import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.authentication.oauth.frontend.OAuthFrontendEndpointResolver;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuthFrontendEndpointResolverTest {

    @Test
    void buildLoginPageUrlUsesViewBaseUrlAndEncodesAuthorizeRedirect() {
        String url = OAuthFrontendEndpointResolver.buildFrontendUrl(
                "https://console.example.com/",
                "/sub-system/oauth2/login",
                Map.of("redirect", List.of("https://api.example.com/system/oauth2/authorize?client_id=demo&scope=openid profile"))
        );

        assertEquals(
                "https://console.example.com/sub-system/oauth2/login?redirect=https%3A%2F%2Fapi.example.com%2Fsystem%2Foauth2%2Fauthorize%3Fclient_id%3Ddemo%26scope%3Dopenid+profile",
                url
        );
    }

    @Test
    void buildConsentPageUrlKeepsRepeatedScopeValues() {
        Map<String, List<String>> query = new LinkedHashMap<>();
        query.put("client_id", List.of("demo"));
        query.put("scope", List.of("openid", "profile"));
        query.put("state", List.of("state-1"));

        String url = OAuthFrontendEndpointResolver.buildFrontendUrl(
                "https://console.example.com",
                "/sub-system/oauth2/loginConsent",
                query
        );

        assertEquals(
                "https://console.example.com/sub-system/oauth2/loginConsent?client_id=demo&scope=openid&scope=profile&state=state-1",
                url
        );
    }
}
