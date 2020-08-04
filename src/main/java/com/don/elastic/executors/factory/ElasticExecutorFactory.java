package com.don.elastic.executors.factory;

import com.don.elastic.executors.excutor.ElasticExecutor;

import java.util.concurrent.TimeUnit;

/**
 * 线程池工厂
 * @author Don Du
 */
public interface ElasticExecutorFactory {

    /**
     * 根据poolName获取线程池
     * @param poolName
     * @return
     */
    ElasticExecutor getExecutor(String poolName);

    /**
     * 关闭所有线程池
     * @param timeout
     * @param timeUnit
     */
    void shutdown(long timeout, TimeUnit timeUnit);

}
