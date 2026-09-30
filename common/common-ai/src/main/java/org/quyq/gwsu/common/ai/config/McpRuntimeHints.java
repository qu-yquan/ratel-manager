package org.quyq.gwsu.common.ai.config;

import org.quyq.gwsu.common.ai.mcp.annotation.McpServerEndpoint;
import org.quyq.gwsu.common.ai.mcp.config.McpProperties;
import org.quyq.gwsu.common.ai.mcp.context.McpRequestContext;
import org.quyq.gwsu.common.ai.mcp.endpoint.McpEndpointDescriptor;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public final class McpRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        register(hints,
                McpServerEndpoint.class,
                McpProperties.class,
                McpRequestContext.class,
                McpEndpointDescriptor.class);
    }

    private void register(RuntimeHints hints, Class<?>... types) {
        for (Class<?> type : types) {
            hints.reflection().registerType(type,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.ACCESS_DECLARED_FIELDS);
        }
    }
}
