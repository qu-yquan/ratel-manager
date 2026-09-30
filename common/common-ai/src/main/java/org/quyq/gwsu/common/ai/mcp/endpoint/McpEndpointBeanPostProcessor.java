package org.quyq.gwsu.common.ai.mcp.endpoint;

import org.quyq.gwsu.common.ai.mcp.annotation.McpServerEndpoint;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class McpEndpointBeanPostProcessor implements BeanPostProcessor {

    private static final Pattern ENDPOINT_ID_PATTERN =
            Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]*");

    private final McpEndpointCollector collector;

    public McpEndpointBeanPostProcessor(McpEndpointCollector collector) {
        this.collector = collector;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        McpServerEndpoint endpoint = AnnotatedElementUtils.findMergedAnnotation(
                targetClass, McpServerEndpoint.class);
        if (endpoint == null || !endpoint.enabled()) {
            return bean;
        }
        String endpointId = endpoint.value().trim();
        AssertUtils.isTrue(ENDPOINT_ID_PATTERN.matcher(endpointId).matches(),
                CommonErrorCode.E00000, "MCP Endpoint ID 格式非法：{}", endpointId);
        AssertUtils.hasText(endpoint.version(), CommonErrorCode.E00000,
                "MCP Endpoint version 不能为空：{}", endpointId);
        Set<String> enabledTools = Arrays.stream(endpoint.tools())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
        collector.register(new McpEndpointDescriptor(
                endpointId,
                endpoint.name().trim(),
                endpoint.version().trim(),
                endpoint.instructions(),
                endpoint.enableOutputSchema(),
                bean,
                targetClass,
                enabledTools));
        return bean;
    }
}
