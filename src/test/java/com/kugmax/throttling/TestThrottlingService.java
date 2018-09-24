package com.kugmax.throttling;

import com.kugmax.throttling.metrics.RpcMetricService;
import com.kugmax.throttling.metrics.SwapRpcMetricServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@Category(IntegrationTest.class)
public class TestThrottlingService {
    ThrottlingService service;
    SlaService spySlaService;
    RpcMetricService spyRpcMetricService;

    int guestRPS;
    ConcurrentMap<String, SlaService.SLA> tokenSLA;

    @Before
    public void before() {
        tokenSLA = new ConcurrentHashMap<>();
        tokenSLA.put("token1", new SlaService.SLA("user1", 5));
        tokenSLA.put("token2", new SlaService.SLA("user2", 30));
        guestRPS = 20;

        spySlaService = spy(new SlaServiceSimpleImpl(tokenSLA));
        spyRpcMetricService = spy(new SwapRpcMetricServiceImpl(1, new ScheduledThreadPoolExecutor(1)));
        service = new ThrottlingServiceImpl(guestRPS, spySlaService, spyRpcMetricService);
    }

    @Test
    public void inWindow() throws Exception {
        UserTask task_1 = new UserTask(10,"token1", service);
        UserTask task_2 = new UserTask(20,"token2", service);

        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> futures = threadPool.invokeAll(Arrays.asList(task_1, task_2));

        assertFalse(futures.get(0).get(100, TimeUnit.SECONDS));
        assertTrue(futures.get(1).get(100, TimeUnit.SECONDS));

        assertTrue(6 >= task_1.getAllowedCals());
        assertTrue(21 >= task_2.getAllowedCals());
    }

    @Test
    public void outWindow() throws Exception {
        inWindow();

        Thread.sleep(1_500);

        inWindow();

        Thread.sleep(1_500);

        inWindow();
    }

    private class UserTask implements Callable<Boolean> {
        private final String token;
        private final ThrottlingService service;
        private final int maxCals;
        private AtomicInteger allowedCals;

        public UserTask(int maxCals, String token, ThrottlingService service) {
            this.maxCals = maxCals;
            this.token = token;
            this.service = service;
            this.allowedCals = new AtomicInteger(0);
        }

        public int getAllowedCals() {
            return allowedCals.get();
        }

        @Override
        public Boolean call() throws Exception {
            while (true) {
                int currentNumCalls = allowedCals.getAndIncrement();
                boolean result = service.isRequestAllowed(Optional.of(token));
                if (!result) {
                    System.out.println("1 " + token + " " + currentNumCalls + " " + result);
                    return currentNumCalls == maxCals;
                }

                if (currentNumCalls == maxCals) {
                    System.out.println("2 " + token + " " + currentNumCalls + " " + result);
                    return result;
                }
            }
        }
    }
}
