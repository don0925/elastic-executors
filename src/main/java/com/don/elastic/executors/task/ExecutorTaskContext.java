package com.don.elastic.executors.task;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程池任务执行上下文
 * @author Don Du
 */
public class ExecutorTaskContext {

    private String taskName;

    private Map<Object, Object> parameters;

    public ExecutorTaskContext(String taskName, Map<Object, Object> parameters) {
        this.taskName = taskName;
        this.parameters = parameters;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Map<Object, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<Object, Object> parameters) {
        this.parameters = parameters;
    }
}
