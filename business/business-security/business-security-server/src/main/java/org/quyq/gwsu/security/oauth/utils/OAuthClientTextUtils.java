package org.quyq.gwsu.security.oauth.utils;

import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientAuthenticationMethod;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthGrantType;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * OAuth 应用列表配置文本转换工具。
 *
 * @author Quyq
 */
public final class OAuthClientTextUtils {

    private OAuthClientTextUtils() {
    }

    public static String fromList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        values.forEach(OAuthClientTextUtils::checkComma);
        return String.join(",", values.stream().map(OAuthClientTextUtils::value).toList());
    }

    public static List<String> toList(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    public static List<OAuthGrantType> toGrantTypes(String value) {
        return toList(value).stream()
                .map(OAuthGrantType::from)
                .toList();
    }

    public static List<OAuthClientAuthenticationMethod> toClientAuthenticationMethods(String value) {
        return toList(value).stream()
                .map(OAuthClientAuthenticationMethod::from)
                .toList();
    }

    private static void checkComma(Object value) {
        if (value != null && value(value).contains(",")) {
            throw new IllegalArgumentException("OAuth应用配置项不能包含逗号");
        }
    }

    private static String value(Object value) {
        if (value instanceof OAuthGrantType grantType) {
            return grantType.getValue();
        }
        if (value instanceof OAuthClientAuthenticationMethod method) {
            return method.getValue();
        }
        return String.valueOf(value);
    }

}
