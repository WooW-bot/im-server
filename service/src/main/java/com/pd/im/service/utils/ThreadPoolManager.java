package com.pd.im.service.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理器
 * <p>
 * 提供统一的线程池创建、管理和监控功能
 * <p>
 * 特性：
 * 1. 支持创建多个命名线程池（业务隔离）
 * 2. 灵活配置线程池参数
 * 3. 自动监控慢任务
 * 4. 统一优雅关闭
 * 5. 线程池状态监控
 *
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
@Component
public class ThreadPoolManager {

    /**
     * 线程池存储
     * Key: 线程池名称
     * Value: 线程池包装对象
     */
    private final Map<String, ManagedThreadPool> threadPools = new ConcurrentHashMap<>();

    /**
     * 默认线程池配置
     */
    public static class ThreadPoolConfig {
        /**
         * 核心线程数
         */
        private int corePoolSize = 8;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 16;

        /**
         * 队列容量
         */
        private int queueCapacity = 2000;

        /**
         * 线程空闲时间（秒）
         */
        private long keepAliveSeconds = 60L;

        /**
         * 拒绝策略
         */
        private RejectedExecutionHandler rejectedHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        /**
         * 是否允许核心线程超时
         */
        private boolean allowCoreThreadTimeOut = true;

        /**
         * 慢任务阈值（毫秒）
         */
        private long slowTaskThreshold = 1000L;

        public static ThreadPoolConfig create() {
            return new ThreadPoolConfig();
        }

        public ThreadPoolConfig corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public ThreadPoolConfig maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public ThreadPoolConfig queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public ThreadPoolConfig keepAliveSeconds(long keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
            return this;
        }

        public ThreadPoolConfig rejectedHandler(RejectedExecutionHandler rejectedHandler) {
            this.rejectedHandler = rejectedHandler;
            return this;
        }

        public ThreadPoolConfig allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
            this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
            return this;
        }

