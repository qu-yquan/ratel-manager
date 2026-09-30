package org.quyq.gwsu.common.core.web;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.AssertUtils;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 统一解析业务模块端点路径。
 *
 * <p>单应用部署时增加模块前缀；分布式部署时服务内部保持原路径，模块前缀由网关处理。</p>
 */
public final class ModuleEndpointPathResolver {

    private static final int BUSINESS_PACKAGE_SEGMENTS = 4;

    private final Environment environment;
    private final List<BusinessModuleInfoProvider> moduleInfoProviders;

    public ModuleEndpointPathResolver(Environment environment) {
        this(environment, List.of());
    }

    public ModuleEndpointPathResolver(
            Environment environment,
            List<BusinessModuleInfoProvider> moduleInfoProviders) {
        this.environment = environment;
        this.moduleInfoProviders = List.copyOf(moduleInfoProviders);
    }

    public String resolve(String modulePrefix, String path) {
        String normalizedPath = normalizePath(path);
        if (isDistributedDeployment()) {
            return normalizedPath;
        }
        AssertUtils.hasText(modulePrefix, CommonErrorCode.E00000,
                "单应用部署的模块端点必须提供 modulePrefix");
        return "/" + normalizeModulePrefix(modulePrefix) + normalizedPath;
    }

    /**
     * 根据端点类所属业务包自动解析模块前缀后生成访问路径。
     */
    public String resolve(Class<?> endpointType, String path) {
        AssertUtils.notNull(endpointType, CommonErrorCode.E00000,
                "endpointType 不能为空");
        return resolve(resolveModulePrefix(endpointType), path);
    }

    private boolean isDistributedDeployment() {
        return Boolean.FALSE.equals(environment.getProperty(
                CoreConstants.Yaml.DEPLOY_SINGLE, Boolean.class));
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path.trim())) {
            return "/";
        }
        String normalized = path.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private String normalizeModulePrefix(String modulePrefix) {
        AssertUtils.hasText(modulePrefix, CommonErrorCode.E00000,
                "modulePrefix 不能为空");
        String normalized = modulePrefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        AssertUtils.hasText(normalized, CommonErrorCode.E00000,
                "modulePrefix 不能为空");
        return normalized;
    }

    private String resolveModulePrefix(Class<?> endpointType) {
        if (isDistributedDeployment()) {
            AssertUtils.isTrue(moduleInfoProviders.size() == 1, CommonErrorCode.E00000,
                    "微服务部署的模块端点所在应用必须且只能配置一个 "
                            + "BusinessModuleInfoProvider，当前数量：{}",
                    moduleInfoProviders.size());
            return modulePrefix(moduleInfoProviders.getFirst());
        }

        List<BusinessModuleInfoProvider> matchedProviders = moduleInfoProviders.stream()
                .filter(provider -> isInPackage(
                        endpointType, businessBasePackage(provider)))
                .toList();
        AssertUtils.isTrue(matchedProviders.size() == 1, CommonErrorCode.E00000,
                "单应用部署无法唯一确定端点所属业务模块：{}，匹配数量：{}",
                endpointType.getName(), matchedProviders.size());
        return modulePrefix(matchedProviders.getFirst());
    }

    private String businessBasePackage(BusinessModuleInfoProvider provider) {
        Class<?> providerType = AopUtils.getTargetClass(provider);
        return Stream.of(providerType.getPackageName().split("\\."))
                .limit(BUSINESS_PACKAGE_SEGMENTS)
                .collect(Collectors.joining("."));
    }

    private boolean isInPackage(Class<?> endpointType, String basePackage) {
        String endpointPackage = endpointType.getPackageName();
        return endpointPackage.equals(basePackage)
                || endpointPackage.startsWith(basePackage + ".");
    }

    private String modulePrefix(BusinessModuleInfoProvider provider) {
        BusinessModuleInfo module = provider.module();
        AssertUtils.notNull(module, CommonErrorCode.E00000,
                "BusinessModuleInfoProvider.module() 不能返回 null：{}",
                AopUtils.getTargetClass(provider).getName());
        AssertUtils.hasText(module.prefix(), CommonErrorCode.E00000,
                "业务模块 prefix 不能为空：{}",
                AopUtils.getTargetClass(provider).getName());
        return normalizeModulePrefix(module.prefix());
    }
}
