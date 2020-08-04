package com.don.elastic.executors.selector;

import com.don.elastic.executors.task.ExecutorTaskContext;

/**
 * @author Don Du
 */
public interface ExecutorSelector {

    /**
     * 根据上下文选择线程池
     * @param taskContext
     * @return
     */
    String selectPoolKey(ExecutorTaskContext taskContext);

}
