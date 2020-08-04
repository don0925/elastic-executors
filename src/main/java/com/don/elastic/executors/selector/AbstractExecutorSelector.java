package com.don.elastic.executors.selector;

import com.don.elastic.executors.config.ExecutorProperty;
import com.don.elastic.executors.config.ExecutorsProperty;
import com.don.elastic.executors.excutor.ElasticExecutor;
import com.don.elastic.executors.task.ExecutorTaskContext;
import com.don.elastic.executors.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 抽象选择器，支持复杂的逻辑表达式
 * @author Don Du
 */
public abstract class AbstractExecutorSelector implements ExecutorSelector {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 选择器表达式
     */
    private ExecutorExpression expression;

    /**
     * 表达式引擎
     */
    private class ExecutorExpression {

        private ExecutorExpressionFunction function;

        private final ConcurrentMap<String, String> cachedInputs =new ConcurrentHashMap<>();

        private ExecutorExpression(ExecutorExpressionFunction function) {
            this.function = function;
        }

        private String match(String input) {
            if (Strings.isBlank(input)) {
                if (logger.isInfoEnabled()) {
                    logger.info("[ExecutorSelector] 表达式: {}, 匹配结果: {}", input, ElasticExecutor.DEFAULT_POOL_NAME);
                }
                return ElasticExecutor.DEFAULT_POOL_NAME;
            }

            String poolName = cachedInputs.get(input);
            if (Strings.isNotBlank(poolName)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[ExecutorSelector] 表达式: {}, 匹配结果: {}", input, poolName);
                }
                return poolName;
            }

            if (function == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("选择器计算引擎function为空， 直接返回选择器的结果值: {}", input);
                }
                return input;
            }

            List<ExecutorProperty> propertyList = ExecutorsProperty.getExecutorPropertyList();
            for (ExecutorProperty property : propertyList) {
                poolName = property.getPoolName();
                String expression = property.getExpression();
                if (Strings.isNotBlank(expression)) {
                    if (function.matches(input, expression)) {
                        cachedInputs.putIfAbsent(input, poolName);
                        if (logger.isDebugEnabled()) {
                            logger.debug("选择器计算引擎匹配到线程池{}, 使用此线程池", poolName);
                        }
                        return poolName;
                    }
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("选择器计算引擎没有匹配线程池，将使用默认的线程池: {}", ElasticExecutor.DEFAULT_POOL_NAME);
            }
            // 原样返回
            return input;
        }

    }

    protected interface ExecutorExpressionFunction {

        /**
         * 输入值是否能够匹配表达式
         * @param input
         * @param expression
         * @return
         */
        boolean matches(String input, String expression);

    }

    protected AbstractExecutorSelector() {
        expression = new ExecutorExpression(provideFunction());
    }

    @Override
    public String selectPoolKey(ExecutorTaskContext taskContext) {
        return expression.match(select(taskContext));
    }

    /**
     * 表达式计算引擎
     * @return
     */
    protected abstract ExecutorExpressionFunction provideFunction();

    /**
     * 真正的选择逻辑，交给子类去实现
     * @param taskContext
     * @return
     */
    protected abstract String select(ExecutorTaskContext taskContext);


}
