package com.don.elastic.executors.task;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认的线程池任务
 * @author Don Du
 */
public class DefaultExecutorTask<V> implements ExecutorTask<V> {

    private final ExecutorTaskContext taskContext;

    private final Callable<V> actualTask;

    private final AtomicReference<TaskState> state;

    public DefaultExecutorTask(ExecutorTaskContext taskContext, Callable<V> actualTask) {
        this.taskContext = taskContext;
        this.actualTask = actualTask;
        this.state = new AtomicReference<>(TaskState.CREATED);
    }

    @Override
    public ExecutorTaskContext getTaskContext() {
        return taskContext;
    }

    @Override
    public void changeState(TaskState newTaskState) {
        state.set(newTaskState);

        switch (newTaskState) {
            case CREATED:
            case COMMITTED:
            case REJECTED:
            case RUNNING:
            case SUCCESS:
            case FAILURE:
            case COMPLETED:
            default:

        }
    }

    @Override
    public void destroy() {
        changeState(TaskState.COMPLETED);
    }

    @Override
    public V call() throws Exception {
        return actualTask.call();
    }
}
