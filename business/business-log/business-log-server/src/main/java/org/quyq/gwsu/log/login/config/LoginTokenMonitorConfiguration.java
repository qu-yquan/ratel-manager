package org.quyq.gwsu.log.login.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 登录 Token 失效扫描调度配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class LoginTokenMonitorConfiguration {
}
