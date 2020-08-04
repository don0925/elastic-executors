package com.don.elastic.executors.excutor;

import com.don.elastic.executors.task.ExecutorTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 弹性的线程池executor，默认底层采用JDK线程池实现
 * @author Don Du
 */
public interface ElasticExecutor extends ConfigurableExecutor, MonitoringExecutor, ExecutorService {

    /**
     * 默认线程池名称
     */
    String DEFAULT_POOL_NAME = "default-executor";

    /**
     * 提交任务到本线程池
     * @param task
     * @param <V>
     * @return
     */
    <V> Future<V> submit(ExecutorTask<V> task);

    /**
     * 关闭线程池
     * @param timeout
     * @param timeUnit
     */
    void shutdown(long timeout, TimeUnit timeUnit);

}
