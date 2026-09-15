package org.quyq.gwsu.common.authentication.config;


import cn.dev33.satoken.config.SaTokenConfig;
import org.quyq.gwsu.common.authentication.config.properties.LoginProperties;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;
import org.quyq.gwsu.common.authentication.filter.ContextFilterForJakartaServlet;
import org.quyq.gwsu.common.authentication.login.LoginHandler;
import org.quyq.gwsu.common.authentication.login.LoginLoadingManager;
import org.quyq.gwsu.common.authentication.login.LoginManager;
import org.quyq.gwsu.common.authentication.login.interceptor.log.LoginLogInterceptor;
import org.quyq.gwsu.common.authentication.login.dao.TokenDaoForRedisTemplate;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.log.security.TokenFingerprintService;
import org.quyq.gwsu.common.log.service.LoginLogHandlerService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
@AutoConfiguration
@EnableConfigurationProperties(LoginProperties.class)
@ImportRuntimeHints(AuthenticationRuntimeHintsRegistrar.class)
public class AuthenticationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".auth")
    public SaTokenConfig saTokenConfig() {
        return new SaTokenConfig();
    }


    @Bean
    @ConditionalOnMissingBean
    public ContextFilterForJakartaServlet saTokenContextFilterForServlet() {
        return new ContextFilterForJakartaServlet();
    }

    @Bean
    public TokenDaoForRedisTemplate tokenDaoForRedisTemplate(CacheUtils cacheUtils) {
        return new TokenDaoForRedisTemplate(cacheUtils);
    }

    @Bean
    public static LoginLoadingManager loginLoadingManager() {
        return new LoginLoadingManager();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public LoginManager loginManager(List<LoginHandler<? extends AbstractLoginDTO>> loginHandlers, ObjectMapper objectMapper) {
        return new LoginManager((List<LoginHandler<AbstractLoginDTO>>) (List<?>) loginHandlers, objectMapper);
    }

    @Bean
    public InitRunner saTokenInitRunner(SaTokenConfig config) {
        return new InitRunner(config);
    }

    @Bean
    public LoginLogInterceptor loginLogInterceptor(LoginLogHandlerService loginLogHandlerService,
                                                   TokenFingerprintService tokenFingerprintService) {
        return new LoginLogInterceptor(loginLogHandlerService, tokenFingerprintService);
    }


}
