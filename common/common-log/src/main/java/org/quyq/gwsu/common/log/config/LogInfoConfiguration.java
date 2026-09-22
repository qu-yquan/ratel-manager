package org.quyq.gwsu.common.log.config;


import org.quyq.gwsu.common.core.provider.BusinessModuleInfoProvider;
import org.quyq.gwsu.common.log.api.ILogClientApi;
import org.quyq.gwsu.common.log.aspect.LogAnnotationAdvisor;
import org.quyq.gwsu.common.log.aspect.LogAspectInterceptor;
import org.quyq.gwsu.common.log.config.properties.LogInfoConfigProperties;
import org.quyq.gwsu.common.log.service.AccessLogHandlerService;
import org.quyq.gwsu.common.log.service.AsyncLoginLogHandlerService;
import org.quyq.gwsu.common.log.service.LoginLogHandlerService;
import org.quyq.gwsu.common.log.service.NoOpLoginLogHandlerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/5/14
 * @description 日志模块自动配置
 */
@AutoConfiguration
@ConditionalOnMissingClass({"org.springframework.cloud.gateway.config.GatewayAutoConfiguration"})
@EnableConfigurationProperties(LogInfoConfigProperties.class)
@ImportRuntimeHints(LogRuntimeHintsRegistrar.class)
public class LogInfoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "dtt.log.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AccessLogHandlerService accessLogHandlerService(ILogClientApi logClientApi, LogInfoConfigProperties properties) {
        return new AccessLogHandlerService(logClientApi, properties.accessLog().recordThreadCount());
    }

    @Bean
    @ConditionalOnProperty(prefix = "dtt.log.login-log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LoginLogHandlerService asyncLoginLogHandlerService(ILogClientApi logClientApi) {
        return new AsyncLoginLogHandlerService(logClientApi);
    }

    @Bean
    @ConditionalOnMissingBean(LoginLogHandlerService.class)
    public LoginLogHandlerService noOpLoginLogHandlerService() {
        return new NoOpLoginLogHandlerService();
    }

    @Bean
    @ConditionalOnBean(AccessLogHandlerService.class)
    public LogAspectInterceptor logAspectInterceptor(LogInfoConfigProperties properties,
                                                     AccessLogHandlerService logHandlerService,
                                                     ObjectMapper objectMapper,
                                                     ObjectProvider<List<BusinessModuleInfoProvider>> providers) {
        return new LogAspectInterceptor(properties.accessLog(), logHandlerService, objectMapper, providers);
    }

    @Bean
    @ConditionalOnBean(LogAspectInterceptor.class)
    public LogAnnotationAdvisor logAnnotationAdvisor(LogAspectInterceptor interceptor) {
        return new LogAnnotationAdvisor(interceptor);
    }

}
