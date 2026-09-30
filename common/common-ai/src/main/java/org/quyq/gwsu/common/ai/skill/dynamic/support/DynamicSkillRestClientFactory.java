package org.quyq.gwsu.common.ai.skill.dynamic.support;

import org.quyq.gwsu.common.ai.skill.dynamic.registry.SkillRegistryProperties;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class DynamicSkillRestClientFactory {

    private DynamicSkillRestClientFactory() {
    }

    public static RestClient create(
            RestClient.Builder builder,
            String applicationName,
            SkillRegistryProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.remoteConnectTimeout());
        requestFactory.setReadTimeout(properties.remoteReadTimeout());
        return builder.clone()
                .baseUrl("http://" + applicationName)
                .requestFactory(requestFactory)
                .build();
    }
}
