package com.don.elastic.executors.factory;

import com.don.elastic.executors.config.ExecutorProperty;
import com.don.elastic.executors.config.ExecutorsProperty;
import com.don.elastic.executors.excutor.DefaultElasticThreadPoolExecutor;
import com.don.elastic.executors.excutor.ElasticExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jdk线程池工厂
 * @author Don Du
 */
public class DefaultElasticExecutorFactory extends AbstractElasticExecutorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultElasticExecutorFactory.class);

    @Override
    protected ElasticExecutor createExecutor(String poolKey) {
        ExecutorProperty property = ExecutorsProperty.getExecutorProperty(poolKey);
        if (property != null) {
            return DefaultElasticThreadPoolExecutor.newBuilder()
                    .poolName(property.getPoolName())
                    .corePoolSize(property.getCorePoolSize())
                    .maximumPoolSize(property.getMaximumPoolSize())
                    .keepAliveTime(property.getKeepAliveTime())
                    .workQueue(property.getWorkQueue())
                    .rejectedExecutionHandler(property.getRejectedHandler())
                    .threadFactory(property.getThreadFactory())
                    .build();
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("没有找到线程池{}, 将使用默认线程池进行任务处理", poolKey);
        }
        return DefaultElasticThreadPoolExecutor.form(poolKey);
    }
}
