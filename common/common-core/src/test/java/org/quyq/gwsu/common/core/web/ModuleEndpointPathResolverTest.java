package org.quyq.gwsu.common.core.web;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.BusinessModuleInfo;
import org.quyq.gwsu.common.core.exception.ArgumentException;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleEndpointPathResolverTest {

    @Test
    void shouldAddModulePrefixInSingleDeployment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty(CoreConstants.Yaml.DEPLOY_SINGLE, Boolean.class))
                .thenReturn(true);
        ModuleEndpointPathResolver resolver = new ModuleEndpointPathResolver(environment);

        assertEquals("/security/mcp/knowledge",
                resolver.resolve("security", "/mcp/knowledge"));
    }

    @Test
    void shouldKeepInternalPathInDistributedDeployment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty(CoreConstants.Yaml.DEPLOY_SINGLE, Boolean.class))
                .thenReturn(false);
        ModuleEndpointPathResolver resolver = new ModuleEndpointPathResolver(environment);

        assertEquals("/mcp/knowledge",
                resolver.resolve("security", "mcp/knowledge"));
    }

    @Test
    void shouldResolveModuleByEndpointTypeInSingleDeployment() {
        ModuleEndpointPathResolver resolver = new ModuleEndpointPathResolver(
                environment(true), List.of(new TestModuleInfoProvider("security")));

        assertEquals("/security/mcp/knowledge",
                resolver.resolve(TestEndpoint.class, "/mcp/knowledge"));
    }

    @Test
    void shouldRejectMultipleModuleProvidersInDistributedDeployment() {
        ModuleEndpointPathResolver resolver = new ModuleEndpointPathResolver(
                environment(false),
                List.of(
                        new TestModuleInfoProvider("security"),
                        new TestModuleInfoProvider("system")));

        assertThrows(ArgumentException.class,
                () -> resolver.resolve(TestEndpoint.class, "/mcp/knowledge"));
    }

    private Environment environment(boolean single) {
        Environment environment = mock(Environment.class);
        when(environment.getProperty(CoreConstants.Yaml.DEPLOY_SINGLE, Boolean.class))
                .thenReturn(single);
        return environment;
    }

    private record TestModuleInfoProvider(String prefix)
            implements BusinessModuleInfoProvider {

        @Override
        public BusinessModuleInfo module() {
            return new BusinessModuleInfo(prefix, "测试模块");
        }
    }

    private static final class TestEndpoint {
    }
}
