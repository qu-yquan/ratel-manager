package org.quyq.gwsu.common.security.config;


import org.quyq.gwsu.common.security.captcha.service.CaptchaProvider;
import org.quyq.gwsu.common.security.captcha.service.CaptchaServiceManager;
import org.quyq.gwsu.common.security.captcha.service.impl.AjCaptchaProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Quyq
 * @date 2026/9/28
 * @description
 */
@AutoConfiguration
public class CaptchaConfiguration {

    @Bean
    public CaptchaServiceManager captchaServiceManager(GenericApplicationContext context, ObjectMapper objectMapper) {
        return new CaptchaServiceManager(context, objectMapper);
    }


    @Bean
    public CaptchaProvider ajcCaptchaProvider(CaptchaServiceManager captchaServiceManager, ObjectMapper objectMapper) {
        return new AjCaptchaProvider(captchaServiceManager, objectMapper);
    }

}
