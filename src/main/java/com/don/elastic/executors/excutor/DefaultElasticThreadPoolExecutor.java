package com.don.elastic.executors.excutor;

import com.don.elastic.executors.queue.ResizableBlockingQueue;
import com.don.elastic.executors.selector.DefaultExecutorSelector;
import com.don.elastic.executors.task.ExecutorTask;
import com.don.elastic.executors.task.TaskRejectedException;
import com.don.elastic.executors.util.Asserts;
import com.don.elastic.executors.util.NetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * 默认的线程池
 * @author Don Du
 */
public class DefaultElasticThreadPoolExecutor extends ThreadPoolExecutor implements ElasticExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticExecutor.class);

    /**
     * 线程池名称
     */
    private final String poolName;

    private int workQueueCapacity;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static ElasticExecutor form(String poolName) {
        return Builder.fromName(poolName);
    }


    public static class Builder {

        // cpu core
        private static final int CPU_PROCESSOR_SIZE = Runtime.getRuntime().availableProcessors();

        // 线程池名称
        private String poolName = ElasticExecutor.DEFAULT_POOL_NAME;

        // 核心线程数
        private int corePoolSize = CPU_PROCESSOR_SIZE;

        // 最大线程池
        private int maximumPoolSize = 2 * CPU_PROCESSOR_SIZE + 1;

        // 空闲保持时间
        private long keepAliveTime = 6000;

        // 阻塞队列容量
        private int capacity = 200;

        // 阻塞队列
        private BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(capacity);

        // 线程工厂
        private ThreadFactory threadFactory = new NamedThreadFactory(poolName);

        // 拒绝策略
        private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

        private Builder() {}

        public Builder poolName(String poolName) {
            Asserts.notEmpty(poolName, "poolName is empty");
            this.poolName = poolName;
            return this;
        }

        public Builder corePoolSize(int corePoolSize) {
            Asserts.isFalse(corePoolSize < 0, "corePoolSize <= 0");
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            Asserts.isFalse(maximumPoolSize <= 0 || maximumPoolSize < corePoolSize, "maximumPoolSize <= 0 || maximumPoolSize < corePoolSize");
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder keepAliveTime(long keepAliveTime) {
            Asserts.isFalse(keepAliveTime <= 0, "keepAliveTime <= 0");
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public Builder workQueue(BlockingQueue<Runnable> workQueue) {
            Asserts.notNull(workQueue, "workQueue == null");
            this.workQueue = workQueue;
            return this;
        }

        public Builder threadFactory(ThreadFactory threadFactory) {
            Asserts.notNull(threadFactory, "threadFactory == null");
            this.threadFactory = threadFactory;
            return this;
        }

        public Builder rejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
            Asserts.notNull(rejectedExecutionHandler, "rejectedExecutionHandler == null");
            this.rejectedExecutionHandler = rejectedExecutionHandler;
            return this;
        }

        public ElasticExecutor build() {
            return new DefaultElasticThreadPoolExecutor(poolName, corePoolSize, maximumPoolSize, keepAliveTime, workQueue, threadFactory, rejectedExecutionHandler);
        }

        private static ElasticExecutor fromName(String poolName) {
            return new Builder().poolName(poolName).build();
        }

    }

    /**
     * 任务拒绝处理包装
     */
    private static class RejectedExecutionHandlerWrapper extends LongAdder implements RejectedExecutionHandler {

        private final RejectedExecutionHandler policy;

        public RejectedExecutionHandlerWrapper(RejectedExecutionHandler policy) {
            this.policy = policy;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            this.increment();
            try {
                policy.rejectedExecution(r, executor);
            } catch (RejectedExecutionException ex) {
                throw new TaskRejectedException(ex);
            }
            throw new TaskRejectedException();
        }

        public long getRejectedCount() {
            return this.longValue();
        }
    }

    @Override
    public <V> Future<V> submit(ExecutorTask<V> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<V> futureTask = newTaskFor(task);
        try {
            execute(futureTask);
        } catch (TaskRejectedException ex) {
            task.changeState(ExecutorTask.TaskState.REJECTED);
            // 如果拒绝策略存在抛出的异常，抛出原始的拒绝异常
            if (ex.existRejectedExecutionException()) {
                throw ex.getRejectedExecutionException();
            }
        } catch (Throwable ex) {
            task.changeState(ExecutorTask.TaskState.FAILURE);
        } finally {
            task.destroy();
        }
        return futureTask;
    }

    @Override
    public void shutdown(long timeout, TimeUnit timeUnit) {
        this.shutdown();

        try {
           boolean terminated = awaitTermination(timeout, timeUnit);
           if (terminated) {
               if (LOGGER.isDebugEnabled()) {
                   LOGGER.debug("线程池{}关闭成功", poolName);
               }
           } else {
               if (LOGGER.isWarnEnabled()) {
                   LOGGER.warn("在{}{}内，线程池{}没有成功关闭", timeout, timeUnit.toString(), poolName);
               }
           }
        } catch (InterruptedException e) {
            LOGGER.error("线程池{}关闭过程中发生中断异常", poolName);
        }
    }

    @Override
    public void setCorePoolSize(int corePoolSize) {
        int oldCorePoolSize = getCorePoolSize();
        if (corePoolSize == oldCorePoolSize) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: corePoolSize 新值: {}和旧值: {}一样，不做修改更新操作", poolName, corePoolSize, oldCorePoolSize);
            }
        } else {
            super.setCorePoolSize(corePoolSize);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: corePoolSize 已被修改生效，新值: {}, 旧值: {}", poolName, corePoolSize, oldCorePoolSize);
            }
        }
    }

    @Override
    public void setMaximumPoolSize(int maximumPoolSize) {
        int oldMaximumPoolSize = getMaximumPoolSize();
        if (oldMaximumPoolSize == maximumPoolSize) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: maximumPoolSize 新值: {}和旧值: {}一样，不做修改更新操作", poolName, maximumPoolSize, oldMaximumPoolSize);
            }
        } else {
            super.setMaximumPoolSize(maximumPoolSize);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: maximumPoolSize 已被修改生效，新值: {}, 旧值: {}", poolName, maximumPoolSize, oldMaximumPoolSize);
            }
        }
    }

    @Override
    public void setKeepAliveTime(long time, TimeUnit unit) {
        long oldKeepAliveTime = getKeepAliveTime(unit);
        if (time == oldKeepAliveTime) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: keepAliveTime 新值: {}和旧值: {}一样，不做修改更新操作", poolName, time, oldKeepAliveTime);
            }
            return ;
        } else {
            super.setKeepAliveTime(time, unit);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: keepAliveTime 已被修改生效，新值: {}, 旧值: {}", poolName, time, oldKeepAliveTime);
            }
        }
    }

    @Override
    public void setWorkQueueCapacity(int newWorkQueueCapacity) {
        BlockingQueue<Runnable> workQueue = this.getQueue();
        if (newWorkQueueCapacity == workQueueCapacity) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: workQueueCapacity 新值: {}和旧值: {}一样，不做修改更新操作", poolName, newWorkQueueCapacity, workQueueCapacity);
            }
        }
        if (workQueue instanceof ResizableBlockingQueue) {
            ((ResizableBlockingQueue)workQueue).setCapacity(newWorkQueueCapacity);
            this.workQueueCapacity = newWorkQueueCapacity;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("线程池{}: 工作队列{}，容量 workQueueCapacity 已被修改生效，新值: {}, 旧值: {}", poolName, workQueue.getClass().getSimpleName(), newWorkQueueCapacity, workQueueCapacity);
            }
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("线程池{}: 工作队列{}不支持修改容量", poolName, workQueue.getClass().getSimpleName());
            }
        }
    }

    @Override
    public String getPoolName() {
        return this.poolName;
    }

    @Override
    public String getHost() {
        return NetUtils.getLocalHost();
    }

    @Override
    public int getActiveCount() {
        return super.getActiveCount();
    }

    @Override
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return super.getRejectedExecutionHandler();
    }

    @Override
    public int getCorePoolSize() {
        return super.getCorePoolSize();
    }

    @Override
    public int getMaximumPoolSize() {
        return super.getMaximumPoolSize();
    }

    @Override
    public long getKeepAliveTime(TimeUnit unit) {
        return super.getKeepAliveTime(unit);
    }

    @Override
    public int getPoolSize() {
        return super.getPoolSize();
    }

    @Override
    public int getLargestPoolSize() {
        return super.getLargestPoolSize();
    }

    @Override
    public long getCompletedTaskCount() {
        return super.getCompletedTaskCount();
    }

    @Override
    public BlockingQueue<Runnable> getWorkQueue() {
        return getQueue();
    }

    @Override
    public String getWorkQueueType() {
        return getQueue().getClass().getSimpleName();
    }

    @Override
    public int getWorkQueueCapacity() {
        return this.workQueueCapacity;
    }

    @Override
    public int getWorkQueueSize() {
        return getQueue().size();
    }

    @Override
    public int getRemainingCapacity() {
        return getQueue().remainingCapacity();
    }

    @Override
    public String getRejectedExecutionHandlerType() {
        return getRejectedExecutionHandler().getClass().getSimpleName();
    }

    @Override
    public long getRejectedTaskCount() {
        return ((RejectedExecutionHandlerWrapper) getRejectedExecutionHandler()).getRejectedCount();
    }

    private DefaultElasticThreadPoolExecutor(String poolName, int corePoolSize, int maximumPoolSize, long keepAliveTime, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue, threadFactory, new RejectedExecutionHandlerWrapper(handler));
        this.poolName = poolName;
        // 队列刚创建剩余容量==容量
        this.workQueueCapacity = workQueue.remainingCapacity();
    }

}
