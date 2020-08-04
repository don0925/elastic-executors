package com.don.elastic.executors.excutor;

import com.don.elastic.executors.queue.BlockingQueueBuilder;
import com.don.elastic.executors.queue.ResizableLinkedBlockingQueue;
import com.don.elastic.executors.task.DefaultExecutorTask;
import com.don.elastic.executors.task.ExecutorTaskContext;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultElasticThreadPoolExecutorTest {

    @Test
    public void test() throws Exception {
        ElasticExecutor executor = DefaultElasticThreadPoolExecutor.newBuilder()
                .poolName("test-pool")
                .corePoolSize(3)
                .maximumPoolSize(5)
                .keepAliveTime(10000)
                .workQueue(new BlockingQueueBuilder<Runnable>()
                        .capacity(10)
                        .type(ResizableLinkedBlockingQueue.class.getSimpleName())
                        .fair(false)
                        .build())
                .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy())
                .build();
        printExecutorStatus(executor, "修改之前: ");
        Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    printExecutorStatus(executor, "创建任务: ");
                    TimeUnit.MILLISECONDS.sleep(1);
                    executor.setCorePoolSize(12);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

        };
        Runnable submitTask = new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    try {
                        executor.submit(new DefaultExecutorTask<>(new ExecutorTaskContext(i + "", new HashMap<>()),task));
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(submitTask).start();
        new Thread(submitTask).start();
        new Thread(submitTask).start();

        TimeUnit.SECONDS.sleep(10);

        printExecutorStatus(executor, "结束: ");

        executor.shutdown(5, TimeUnit.SECONDS);

    }

    private static void printExecutorStatus(ElasticExecutor executor, String prefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix)
                .append("poolName: ").append(executor.getPoolName()).append(" ")
                .append("coreSize: ").append(executor.getCorePoolSize()).append(" ")
                .append("maximumPoolSize: ").append(executor.getMaximumPoolSize()).append(" ")
                .append("activeCount: ").append(executor.getActiveCount()).append(" ")
                .append("poolSize: ").append(executor.getPoolSize()).append(" ")
                .append("largestPoolSize: ").append(executor.getLargestPoolSize()).append(" ")
                .append("queueType: ").append(executor.getWorkQueueType()).append(" ")
                .append("queueCapacity: ").append(executor.getWorkQueueCapacity()).append(" ")
                .append("queueSize: ").append(executor.getWorkQueueSize()).append(" ")
                .append("queueRemainingCapacity: ").append(executor.getWorkQueueCapacity()).append(" ")
                .append("completedTaskCount: ").append(executor.getCompletedTaskCount()).append(" ")
                .append("rejectCount: ").append(executor.getRejectedTaskCount());

        System.out.println(sb.toString());
    }
}