package com.kugmax.throttling.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SwapRpcMetricServiceImpl implements RpcMetricService {
    private final ConcurrentHashMap<String, Integer> rpcA = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> rpcB = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Integer> currentRPC = rpcA;
    private volatile boolean isCurrentA = true;

    private ScheduledThreadPoolExecutor scheduled;

    public SwapRpcMetricServiceImpl(int windowInSeconds, ScheduledThreadPoolExecutor scheduled) {
        this.scheduled = scheduled;
        this.scheduled.scheduleWithFixedDelay( () -> {
                    boolean localVal = isCurrentA;

                    ConcurrentHashMap<String, Integer> previous = currentRPC;
                    currentRPC = localVal ? rpcB :  rpcA;

                    previous.clear();
                    isCurrentA = !localVal;
                },
                windowInSeconds, windowInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public int computeAngGetRpcByUser(String user) {
        return currentRPC.compute(user, (key, rpc) -> rpc == null ? 1 : rpc + 1);
    }
}
