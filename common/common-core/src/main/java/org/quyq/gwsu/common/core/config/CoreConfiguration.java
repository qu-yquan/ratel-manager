package org.quyq.gwsu.common.core.config;


import io.micrometer.context.ContextRegistry;
import org.quyq.gwsu.common.core.config.properties.ProjectProperties;
import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.core.utils.ProjectUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.core.utils.filter.ProcessorChain;
import org.quyq.gwsu.common.core.utils.filter.RequestResponseProcessor;
import org.quyq.gwsu.common.core.web.ModuleEndpointPathResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/3/11
 * @description
 */
@AutoConfiguration
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableConfigurationProperties({ProjectProperties.class})
public class CoreConfiguration {

    @Bean
    public SpringUtils springUtils() {
        return new SpringUtils();
    }

    @Bean
    public ProjectUtils projectUtils(Environment environment, ProjectProperties projectProperties) {
        return new ProjectUtils(projectProperties, environment);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModuleEndpointPathResolver moduleEndpointPathResolver(
            Environment environment,
            ObjectProvider<List<BusinessModuleInfoProvider>> moduleInfoProviders) {
        return new ModuleEndpointPathResolver(
                environment, moduleInfoProviders.getIfAvailable(List::of));
    }


    @Bean
    public ProcessorChain requestProcessorChain(ObjectProvider<List<RequestResponseProcessor>> processors) {
        return new ProcessorChain(processors.getIfAvailable());
    }

//    @Bean
//    public ApplicationRunner applicationThreadLocalRunner() {
//        return (args) ->{
//            ContextRegistry
//                    .getInstance()
//                    .registerContextAccessor()
//
//        };
//    }


}