        public ThreadPoolConfig slowTaskThreshold(long slowTaskThreshold) {
            this.slowTaskThreshold = slowTaskThreshold;
            return this;
        }
    }

    /**
     * 创建线程池（使用默认配置）
     *
     * @param poolName 线程池名称
     * @return 线程池执行器
     */
    public ThreadPoolExecutor createThreadPool(String poolName) {
        return createThreadPool(poolName, ThreadPoolConfig.create());
    }

    /**
     * 创建线程池（自定义配置）
     *
     * @param poolName 线程池名称
     * @param config   线程池配置
     * @return 线程池执行器
     */
    public ThreadPoolExecutor createThreadPool(String poolName, ThreadPoolConfig config) {
        if (threadPools.containsKey(poolName)) {
            log.warn("线程池已存在，返回已有线程池: {}", poolName);
            return threadPools.get(poolName).getExecutor();
        }

        log.info("创建线程池: name={}, coreSize={}, maxSize={}, queueCapacity={}",
                poolName, config.corePoolSize, config.maxPoolSize, config.queueCapacity);

        // 创建线程工厂
        ThreadFactory threadFactory = createThreadFactory(poolName);

        // 创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                config.corePoolSize,
                config.maxPoolSize,
                config.keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.queueCapacity),
                threadFactory,
                config.rejectedHandler
        );

        // 配置核心线程超时
        executor.allowCoreThreadTimeOut(config.allowCoreThreadTimeOut);

        // 包装并存储
        ManagedThreadPool managedPool = new ManagedThreadPool(poolName, executor, config);
        threadPools.put(poolName, managedPool);

        return executor;
    }

    /**
     * 获取线程池
     *
     * @param poolName 线程池名称
     * @return 线程池执行器，不存在返回null
     */
    public ThreadPoolExecutor getThreadPool(String poolName) {
        ManagedThreadPool managedPool = threadPools.get(poolName);
        return managedPool != null ? managedPool.getExecutor() : null;
    }

    /**
     * 提交任务（带监控）
     *
     * @param poolName 线程池名称
     * @param task     任务
     */
    public void submit(String poolName, Runnable task) {
        ManagedThreadPool managedPool = threadPools.get(poolName);
        if (managedPool == null) {
            log.error("线程池不存在: {}", poolName);
            throw new IllegalArgumentException("线程池不存在: " + poolName);
        }

        managedPool.submit(task);
    }

    /**
     * 获取线程池状态
     *
     * @param poolName 线程池名称
     * @return 状态信息
     */
    public String getThreadPoolStatus(String poolName) {
        ManagedThreadPool managedPool = threadPools.get(poolName);
        if (managedPool == null) {
            return "线程池不存在: " + poolName;
        }

        return managedPool.getStatus();
    }

    /**
     * 获取所有线程池状态
     *
     * @return 状态信息
     */
    public Map<String, String> getAllThreadPoolStatus() {
        Map<String, String> statusMap = new ConcurrentHashMap<>();
        threadPools.forEach((name, pool) -> statusMap.put(name, pool.getStatus()));
        return statusMap;
    }

    /**
     * 创建线程工厂
     *
     * @param poolName 线程池名称
     * @return 线程工厂
     */
    private ThreadFactory createThreadFactory(String poolName) {
        AtomicInteger threadCounter = new AtomicInteger(0);

        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName(poolName + "-" + threadCounter.getAndIncrement());
            thread.setUncaughtExceptionHandler((t, e) ->
                    log.error("线程执行异常: thread={}, pool={}", t.getName(), poolName, e));
            return thread;
        };
    }

    /**
     * 优雅关闭所有线程池
     */
    @PreDestroy
    public void shutdown() {
        log.info("开始关闭所有线程池，总数: {}", threadPools.size());

        for (Map.Entry<String, ManagedThreadPool> entry : threadPools.entrySet()) {
            String poolName = entry.getKey();
            ManagedThreadPool managedPool = entry.getValue();

            try {
                log.info("关闭线程池: {}", poolName);
                managedPool.shutdown();
            } catch (Exception e) {
                log.error("关闭线程池异常: {}", poolName, e);
            }
        }

        log.info("所有线程池已关闭");
    }

    /**
     * 线程池包装类
     * <p>
     * 提供监控和管理功能
     */
    private static class ManagedThreadPool {
        private final String poolName;
        private final ThreadPoolExecutor executor;
        private final ThreadPoolConfig config;
        private final AtomicInteger pendingTasks = new AtomicInteger(0);

        public ManagedThreadPool(String poolName, ThreadPoolExecutor executor, ThreadPoolConfig config) {
            this.poolName = poolName;
            this.executor = executor;
            this.config = config;
        }

        public ThreadPoolExecutor getExecutor() {
            return executor;
        }

        /**
         * 提交任务（带监控）
         */
        public void submit(Runnable task) {
            pendingTasks.incrementAndGet();

            try {
                executor.execute(() -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        task.run();
                    } catch (Exception e) {
                        log.error("任务执行异常: pool={}", poolName, e);
                    } finally {
                        long duration = System.currentTimeMillis() - startTime;
                        int remaining = pendingTasks.decrementAndGet();

                        // 慢任务告警
                        if (duration > config.slowTaskThreshold) {
                            log.warn("检测到慢任务: pool={}, duration={}ms, remainingTasks={}",
                                    poolName, duration, remaining);
                        } else {
                            log.debug("任务完成: pool={}, duration={}ms, remainingTasks={}",
                                    poolName, duration, remaining);
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                pendingTasks.decrementAndGet();
                log.error("任务被拒绝: pool={}", poolName, e);
                throw e;
            }
        }

        /**
         * 获取线程池状态
         */
        public String getStatus() {
            return String.format(
                    "线程池[%s] - 核心线程数:%d, 最大线程数:%d, 活跃线程数:%d, 队列大小:%d/%d, 已完成任务数:%d, 待处理任务数:%d",
                    poolName,
                    executor.getCorePoolSize(),
                    executor.getMaximumPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    config.queueCapacity,
                    executor.getCompletedTaskCount(),
                    pendingTasks.get()
            );
        }

        /**
         * 优雅关闭
         */
        public void shutdown() {
            log.info("关闭线程池: {}, 当前状态: {}", poolName, getStatus());

            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("线程池未在30秒内完成关闭，强制关闭: {}", poolName);
                    executor.shutdownNow();

                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        log.error("线程池强制关闭失败: {}", poolName);
                    }
                }
                log.info("线程池已关闭: {}", poolName);
            } catch (InterruptedException e) {
                log.error("关闭线程池时被中断: {}", poolName, e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
