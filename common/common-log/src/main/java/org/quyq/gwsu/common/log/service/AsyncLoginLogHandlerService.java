package org.quyq.gwsu.common.log.service;

import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.api.ILogClientApi;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 有界队列异步提交认证日志。
 */
@Slf4j
public class AsyncLoginLogHandlerService
        implements LoginLogHandlerService, InitializingBean, DisposableBean {

    private static final int QUEUE_CAPACITY = 5000;

    private final ILogClientApi logClientApi;

    private final BlockingQueue<LogLoginVO> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final AtomicBoolean stopping = new AtomicBoolean(false);

    private Thread consumer;

    public AsyncLoginLogHandlerService(ILogClientApi logClientApi) {
        this.logClientApi = logClientApi;
    }

    @Override
    public void save(LogLoginVO loginLog) {
        if (!queue.offer(loginLog)) {
            log.warn("认证日志队列已满，日志提交失败：authorizationId={}", loginLog.getAuthorizationId());
        }
    }

    @Override
    public void afterPropertiesSet() {
        consumer = Thread.ofVirtual().name("login-log-consumer").start(this::consume);
    }

    private void consume() {
        while (!stopping.get()) {
            try {
                persist(queue.take());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                log.warn("认证日志记录异常：{}", exception.getMessage());
            }
        }
        LogLoginVO loginLog;
        while ((loginLog = queue.poll()) != null) {
            try {
                persist(loginLog);
            } catch (Exception exception) {
                log.warn("停止前认证日志记录异常：{}", exception.getMessage());
            }
        }
    }

    private void persist(LogLoginVO loginLog) {
        R<Boolean> result = logClientApi.saveLoginLog(loginLog);
        if (!result.isSuccess()) {
            log.warn("认证日志记录失败：authorizationId={}，原因={}",
                    loginLog.getAuthorizationId(), result.msg());
        }
    }

    @Override
    public void destroy() throws InterruptedException {
        stopping.set(true);
        if (consumer != null) {
            consumer.interrupt();
            consumer.join();
        }
    }
}
