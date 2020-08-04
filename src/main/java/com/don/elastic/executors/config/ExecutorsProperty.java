package com.don.elastic.executors.config;

import com.don.elastic.executors.excutor.ElasticExecutor;
import com.don.elastic.executors.selector.DefaultExecutorSelector;
import com.don.elastic.executors.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author Don Du
 */
public class ExecutorsProperty {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorsProperty.class);

    private static final String DEFAULT_PROPERTIES_FILE_NAME = "elastic-executors.properties";

    private static final String PROPERTY_PREFIX = "elastic.executors.";
    private static final String SELECTOR = PROPERTY_PREFIX + "selector";
    private static final String METRICS_ENABLE = PROPERTY_PREFIX + "metricsEnable";
    private static final String EXECUTOR = PROPERTY_PREFIX + "executor";

    private static final String DEFAULT_SELECTOR = DefaultExecutorSelector.class.getName();
    private static final List<ExecutorProperty> EXECUTOR_PROPERTY_LIST = new ArrayList<>();
    private static final String DEFAULT_METRICS_ENABLE = "false";

    private static final Map<String, String> props = new ConcurrentHashMap<>();

    private static final String POOL_NAME = "name";
    private static final String CORE_POOL_SIZE = "corePoolSize";
    private static final String MAXIMUM_POOL_SIZE = "maximumPoolSize";
    private static final String KEEP_ALIVE_TIME = "keepAliveTime";
    private static final String QUEUE_CAPACITY = "queueCapacity";
    private static final String WORK_QUEUE_TYPE = "workQueueType";
    private static final String REJECTED_HANDLER_TYPE = "rejectedHandlerType";
    private static final String SELECTOR_EXPRESSION = "expression";

    static {
        try {
            initialize();
            loadProps();
        } catch (Throwable ex) {
            LOGGER.warn("[ExecutorsProperties] 解析失败", ex);
        }
    }

    private static void loadProps() throws IOException {
        Properties properties = new Properties();
        properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE_NAME));
        String selector = properties.getProperty(SELECTOR);
        if (Strings.isNotBlank(selector)) {
            props.put(SELECTOR, selector);
        }
        String metricsEnable = properties.getProperty(METRICS_ENABLE);
        if (Strings.isNotBlank(metricsEnable)) {
            props.put(METRICS_ENABLE, metricsEnable);
        }
        Pattern pattern = Pattern.compile("[^0-9]");
        Set<String> counts = new HashSet<>();
        for (Object object : properties.keySet()) {
            String key = (String) object;
            if (key.startsWith(EXECUTOR)) {
                String numberKey =  pattern.matcher(key).replaceAll("");
                if (Strings.isNotBlank(numberKey)) {
                    counts.add(numberKey);
                }
            }
        }

        if (counts.size() > 0) {
            for (String index : counts) {
                ExecutorProperty property = new ExecutorProperty();
                property.addProperty(POOL_NAME, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + POOL_NAME));
                property.addProperty(CORE_POOL_SIZE, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + CORE_POOL_SIZE));
                property.addProperty(MAXIMUM_POOL_SIZE, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + MAXIMUM_POOL_SIZE));
                property.addProperty(KEEP_ALIVE_TIME, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + KEEP_ALIVE_TIME));
                property.addProperty(QUEUE_CAPACITY, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + QUEUE_CAPACITY));
                property.addProperty(WORK_QUEUE_TYPE, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + WORK_QUEUE_TYPE));
                property.addProperty(REJECTED_HANDLER_TYPE, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + REJECTED_HANDLER_TYPE));
                property.addProperty(SELECTOR_EXPRESSION, properties.getProperty(EXECUTOR + "[" + index + "]" + "." + SELECTOR_EXPRESSION));

                EXECUTOR_PROPERTY_LIST.add(Integer.parseInt(index), property);
            }
        }
    }

    private static void initialize() {
        props.put(SELECTOR, DEFAULT_SELECTOR);
        props.put(METRICS_ENABLE, DEFAULT_METRICS_ENABLE);
        ExecutorProperty property = new ExecutorProperty();
        EXECUTOR_PROPERTY_LIST.add(0, property);
    }

    public String getSelector() {
        return props.get(SELECTOR);
    }

    public static List<ExecutorProperty> getExecutorPropertyList() {
        return EXECUTOR_PROPERTY_LIST;
    }

    public static ExecutorProperty getExecutorProperty(String poolName) {
        int index = EXECUTOR_PROPERTY_LIST.indexOf(ExecutorProperty.named(poolName));
        return index != -1 ? EXECUTOR_PROPERTY_LIST.get(index) : null;
    }

    public boolean isMetricsEnable() {
        return Boolean.parseBoolean(props.get(METRICS_ENABLE));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ExecutorsProperty[");
        builder.append("selector=").append(getSelector()).append(", ")
                .append("metricsEnable").append(isMetricsEnable()).append(", ");
        for (ExecutorProperty property : EXECUTOR_PROPERTY_LIST) {
            System.out.println(1111);
            builder.append(property.toString());
        }
        builder.append("]");
        return builder.toString();
    }

}
