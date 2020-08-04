package com.don.elastic.executors.excutor;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 可动态修改配置参数的线程池
 * @author Don Du
 */
public interface ConfigurableExecutor extends Executor {

    /**
     * 修改核心线程数
     * @param corePoolSize
     */
    void setCorePoolSize(int corePoolSize);

    /**
     * 修改最大线程数
     * @param maximumPoolSize
     */
    void setMaximumPoolSize(int maximumPoolSize);

    /**
     * 修改空闲保持时间
     * @param time
     * @param unit
     */
    void setKeepAliveTime(long time, TimeUnit unit);

    /**
     * 修改工作队列容量
     * @param workQueueCapacity
     */
    void setWorkQueueCapacity(int workQueueCapacity);

}
