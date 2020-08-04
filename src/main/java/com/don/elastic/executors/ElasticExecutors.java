package com.don.elastic.executors;

import com.don.elastic.executors.factory.ElasticExecutorFactory;
import com.don.elastic.executors.task.ExecutorTask;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 复合弹性线程池
 * @author Don Du
 */
public interface ElasticExecutors {

    /**
     * 根据任务上下文，提交任务到某一个线程池
     * @param task
     * @param <V>
     */
    <V> Future<V> submit(ExecutorTask<V> task);

    /**
     * 关闭线程池
     * @param timeout
     * @param timeUnit
     */
    void shutdown(int timeout, TimeUnit timeUnit);

}
