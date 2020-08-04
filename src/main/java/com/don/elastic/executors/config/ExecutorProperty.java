package com.don.elastic.executors.config;

import com.don.elastic.executors.excutor.ElasticExecutor;
import com.don.elastic.executors.excutor.NamedThreadFactory;
import com.don.elastic.executors.queue.ResizableLinkedBlockingQueue;
import com.don.elastic.executors.util.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @author Don Du
 */
public class ExecutorProperty {

    // cpu core
    private static final int CPU_PROCESSOR_SIZE = Runtime.getRuntime().availableProcessors();
    // 线程池名称
    private static final String DEFAULT_POOL_NAME = ElasticExecutor.DEFAULT_POOL_NAME;
    // 核心线程数
    private static final int DEFAULT_CORE_POOL_SIZE = CPU_PROCESSOR_SIZE;
    // 最大线程池
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 2 * CPU_PROCESSOR_SIZE + 1;
    // 空闲保持时间
    private static final int DEFAULT_KEEP_ALIVE_TIME = 6000;
    // 阻塞队列容量
    private static final int DEFAULT_QUEUE_CAPACITY = 200;
    // 阻塞队列类型
    private static final String DEFAULT_WORK_QUEUE_TYPE = LinkedBlockingDeque.class.getSimpleName();
    // 拒绝策略类型
    private static final String DEFAULT_REJECTED_HANDLER_TYPE = ThreadPoolExecutor.AbortPolicy.class.getSimpleName();
    // 默认选择器表达式
    private static final String DEFAULT_SELECTOR_EXPRESSION = "";

    private static final String POOL_NAME = "name";
    private static final String CORE_POOL_SIZE = "corePoolSize";
    private static final String MAXIMUM_POOL_SIZE = "maximumPoolSize";
    private static final String KEEP_ALIVE_TIME = "keepAliveTime";
    private static final String QUEUE_CAPACITY = "queueCapacity";
    private static final String WORK_QUEUE_TYPE = "workQueueType";
    private static final String REJECTED_HANDLER_TYPE = "rejectedHandlerType";
    private static final String SELECTOR_EXPRESSION = "expression";

    private final Map<String, String> properties;

    private static final Map<String, BlockingQueue<Runnable>> BLOCKING_QUEUES = new HashMap<>();
    private static final Map<String, RejectedExecutionHandler> REJECTED_HANDLERS = new HashMap<>();

    private final ThreadFactory threadFactory;

    public ExecutorProperty() {
        properties = new HashMap<>();
        properties.put(POOL_NAME, DEFAULT_POOL_NAME);
        properties.put(CORE_POOL_SIZE, DEFAULT_CORE_POOL_SIZE + "");
        properties.put(MAXIMUM_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE + "");
        properties.put(KEEP_ALIVE_TIME, DEFAULT_KEEP_ALIVE_TIME + "");
        properties.put(QUEUE_CAPACITY, DEFAULT_QUEUE_CAPACITY + "");
        properties.put(WORK_QUEUE_TYPE, DEFAULT_WORK_QUEUE_TYPE);
        properties.put(REJECTED_HANDLER_TYPE, DEFAULT_REJECTED_HANDLER_TYPE);
        properties.put(SELECTOR_EXPRESSION, DEFAULT_SELECTOR_EXPRESSION);

        BLOCKING_QUEUES.put(ResizableLinkedBlockingQueue.class.getSimpleName(), new DelayQueue());
        BLOCKING_QUEUES.put(LinkedBlockingQueue.class.getSimpleName(), new LinkedBlockingQueue<>());
        BLOCKING_QUEUES.put(ArrayBlockingQueue.class.getSimpleName(), new ArrayBlockingQueue<>(getQueueCapacity()));
        BLOCKING_QUEUES.put(SynchronousQueue.class.getSimpleName(), new SynchronousQueue<>());
        BLOCKING_QUEUES.put(LinkedTransferQueue.class.getSimpleName(), new LinkedTransferQueue<>());
        BLOCKING_QUEUES.put(PriorityBlockingQueue.class.getSimpleName(), new PriorityBlockingQueue<>(getQueueCapacity()));
        BLOCKING_QUEUES.put(DelayQueue.class.getSimpleName(), new DelayQueue());

        REJECTED_HANDLERS.put("AbortPolicy", new ThreadPoolExecutor.AbortPolicy());
        REJECTED_HANDLERS.put("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy());
        REJECTED_HANDLERS.put("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());
        REJECTED_HANDLERS.put("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy());

        threadFactory = new NamedThreadFactory(getPoolName());
    }

    public String getPoolName() {
        return Strings.blankDefault(properties.get(POOL_NAME), DEFAULT_POOL_NAME);
    }

    public int getCorePoolSize() {
        return Strings.blankDefaultInt(properties.get(CORE_POOL_SIZE), DEFAULT_CORE_POOL_SIZE);
    }

    public int getMaximumPoolSize() {
        return Strings.blankDefaultInt(properties.get(MAXIMUM_POOL_SIZE), DEFAULT_MAXIMUM_POOL_SIZE);
    }


    public long getKeepAliveTime() {
        return Strings.blankDefaultLong(properties.get(KEEP_ALIVE_TIME), DEFAULT_KEEP_ALIVE_TIME);
    }

    public String getWorkQueueType() {
        return Strings.blankDefault(properties.get(WORK_QUEUE_TYPE), DEFAULT_WORK_QUEUE_TYPE);
    }

    public BlockingQueue<Runnable> getWorkQueue() {
        return BLOCKING_QUEUES.get(getWorkQueueType());
    }

    public int getQueueCapacity() {
        return Strings.blankDefaultInt(properties.get(QUEUE_CAPACITY), DEFAULT_QUEUE_CAPACITY);
    }

    public String getRejectedHandlerType() {
        return Strings.blankDefault(properties.get(REJECTED_HANDLER_TYPE), DEFAULT_REJECTED_HANDLER_TYPE);
    }

    public RejectedExecutionHandler getRejectedHandler() {
        return REJECTED_HANDLERS.get(getRejectedHandlerType());
    }

    public String getExpression() {
        return Strings.blankDefault(properties.get(SELECTOR_EXPRESSION), DEFAULT_SELECTOR_EXPRESSION);
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void addProperty(String key, String value) {
        this.properties.put(key, value);
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ExecutorProperty)) {
            return false;
        }
        ExecutorProperty other = (ExecutorProperty) object;
        return this.getPoolName().equals(other.getPoolName());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPoolName());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ExecutorProperty[");
        builder.append("poolName=").append(getPoolName()).append(", ")
                .append("corePoolSize=").append(getCorePoolSize()).append(", ")
                .append("maximumPoolSize=").append(getMaximumPoolSize()).append(", ")
                .append("keepAliveTime=").append(getKeepAliveTime()).append(", ")
                .append("workQueueType=").append(getWorkQueueType()).append(", ")
                .append("queueCapacity=").append(getQueueCapacity()).append(", ")
                .append("rejectedHandlerType=").append(getRejectedHandlerType()).append(", ")
                .append("expression=").append(getExpression()).append(", ")
                .append("]");
        return builder.toString();
    }

    static ExecutorProperty named(String poolName) {
        return new NamedExecutorProperty(poolName);
    }

    static class NamedExecutorProperty extends ExecutorProperty {

        public NamedExecutorProperty(String poolName) {
            super();
            addProperty(POOL_NAME, poolName);
        }
    }
}