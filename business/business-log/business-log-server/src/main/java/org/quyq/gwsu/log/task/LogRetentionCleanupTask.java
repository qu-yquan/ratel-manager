package org.quyq.gwsu.log.task;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.cache.exceptions.CacheException;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.common.log.dto.LogLifeCycle;
import org.quyq.gwsu.common.log.dto.LogStorage;
import org.quyq.gwsu.common.log.dto.LogStorageConfig;
import org.quyq.gwsu.common.log.enums.SaveMedium;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.log.login.service.ILogLoginService;
import org.quyq.gwsu.log.operation.service.ILogOperationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据库日志生命周期清理任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogRetentionCleanupTask {

    private static final String TASK_LOCK = "log:retention:cleanup";
    private static final int DELETE_BATCH_SIZE = 2_000;

    private final CacheUtils cacheUtils;
    private final ILogOperationService operationLogService;
    private final ILogLoginService loginLogService;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        scheduler = ThreadPoolUtil.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "log-retention-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::cleanupExpiredLogs, 1, 1, TimeUnit.HOURS);
        log.info("日志生命周期清理任务已启动，执行间隔：1小时");
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("日志生命周期清理任务已停止");
        }
    }

    public void cleanupExpiredLogs() {
        try {
            cacheUtils.executeWithLock(TASK_LOCK, 0, -1, TimeUnit.SECONDS, this::cleanupWithConfig);
        } catch (CacheException exception) {
            log.debug("未获取日志生命周期清理任务锁，本轮清理已跳过");
        } catch (Exception exception) {
            log.warn("日志生命周期清理任务执行异常，本轮清理已终止", exception);
        }
    }

    void cleanupWithConfig() {
        LogStorageConfig config = ConfigInfoUtils.getByObject(
                LogStorageConfig.CONFIG_KEY,
                LogStorageConfig.class);
        cleanup(config);
    }

    void cleanup(LogStorageConfig config) {
        int operationRemoved = cleanupOperationLog(config.operationLog());
        int loginRemoved = cleanupLoginLog(config.loginLog());
        if (operationRemoved > 0 || loginRemoved > 0) {
            log.info("日志生命周期清理完成，操作日志删除 {} 条，登录日志删除 {} 条",
                    operationRemoved, loginRemoved);
        }
    }

    private int cleanupOperationLog(LogStorage storage) {
        Integer deleteMinAge = databaseDeleteMinAge(storage, "操作日志");
        if (deleteMinAge == null) {
            return 0;
        }
        return operationLogService.removeExpiredBefore(
                LocalDateTime.now().minusDays(deleteMinAge), DELETE_BATCH_SIZE);
    }

    private int cleanupLoginLog(LogStorage storage) {
        Integer deleteMinAge = databaseDeleteMinAge(storage, "登录日志");
        if (deleteMinAge == null) {
            return 0;
        }
        return loginLogService.removeExpiredBefore(
                LocalDateTime.now().minusDays(deleteMinAge), DELETE_BATCH_SIZE);
    }

    private Integer databaseDeleteMinAge(LogStorage storage, String logName) {
        if (storage == null || storage.medium() != SaveMedium.DATABASE) {
            return null;
        }
        LogLifeCycle lifeCycle = storage.dataLifeCycle();
        if (lifeCycle == null || lifeCycle.deleteMinAge() == null || lifeCycle.deleteMinAge() <= 0) {
            log.warn("{}数据库生命周期配置无效，本轮清理已跳过", logName);
            return null;
        }
        return lifeCycle.deleteMinAge();
    }
}
