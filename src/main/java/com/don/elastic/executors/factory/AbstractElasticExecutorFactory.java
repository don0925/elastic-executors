package com.don.elastic.executors.factory;

import com.don.elastic.executors.excutor.ElasticExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 抽象将真正的线程池创建逻辑交给子类，懒启动
 * @author Don Du
 */
public abstract class AbstractElasticExecutorFactory implements ElasticExecutorFactory {

    private final ConcurrentMap<String, ElasticExecutor> cachedExecutors = new ConcurrentHashMap<>();

    /**
     * 获取指定名称的线程池，如果获取不到返回内置默认线程池
     * @param poolKey
     * @return
     */
    @Override
    public ElasticExecutor getExecutor(String poolKey) {
        ElasticExecutor executor = cachedExecutors.get(poolKey);
        if (executor == null) {
            synchronized (this) {
                executor = cachedExecutors.get(poolKey);
                if (executor == null) {
                    executor = createExecutor(poolKey);
                    cachedExecutors.putIfAbsent(poolKey, executor);
                }
            }
        }
        return executor;
    }

    @Override
    public synchronized void shutdown(long timeout, TimeUnit timeUnit) {
        for (Map.Entry<String, ElasticExecutor> entry : cachedExecutors.entrySet()) {
            ElasticExecutor executor = entry.getValue();
            executor.shutdown(timeout, timeUnit);
        }
    }

    /**
     * 创建线程池
     * @param poolName
     * @return
     */
    protected abstract ElasticExecutor createExecutor(String poolName);

}
