package com.don.elastic.executors;


import com.don.elastic.executors.excutor.DefaultElasticThreadPoolExecutor;
import com.don.elastic.executors.excutor.ElasticExecutor;
import com.don.elastic.executors.queue.BlockingQueueBuilder;
import com.don.elastic.executors.queue.ResizableLinkedBlockingQueue;
import com.don.elastic.executors.task.DefaultExecutorTask;
import com.don.elastic.executors.task.ExecutorTaskContext;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class DefaultElasticExecutorsTest {

    @Test
    public void test() throws Exception {
        ElasticExecutors executors = new DefaultElasticExecutors();
        Callable<Void> task = new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    System.out.println("12132131231");
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
                        executors.submit(new DefaultExecutorTask<>(new ExecutorTaskContext(i + "", new HashMap<>()),task));
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

        executors.shutdown(5, TimeUnit.SECONDS);

    }
}