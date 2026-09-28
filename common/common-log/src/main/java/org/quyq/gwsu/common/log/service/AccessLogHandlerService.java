package org.quyq.gwsu.common.log.service;


import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.api.ILogClientApi;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步操作日志处理服务
 * <p>
 * 使用多队列+哈希分片，确保相同 operId 的日志（请求/响应两次 save）始终路由到同一队列，
 * 由同一虚拟线程顺序消费，保证时序正确。
 * </p>
 *
 * @author Quyq
 */
@Slf4j
public class AccessLogHandlerService implements InitializingBean, DisposableBean {

    private static final int QUEUE_CAPACITY = 5000;

    private final List<BlockingQueue<LogOperationVO>> queues;

    private final AtomicBoolean stopping = new AtomicBoolean(false);

    private final List<Thread> consumers = new ArrayList<>();

    private final ILogClientApi logClientApi;

    private final int threadCount;

    public AccessLogHandlerService(ILogClientApi logClientApi, int threadCount) {
        this.logClientApi = logClientApi;
        this.threadCount = threadCount;
        this.queues = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            queues.add(new LinkedBlockingQueue<>(QUEUE_CAPACITY));
        }
    }

    /**
     * 保存操作日志到队列，相同 operId 的日志会路由到同一队列，保证消费顺序
     */
    public void save(LogOperationVO vo) {
        if (!StringUtils.hasText(vo.getOperId())) {
            vo.setOperId(IdUtil.getSnowflakeNextIdStr());
        }
        LogOperationVO snapshot = vo.snapshot();
        int index = getQueueIndex(snapshot.getOperId());
        boolean offered = queues.get(index).offer(snapshot);
        if (!offered) {
            log.warn("日志队列【{}】已满，操作日志记录失败：operId={}", index, snapshot.getOperId());
        }
    }

    private int getQueueIndex(String operId) {
        return Math.abs(operId.hashCode() % queues.size());
    }

    @Override
    public void afterPropertiesSet() {
        for (int i = 0; i < threadCount; i++) {
            Thread consumer = Thread.ofVirtual()
                    .name("log-consumer-" + i)
                    .unstarted(new LogConsumer(logClientApi, stopping, queues.get(i)));
            consumer.start();
            consumers.add(consumer);
        }
        log.info("操作日志消费者虚拟线程启动成功，数量：{}", threadCount);
    }

    @Override
    public void destroy() {
        stopping.set(true);
        for (Thread consumer : consumers) {
            consumer.interrupt();
            try {
                consumer.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        consumers.clear();
        log.info("操作日志消费者虚拟线程已销毁");
    }

    private static class LogConsumer implements Runnable {

        private final ILogClientApi logClientApi;
        private final AtomicBoolean stopping;
        private final BlockingQueue<LogOperationVO> queue;

        LogConsumer(ILogClientApi logClientApi, AtomicBoolean stopping, BlockingQueue<LogOperationVO> queue) {
            this.logClientApi = logClientApi;
            this.stopping = stopping;
            this.queue = queue;
        }

        @Override
        public void run() {
            while (!stopping.get()) {
                try {
                    LogOperationVO vo = queue.take();
                    R<Boolean> result = logClientApi.saveOperLog(vo);
                    if (result == null || !result.isSuccess() || !Boolean.TRUE.equals(result.data())) {
                        String reason = result == null
                                ? "日志服务无响应"
                                : result.isSuccess() ? "日志服务未写入数据" : result.msg();
                        log.warn("操作日志记录失败：operId={}，原因：{}", vo.getOperId(), reason);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    log.warn("操作日志记录异常：{}", ex.getMessage());
                }
            }
            // 停止前消费剩余日志
            drainRemaining();
        }

        private void drainRemaining() {
            LogOperationVO vo;
            while ((vo = queue.poll()) != null) {
                try {
                    logClientApi.saveOperLog(vo);
                } catch (Exception ex) {
                    log.warn("停止前操作日志记录异常：operId={}，原因：{}", vo.getOperId(), ex.getMessage());
                }
            }
        }
    }

}
