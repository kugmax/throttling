package com.kugmax.throttling;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public class SlaServiceSimpleImpl implements SlaService {
    private final ConcurrentMap<String, SLA> tokenSLA;

    public SlaServiceSimpleImpl(ConcurrentMap<String, SLA> tokenSLA) {
        this.tokenSLA = tokenSLA;
    }

    @Override
    public CompletableFuture<SLA> getSlaByToken(String token) {
//        shod be ~200 - 300 ml response
        return CompletableFuture.completedFuture(
                tokenSLA.get(token)
        );
    }
}
