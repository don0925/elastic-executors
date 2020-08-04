package com.don.elastic.executors.task;

import java.util.concurrent.RejectedExecutionException;

/**
 * 任务提交被拒绝异常
 * @author Don Du
 */
public class TaskRejectedException extends RejectedExecutionException {

    private RejectedExecutionException executionException;

    public TaskRejectedException() {
    }

    public TaskRejectedException(RejectedExecutionException executionException) {
        super(executionException);
        this.executionException = executionException;
    }

    public RejectedExecutionException getRejectedExecutionException() {
        return executionException;
    }

    public boolean existRejectedExecutionException() {
        return executionException != null;
    }

}
