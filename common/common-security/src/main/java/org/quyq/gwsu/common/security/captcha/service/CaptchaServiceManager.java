package org.quyq.gwsu.common.security.captcha.service;

import cn.hutool.crypto.digest.MD5;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;
import org.quyq.gwsu.common.security.captcha.properties.AjCaptchaProperties;
import org.quyq.gwsu.common.security.captcha.properties.CaptchaProperties;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务管理器，按配置动态初始化 AJ-Captcha 服务。
 *
 * @author Quyq
 */
@Slf4j
@RequiredArgsConstructor
public class CaptchaServiceManager {

    private static final String BEAN_NAME_PREFIX = "captchaService_";

    private final GenericApplicationContext context;
    private final ObjectMapper objectMapper;

    private final Map<CaptchaType, String> currentMd5 = new ConcurrentHashMap<>();
    private final Map<String, ManagedCaptchaService> serviceCache = new ConcurrentHashMap<>();

    public CaptchaService get(CaptchaType requestType) {
        CaptchaProperties config = ConfigInfoUtils.getByObject(CaptchaProperties.CONFIG_KEY, CaptchaProperties.class);
        CaptchaType type = config.effectiveType(requestType);
        String newMd5 = genMd5(config, type);

        String oldMd5 = currentMd5.get(type);
        if (oldMd5 != null && !oldMd5.equals(newMd5)) {
            ManagedCaptchaService oldService = serviceCache.remove(cacheKey(type, oldMd5));
            if (oldService != null) {
                destroyService(type, oldMd5, oldService);
                log.info("验证码配置变更，销毁旧验证码服务，type={}, oldMd5={}, newMd5={}", type, oldMd5, newMd5);
            }
        }

        String cacheKey = cacheKey(type, newMd5);
        ManagedCaptchaService service = serviceCache.get(cacheKey);
        if (service != null) {
            currentMd5.put(type, newMd5);
            return service.service();
        }

        synchronized (this) {
            service = serviceCache.get(cacheKey);
            if (service != null) {
                currentMd5.put(type, newMd5);
                return service.service();
            }

            service = registerBean(config, type, newMd5);
            serviceCache.put(cacheKey, service);
            currentMd5.put(type, newMd5);
            log.info("动态注册验证码服务成功，type={}, md5={}", type, newMd5);
            return service.service();
        }
    }

    private ManagedCaptchaService registerBean(CaptchaProperties config, CaptchaType type, String md5) {
        AjCaptchaProperties ajConfig = new AjCaptchaProperties();
        Properties properties = ajConfig.toCaptchaServiceProperties(config, type);
        CaptchaService target = CaptchaServiceFactory.getInstance(properties);
        String beanName = beanName(type, md5);

        if (context.containsBean(beanName)) {
            destroyBean(beanName);
        }

        context.registerBean(beanName, CaptchaService.class, () -> target,
                bd -> bd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON));
        return new ManagedCaptchaService(context.getBean(beanName, CaptchaService.class), properties);
    }

    private void destroyService(CaptchaType type, String md5, ManagedCaptchaService managedService) {
        managedService.service().destroy(managedService.properties());
        destroyBean(beanName(type, md5));
    }

    private void destroyBean(String beanName) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
        if (beanFactory.containsBean(beanName)) {
            beanFactory.destroySingleton(beanName);
            beanFactory.removeBeanDefinition(beanName);
        }
    }

    private String genMd5(CaptchaProperties config, CaptchaType type) {
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "common", config,
                    "aj", new AjCaptchaProperties()
            ));
            return MD5.create().digestHex(json);
        } catch (Exception e) {
            throw new SecurityException(CommonErrorCode.E04013, e);
        }
    }

    private String cacheKey(CaptchaType type, String md5) {
        return type.name() + ":" + md5;
    }

    private String beanName(CaptchaType type, String md5) {
        return BEAN_NAME_PREFIX + type.name() + "_" + md5;
    }

    private record ManagedCaptchaService(CaptchaService service, Properties properties) {
    }
}
