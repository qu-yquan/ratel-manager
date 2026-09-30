package org.quyq.gwsu.common.ai.mcp.endpoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 在 Bean 创建阶段收集 MCP Endpoint，运行时注册表在单例初始化完成后消费快照。
 */
public final class McpEndpointCollector {

    private final List<McpEndpointDescriptor> descriptors = new CopyOnWriteArrayList<>();

    public synchronized void register(McpEndpointDescriptor descriptor) {
        boolean registered = descriptors.stream()
                .anyMatch(existing -> existing.bean() == descriptor.bean());
        if (!registered) {
            descriptors.add(descriptor);
        }
    }

    public List<McpEndpointDescriptor> snapshot() {
        List<McpEndpointDescriptor> result = new ArrayList<>(descriptors);
        result.sort(Comparator.comparing(McpEndpointDescriptor::endpointId)
                .thenComparing(descriptor -> descriptor.targetClass().getName()));
        return List.copyOf(result);
    }
}
