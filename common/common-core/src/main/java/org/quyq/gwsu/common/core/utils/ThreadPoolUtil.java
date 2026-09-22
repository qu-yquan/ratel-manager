package org.quyq.gwsu.common.core.utils;


import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextScheduledExecutorService;
import io.micrometer.context.ContextSnapshotFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * 创建线程池工具类，可实现参数父子继承
 */
public class ThreadPoolUtil {

    private ThreadPoolUtil() {
    }

    private final static ContextSnapshotFactory FACTORY = getContextSnapshotFactory();

    /**
     * 创建单线程执行器
     *
     * @return
     */
    public static ExecutorService newSingleThreadExecutor() {
        return ContextExecutorService.wrap(Executors.newSingleThreadExecutor(), FACTORY);
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory factory) {
        return ContextExecutorService.wrap(Executors.newSingleThreadExecutor(factory), FACTORY);
    }

    public static ExecutorService newVirtualThreadPerTaskExecutor() {
        return ContextExecutorService.wrap(Executors.newVirtualThreadPerTaskExecutor(), FACTORY);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return ContextScheduledExecutorService.wrap(Executors.newSingleThreadScheduledExecutor(), FACTORY);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory factory) {
        return ContextScheduledExecutorService.wrap(Executors.newSingleThreadScheduledExecutor(factory), FACTORY);
    }

    /**
     * 创建固定数量线程池
     *
     * @param nThreads
     * @return
     */
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return getExecutorService(Executors.newFixedThreadPool(nThreads));
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory factory) {
        return getExecutorService(Executors.newFixedThreadPool(nThreads, factory));
    }

    public static ExecutorService newCachedThreadPool() {
        return getExecutorService(Executors.newCachedThreadPool());
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory factory) {
        return getExecutorService(Executors.newCachedThreadPool(factory));
    }

    public static ExecutorService newWorkStealingPool() {
        return getExecutorService(Executors.newWorkStealingPool());
    }

    public static ExecutorService newWorkStealingPool(int parallelism) {
        return getExecutorService(Executors.newWorkStealingPool(parallelism));
    }

    public static ExecutorService getExecutorService(ExecutorService executorService) {
        return ContextExecutorService.wrap(executorService, FACTORY);
    }


    private static ContextSnapshotFactory getContextSnapshotFactory() {
        return ContextSnapshotFactory.builder()
                .contextRegistry(ContextRegistry.getInstance())
                .clearMissing(true)
                .build();
    }


}
