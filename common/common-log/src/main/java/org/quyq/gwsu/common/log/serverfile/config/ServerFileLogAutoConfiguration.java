package org.quyq.gwsu.common.log.serverfile.config;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.exception.handler.GlobalExceptionFunctionHandler;
import org.quyq.gwsu.common.log.serverfile.handler.InternalLogFileHandler;
import org.quyq.gwsu.common.log.serverfile.service.LocalLogFileService;
import org.quyq.gwsu.common.log.serverfile.service.impl.LocalLogFileServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * 服务器本地日志检索自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix = CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".log.server-file",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(ServerFileLogProperties.class)
public class ServerFileLogAutoConfiguration {

    private static final String BASE_PATH = "/internal/log-files";

    @Bean
    @ConditionalOnMissingBean
    public LocalLogFileService localLogFileService(
            ServerFileLogProperties properties,
            Environment environment) {
        return new LocalLogFileServiceImpl(properties, environment);
    }

    @Bean
    @ConditionalOnProperty(
            name = CoreConstants.Yaml.DEPLOY_SINGLE,
            havingValue = "false")
    public InternalLogFileHandler internalLogFileHandler(LocalLogFileService localLogFileService) {
        return new InternalLogFileHandler(localLogFileService);
    }

    @Bean
    @ConditionalOnProperty(
            name = CoreConstants.Yaml.DEPLOY_SINGLE,
            havingValue = "false")
    public RouterFunction<ServerResponse> internalLogFileRoutes(InternalLogFileHandler handler) {
        return RouterFunctions
                .route(
                        RequestPredicates.POST(BASE_PATH + "/search")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        handler::search)
                .andRoute(
                        RequestPredicates.POST(BASE_PATH + "/context")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        handler::context)
                .andRoute(
                        RequestPredicates.GET(BASE_PATH + "/download"),
                        handler::download)
                .filter(new GlobalExceptionFunctionHandler());
    }
}
