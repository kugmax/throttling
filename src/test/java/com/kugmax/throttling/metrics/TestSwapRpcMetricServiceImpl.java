package com.kugmax.throttling.metrics;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

public class TestSwapRpcMetricServiceImpl {

    SwapRpcMetricServiceImpl service;
    ScheduledThreadPoolExecutor schedulled;

    @Before
    public void before() {
        schedulled = spy(new ScheduledThreadPoolExecutor(1));

        service = new SwapRpcMetricServiceImpl(1, schedulled);
    }

    @Test
    public void oneThreadInWindow() {
        assertEquals(1, service.computeAngGetRpcByUser("user1"));
        assertEquals(2, service.computeAngGetRpcByUser("user1"));
        assertEquals(1, service.computeAngGetRpcByUser("user2"));
        assertEquals(3, service.computeAngGetRpcByUser("user1"));
        assertEquals(2, service.computeAngGetRpcByUser("user2"));
    }

    @Test
    public void oneThreadOutWindow() throws Exception {
        assertEquals(1, service.computeAngGetRpcByUser("user1"));
        assertEquals(2, service.computeAngGetRpcByUser("user1"));

        assertEquals(1, service.computeAngGetRpcByUser("user2"));
        assertEquals(2, service.computeAngGetRpcByUser("user2"));

        Thread.sleep(1_500);

        assertEquals(1, service.computeAngGetRpcByUser("user1"));
        assertEquals(2, service.computeAngGetRpcByUser("user1"));

        assertEquals(1, service.computeAngGetRpcByUser("user2"));
        assertEquals(2, service.computeAngGetRpcByUser("user2"));
    }

    @Test
    public void manyThreadsInWindow() throws Exception {
        service = new SwapRpcMetricServiceImpl(60, schedulled);

        UserTask task_1 = new UserTask(500, "user1", service);
        UserTask task_2 = new UserTask(500, "user1", service);

        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futures = threadPool.invokeAll(Arrays.asList(task_1, task_2));
        Thread.sleep(1_000);

        assertEquals(1_001, service.computeAngGetRpcByUser("user1"));
        assertEquals(1, service.computeAngGetRpcByUser("user2"));
    }

    private class UserTask implements Callable<Integer> {
        private final String user;
        private final RpcMetricService service;
        private final int maxCalls;

        public UserTask(int maxCalls, String user, RpcMetricService service) {
            this.maxCalls = maxCalls;
            this.user = user;
            this.service = service;
        }

        @Override
        public Integer call() throws Exception {
            Integer last = 0;
            for (int i = 0; i < maxCalls; i++) {
                last = service.computeAngGetRpcByUser(user);
            }

            return last;
        }
    }
}
