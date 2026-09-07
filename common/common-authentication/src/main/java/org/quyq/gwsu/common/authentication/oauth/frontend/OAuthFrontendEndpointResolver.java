package org.quyq.gwsu.common.authentication.oauth.frontend;

import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * OAuth 浏览器交互页面地址解析器。
 *
 * @author Quyq
 */
public class OAuthFrontendEndpointResolver {

    public String apiUrl(String path) {
        return buildBaseUrl(apiBaseUrl(), path);
    }

    public String frontendUrl(String pagePath, Map<String, List<String>> queryParams) {
        return buildFrontendUrl(viewBaseUrl(), pagePath, queryParams);
    }

    public static String buildFrontendUrl(String viewBaseUrl, String pagePath, Map<String, List<String>> queryParams) {
        if (!StringUtils.hasText(viewBaseUrl)) {
            throw new IllegalStateException("基础项目信息未配置前端地址");
        }
        String normalizedBaseUrl = viewBaseUrl.endsWith("/")
                ? viewBaseUrl.substring(0, viewBaseUrl.length() - 1)
                : viewBaseUrl;
        String normalizedPath = pagePath.startsWith("/") ? pagePath : "/" + pagePath;
        StringBuilder url = new StringBuilder(normalizedBaseUrl).append(normalizedPath);
        if (CollectionUtils.isEmpty(queryParams)) {
            return url.toString();
        }
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                continue;
            }
            for (String value : entry.getValue()) {
                url.append(first ? "?" : "&");
                first = false;
                url.append(encode(entry.getKey())).append("=").append(encode(value));
            }
        }
        return url.toString();
    }

    private static String buildBaseUrl(String baseUrl, String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBaseUrl + normalizedPath;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String viewBaseUrl() {
        BaseProjectInfoProperties properties = ConfigInfoUtils.getByObject(
                BaseProjectInfoProperties.CONFIG_KEY,
                BaseProjectInfoProperties.class
        );
        if (properties == null || !StringUtils.hasText(properties.viewBaseUrl())) {
            throw new IllegalStateException("基础项目信息未配置前端地址");
        }
        return properties.viewBaseUrl();
    }

    private String apiBaseUrl() {
        BaseProjectInfoProperties properties = ConfigInfoUtils.getByObject(
                BaseProjectInfoProperties.CONFIG_KEY,
                BaseProjectInfoProperties.class
        );
        if (properties == null || !StringUtils.hasText(properties.apiBaseUrl())) {
            throw new IllegalStateException("基础项目信息未配置后端API地址");
        }
        return properties.apiBaseUrl();
    }
}
