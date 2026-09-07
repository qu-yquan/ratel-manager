package org.quyq.gwsu.common.job.executor;

import org.quyq.gwsu.common.job.constant.JobConst;
import org.quyq.gwsu.common.job.glue.GlueFactory;
import org.quyq.gwsu.common.job.handler.IJobHandler;
import org.quyq.gwsu.common.job.handler.annotation.XxlJob;
import org.quyq.gwsu.common.job.handler.impl.MethodJobHandler;
import org.quyq.gwsu.common.job.log.XxlJobFileAppender;
import org.quyq.gwsu.common.job.openapi.admin.JobAdminClientApi;
import org.quyq.gwsu.common.job.thread.ExecutorRegistryHelper;
import org.quyq.gwsu.common.job.thread.JobLogFileCleanHelper;
import org.quyq.gwsu.common.job.thread.JobThread;
import org.quyq.gwsu.common.job.thread.TriggerCallbackHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务执行器（Spring管理Bean）
 * <p>
 * 去掉EmbedServer，改为Spring管理。
 * 通过JobAdminClientApi与Admin通信，通过RouterFunctions暴露端点。
 */
public class XxlJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobExecutor.class);

    // ---------------------- 单例 ----------------------

    private static XxlJobExecutor xxlJobExecutor = null;

    public static XxlJobExecutor getInstance() {
        if (xxlJobExecutor == null) {
            throw new RuntimeException(">>>>>>>>>>> xxl-job load executor instance fail, please initialize it.");
        }
        return xxlJobExecutor;
    }

    // ---------------------- 字段 ----------------------

    private String appname;
    private String logPath;
    private int logRetentionDays = 30;
    private boolean glueEnabled = true;

    private final JobAdminClientApi jobAdminClientApi;

    public XxlJobExecutor(String appname, String logPath, int logRetentionDays, boolean glueEnabled, JobAdminClientApi jobAdminClientApi) {
        this.appname = appname;
        this.logPath = logPath;
        this.logRetentionDays = logRetentionDays;
        this.glueEnabled = glueEnabled;
        this.jobAdminClientApi = jobAdminClientApi;
    }

    public String getAppname() {
        return appname;
    }

    public boolean getGlueEnabled() {
        return glueEnabled;
    }

    public JobAdminClientApi getJobAdminClientApi() {
        return jobAdminClientApi;
    }

    // ---------------------- 辅助线程 ----------------------

    private ExecutorRegistryHelper executorRegistryHelper;
    private JobLogFileCleanHelper jobLogFileCleanHelper;
    private TriggerCallbackHelper triggerCallbackHelper;

    public TriggerCallbackHelper getTriggerCallbackHelper() {
        return triggerCallbackHelper;
    }

    // ---------------------- 生命周期 ----------------------

    @Override
    public void afterSingletonsInstantiated() {

        // 扫描JobHandler方法
        scanJobHandlerMethod(applicationContext);

        // 刷新GlueFactory为Spring模式
        GlueFactory.refreshInstance(1);

        // 绑定单例
        xxlJobExecutor = this;

        // 初始化日志路径
        try {
            XxlJobFileAppender.initLogPath(logPath);
        } catch (Exception e) {
            throw new RuntimeException("xxl-job executor initLogPath error.", e);
        }

        // 1、初始化日志清理线程
        jobLogFileCleanHelper = new JobLogFileCleanHelper();
        jobLogFileCleanHelper.start(logRetentionDays);

        // 2、初始化回调线程
        triggerCallbackHelper = new TriggerCallbackHelper();
        triggerCallbackHelper.start(this);

        // 3、初始化注册线程
        executorRegistryHelper = new ExecutorRegistryHelper();
        executorRegistryHelper.start(this);

        logger.info(">>>>>>>>>>> xxl-job executor start success, appname:{}", appname);
    }

    @Override
    public void destroy() {

        // 1、停止注册
        if (executorRegistryHelper != null) {
            executorRegistryHelper.stop(this);
        }

        // 2、停止JobThread
        if (!jobThreadRepository.isEmpty()) {

            // 优雅关闭等待任务完成
            try {
                TimeUnit.SECONDS.sleep(JobConst.ELEGANT_SHUTDOWN_WAITING_SECONDS);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }

            // 中断所有JobThread
            for (Map.Entry<String, JobThread> item : jobThreadRepository.entrySet()) {
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        logger.error(">>>>>>>>>>> xxl-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
            }
            jobThreadRepository.clear();
        }
        jobHandlerRepository.clear();

        // 3、停止回调线程
        if (triggerCallbackHelper != null) {
            triggerCallbackHelper.stop();
        }

        // 4、停止日志清理
        if (jobLogFileCleanHelper != null) {
            jobLogFileCleanHelper.stop();
        }

        logger.info(">>>>>>>>>>> xxl-job executor destroy success, appname:{}", appname);
    }

    // ---------------------- 扫描JobHandler方法 ----------------------

    /**
     * 排除的包名
     */
    private String excludedPackage = "org.springframework.,spring.";

    public void setExcludedPackage(String excludedPackage) {
        this.excludedPackage = excludedPackage;
    }

    /**
     * 扫描Spring容器中带@XxlJob注解的方法
     */
    private void scanJobHandlerMethod(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }

        // 1、构建排除包列表
        List<String> excludedPackageList = new ArrayList<>();
        if (excludedPackage != null) {
            for (String pkg : excludedPackage.split(",")) {
                if (!pkg.trim().isEmpty()) {
                    excludedPackageList.add(pkg.trim());
                }
            }
        }

        // 2、扫描Bean中的JobHandler
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class, false, false);
        for (String beanName : beanNames) {

            // 2.1、跳过排除包和懒加载Bean
            if (applicationContext instanceof BeanDefinitionRegistry beanDefinitionRegistry) {
                if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
                    continue;
                }
                BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanName);

                String beanClassName = beanDefinition.getBeanClassName();
                if (isExcluded(excludedPackageList, beanClassName)) {
                //    logger.debug(">>>>>>>>>>> xxl-job bean-definition scan, skip excluded-package beanName:{}, beanClassName:{}", beanName, beanClassName);
                    continue;
                }

                if (beanDefinition.isLazyInit()) {
                 //   logger.debug(">>>>>>>>>>> xxl-job bean-definition scan, skip lazy-init beanName:{}", beanName);
                    continue;
                }
            }

            // 2.2、扫描方法上的@XxlJob注解
            Class<?> beanClass = applicationContext.getType(beanName, false);
            if (beanClass == null) {
                continue;
            }

            Map<Method, XxlJob> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(beanClass,
                        (MethodIntrospector.MetadataLookup<XxlJob>) method -> AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class));
            } catch (Throwable ex) {
                logger.error(">>>>>>>>>>> xxl-job method-jobhandler resolve error for bean[" + beanName + "].", ex);
            }
            if (annotatedMethods == null || annotatedMethods.isEmpty()) {
                continue;
            }

            // 2.3、注册JobHandler
            Object jobBean = applicationContext.getBean(beanName);
            for (Map.Entry<Method, XxlJob> jobMethodEntry : annotatedMethods.entrySet()) {
                Method jobMethod = jobMethodEntry.getKey();
                XxlJob xxlJob = jobMethodEntry.getValue();
                registryJobHandler(xxlJob, jobBean, jobMethod);
            }
        }
    }

    private boolean isExcluded(List<String> excludedPackageList, String beanClassName) {
        if (excludedPackageList == null || excludedPackageList.isEmpty()) {
            return false;
        }
        if (beanClassName == null) {
            return false;
        }
        for (String pkg : excludedPackageList) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------- JobHandler仓库 ----------------------

    private final ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<>();

    public IJobHandler loadJobHandler(String name) {
        return jobHandlerRepository.get(name);
    }

    /**
     * 获取所有已注册的JobHandler名称
     */
    public Set<String> getJobHandlerNames() {
        return jobHandlerRepository.keySet();
    }

    public IJobHandler registryJobHandler(String name, IJobHandler jobHandler) {
        logger.info(">>>>>>>>>>> xxl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return jobHandlerRepository.put(name, jobHandler);
    }

    protected void registryJobHandler(XxlJob xxlJob, Object bean, Method executeMethod) {
        if (xxlJob == null) {
            return;
        }

        String name = xxlJob.value();
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (name.trim().isEmpty()) {
            throw new RuntimeException("xxl-job method-jobhandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }
        if (loadJobHandler(name) != null) {
            throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }

        executeMethod.setAccessible(true);

        Method initMethod = null;
        Method destroyMethod = null;

        if (!xxlJob.init().isEmpty()) {
            try {
                initMethod = clazz.getDeclaredMethod(xxlJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler initMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }
        if (!xxlJob.destroy().isEmpty()) {
            try {
                destroyMethod = clazz.getDeclaredMethod(xxlJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("xxl-job method-jobhandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }

        registryJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));
    }

    // ---------------------- JobThread仓库 ----------------------

    private final ConcurrentMap<String, JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    public JobThread registJobThread(String jobId, IJobHandler handler, String removeOldReason) {
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        logger.info(">>>>>>>>>>> xxl-job register JobThread success, jobId:{}, handler:{}", jobId, handler);

        JobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }

        return newJobThread;
    }

    public JobThread removeJobThread(String jobId, String removeOldReason) {
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
            return oldJobThread;
        }
        return null;
    }

    public JobThread loadJobThread(String jobId) {
        return jobThreadRepository.get(jobId);
    }

    // ---------------------- ApplicationContext ----------------------

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XxlJobExecutor.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
