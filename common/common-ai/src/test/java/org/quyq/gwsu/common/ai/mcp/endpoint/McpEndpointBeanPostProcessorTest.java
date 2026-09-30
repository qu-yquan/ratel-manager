package org.quyq.gwsu.common.ai.mcp.endpoint;

import org.quyq.gwsu.common.ai.mcp.annotation.McpServerEndpoint;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpEndpointBeanPostProcessorTest {

    @Test
    void shouldDiscoverEndpointBehindSpringProxy() {
        McpEndpointCollector collector = new McpEndpointCollector();
        McpEndpointBeanPostProcessor processor =
                new McpEndpointBeanPostProcessor(collector);
        ProxyFactory proxyFactory = new ProxyFactory(new TestEndpoint());
        proxyFactory.setProxyTargetClass(true);

        processor.postProcessAfterInitialization(proxyFactory.getProxy(), "testEndpoint");

        McpEndpointDescriptor descriptor = collector.snapshot().getFirst();
        assertEquals("test", descriptor.endpointId());
        assertEquals(TestEndpoint.class, descriptor.targetClass());
    }

    @Test
    void shouldAllowSameEndpointIdInDifferentBusinessModules() {
        McpEndpointCollector collector = new McpEndpointCollector();
        McpEndpointBeanPostProcessor processor =
                new McpEndpointBeanPostProcessor(collector);

        processor.postProcessAfterInitialization(new TestEndpoint(), "testEndpoint");

        processor.postProcessAfterInitialization(
                new DuplicateEndpoint(), "duplicateEndpoint");

        assertEquals(2, collector.snapshot().size());
    }

    @McpServerEndpoint("test")
    static class TestEndpoint {
    }

    @McpServerEndpoint("test")
    static class DuplicateEndpoint {
    }
}
