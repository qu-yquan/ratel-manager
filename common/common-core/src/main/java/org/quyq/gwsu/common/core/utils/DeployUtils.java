package org.quyq.gwsu.common.core.utils;


import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.DistributedServerInfo;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/3/11
 * @description 部署工具类
 */
public class DeployUtils {

    private DeployUtils() {
    }

    /**
     * 是否是单应用部署
     *
     * @return
     */
    public static boolean isSingle() {
        return Boolean.TRUE.equals(
                SpringUtils.getBean(Environment.class).getProperty(CoreConstants.Yaml.DEPLOY_SINGLE, Boolean.class)
        );
    }


    /**
     * 分布式模式部署时，获取服务名与服务模块前缀的映射关系
     *
     * @return
     */
    public static Map<String, DistributedServerInfo> getDistributedServerModuleMapping() {

        if (isSingle() || !ProxyUtil.hasClass("org.springframework.cloud.client.discovery.DiscoveryClient")) {
            return Map.of();
        }

        DiscoveryClient discoveryClient = SpringUtils.getBean(DiscoveryClient.class);
        List<String> services = discoveryClient
                .getServices();
        if (CollectionUtils.isEmpty(services)) {
            return Map.of();
        }

        Map<String, DistributedServerInfo> result = new HashMap<>();
        for (String service : services) {
            discoveryClient.getInstances(service).stream().findFirst().ifPresent(instance -> {
                Map<String, String> metadata = instance.getMetadata();
                String prefix = metadata.get("prefix");
                if (!StringUtils.hasText(prefix)) {
                    return;
                }
                String note = Optional.ofNullable(metadata.get("note"))
                        .filter(StringUtils::hasText)
                        .orElse(service);
                result.put(prefix, new DistributedServerInfo(service, note));
            });
        }

        return Map.copyOf(result);
    }


}
