package com.don.elastic.executors;

import com.don.elastic.executors.excutor.ElasticExecutor;
import com.don.elastic.executors.factory.DefaultElasticExecutorFactory;
import com.don.elastic.executors.factory.ElasticExecutorFactory;
import com.don.elastic.executors.selector.DefaultExecutorSelector;
import com.don.elastic.executors.selector.ExecutorSelector;
import com.don.elastic.executors.task.ExecutorTask;
import com.don.elastic.executors.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 复合线程池默认实现
 *
 * @author Don Du
 */
public class DefaultElasticExecutors implements ElasticExecutors {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticExecutors.class);
    /**
     * 线程池选择器
     */
    private ExecutorSelector selector = new DefaultExecutorSelector();

    /**
     * 线程池工厂
     */
    private ElasticExecutorFactory executorFactory = new DefaultElasticExecutorFactory();


    @Override
    public <V> Future<V> submit(ExecutorTask<V> task) {
        Asserts.notNull(task, "task == null");
        String poolKey = selector.selectPoolKey(task.getTaskContext());
        ElasticExecutor executor = executorFactory.getExecutor(poolKey);
        return executor.submit(task);
    }

    @Override
    public void shutdown(int timeout, TimeUnit timeUnit) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("开始关闭复合弹性线程池, 等待时间: {}, 单位: {}", timeout, timeUnit);
        }
        executorFactory.shutdown(timeout, timeUnit);
    }

}
