package com.don.elastic.executors.excutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 受监控的线程池
 * @author Don Du
 */
public interface MonitoringExecutor extends Executor {

    /**
     * 名称
     * @return
     */
    String getPoolName();

    /**
     * 主机名
     * @return
     */
    String getHost();

    /**
     * 激活的任务数
     * @return
     */
    int getActiveCount();

    /**
     * 核心线程数
     * @return
     */
    int getCorePoolSize();

    /**
     * 最大线程数
     * @return
     */
    int getMaximumPoolSize();

    /**
     * 当前线程数
     * @return
     */
    int getPoolSize();

    /**
     * 历史最大线程数
     * @return
     */
    int getLargestPoolSize();

    /**
     * 已完成任务数
     * @return
     */
    long getCompletedTaskCount();

    /**
     * 工作阻塞队列
     * @return
     */
    BlockingQueue<Runnable> getWorkQueue();

    /**
     * 阻塞队列类型名称
     * @return
     */
    String getWorkQueueType();

    /**
     * 阻塞队列容量
     * @return
     */
    int getWorkQueueCapacity();

    /**
     * 阻塞队列当前大小
     * @return
     */
    int getWorkQueueSize();

    /**
     * 阻塞队列剩余容量
     * @return
     */
    int getRemainingCapacity();

    /**
     * 拒绝策略
     * @return
     */
    RejectedExecutionHandler getRejectedExecutionHandler();

    /**
     * 拒绝策略类型
     * @return
     */
    String getRejectedExecutionHandlerType();

    /**
     * 拒绝的任务数
     * @return
     */
    long getRejectedTaskCount();

}
