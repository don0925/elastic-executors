package com.don.elastic.executors.task;

import java.util.concurrent.Callable;

/**
 * 线程池任务
 * @author Don Du
 */
public interface ExecutorTask<V> extends Callable<V> {

    /**
     * 获取任务执行上下文，主要用于线程池选择器
     * @return
     */
    ExecutorTaskContext getTaskContext();

    /**
     * 改变任务状态
     * @param newTaskState
     */
    void changeState(TaskState newTaskState);

    /**
     * 销毁任务，主要做一些清理工作
     */
    void destroy();

    /**
     * 任务状态
     */
    enum TaskState {

        /**
         * 已创建
         */
        CREATED,

        /**
         * 被拒绝
         */
        REJECTED,

        /**
         * 已提交
         */
        COMMITTED,

        /**
         * 正在执行
         */
        RUNNING,

        /**
         * 执行成功
         */
        SUCCESS,

        /**
         * 执行失败
         */
        FAILURE,

        /**
         * 已完成
         */
        COMPLETED
    }

}
