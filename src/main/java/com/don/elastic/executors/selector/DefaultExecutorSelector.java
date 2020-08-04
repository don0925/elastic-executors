package com.don.elastic.executors.selector;

import com.don.elastic.executors.excutor.ElasticExecutor;
import com.don.elastic.executors.task.ExecutorTaskContext;

/**
 * 默认线程池选择器，直接选择默认线程池
 * @author Don Du
 */
public class DefaultExecutorSelector extends AbstractExecutorSelector {

    @Override
    protected ExecutorExpressionFunction provideFunction() {
        return null;
    }

    @Override
    protected String select(ExecutorTaskContext taskContext) {
        if (logger.isDebugEnabled()) {
            logger.debug("[DefaultExecutorSelector] 使用默认的线程池选择器，返回默认的线程池{}", ElasticExecutor.DEFAULT_POOL_NAME);
        }
        return ElasticExecutor.DEFAULT_POOL_NAME;
    }

}
