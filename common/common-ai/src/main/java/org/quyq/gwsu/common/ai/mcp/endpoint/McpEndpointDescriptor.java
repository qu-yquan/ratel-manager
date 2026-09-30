package org.quyq.gwsu.common.ai.mcp.endpoint;

import java.util.Objects;
import java.util.Set;

public record McpEndpointDescriptor(
        String endpointId,
        String name,
        String version,
        String instructions,
        boolean enableOutputSchema,
        Object bean,
        Class<?> targetClass,
        Set<String> enabledTools
) {

    public McpEndpointDescriptor {
        Objects.requireNonNull(endpointId, "endpointId 不能为空");
        Objects.requireNonNull(version, "version 不能为空");
        Objects.requireNonNull(bean, "bean 不能为空");
        Objects.requireNonNull(targetClass, "targetClass 不能为空");
        enabledTools = enabledTools == null ? Set.of() : Set.copyOf(enabledTools);
    }
}
