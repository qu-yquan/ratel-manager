package org.quyq.gwsu.common.ai.mcp.endpoint;

import org.quyq.gwsu.common.ai.mcp.server.McpEndpointRuntimeFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultMcpEndpointRegistry
        implements McpEndpointRegistry, SmartInitializingSingleton, DisposableBean {

    private final McpEndpointCollector collector;
    private final McpEndpointRuntimeFactory runtimeFactory;
    private final AtomicReference<Map<String, McpEndpointRuntime>> runtimes =
            new AtomicReference<>(Map.of());

    public DefaultMcpEndpointRegistry(
            McpEndpointCollector collector,
            McpEndpointRuntimeFactory runtimeFactory) {
        this.collector = collector;
        this.runtimeFactory = runtimeFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, McpEndpointRuntime> next = new LinkedHashMap<>();
        List<McpEndpointRuntime> created = new ArrayList<>();
        try {
            for (McpEndpointDescriptor descriptor : collector.snapshot()) {
                McpEndpointRuntime runtime = runtimeFactory.create(descriptor);
                created.add(runtime);
                McpEndpointRuntime previous = next.putIfAbsent(runtime.registryKey(), runtime);
                if (previous != null) {
                    throw new IllegalStateException("MCP Endpoint 重复：" + runtime.registryKey()
                            + "，类型：" + previous.descriptor().targetClass().getName()
                            + "、" + descriptor.targetClass().getName());
                }
            }
            runtimes.set(Map.copyOf(next));
        } catch (RuntimeException exception) {
            created.forEach(McpEndpointRuntime::close);
            throw exception;
        }
    }

    @Override
    public Optional<McpEndpointRuntime> find(String path) {
        return Optional.ofNullable(runtimes.get().get(path));
    }

    @Override
    public Collection<McpEndpointRuntime> endpoints() {
        return runtimes.get().values();
    }

    @Override
    public void destroy() {
        Map<String, McpEndpointRuntime> previous = runtimes.getAndSet(Map.of());
        previous.values().forEach(McpEndpointRuntime::close);
    }
}
