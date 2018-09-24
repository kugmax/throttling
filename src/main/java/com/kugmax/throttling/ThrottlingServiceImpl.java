package com.kugmax.throttling;

import com.kugmax.throttling.metrics.RpcMetricService;

import java.util.Optional;
import java.util.concurrent.*;

public class ThrottlingServiceImpl implements ThrottlingService {
    private final int guestRPS;
    private final String guestToken = "";

    private final SlaService slaService;
    private final RpcMetricService rpcMetricService;

    private final ConcurrentHashMap<String, SlaService.SLA> cache = new ConcurrentHashMap<>();

    public ThrottlingServiceImpl(int guestRPS, SlaService slaService, RpcMetricService rpcMetricService) {
        this.guestRPS = guestRPS;
        this.slaService = slaService;
        this.rpcMetricService = rpcMetricService;
        cache.put(guestToken, new SlaService.SLA(guestToken, guestRPS));
    }

    @Override
    public boolean isRequestAllowed(Optional<String> token) {
        SlaService.SLA sla = getSLA(token);

        return sla.getRps() > rpcMetricService.computeAngGetRpcByUser(sla.getUser());
    }

    private SlaService.SLA getSLA(Optional<String> token) {

        if (!token.isPresent()) {
            return cache.get(guestToken);
        }

        final String tokenVal = token.get();

        return cache.computeIfAbsent(tokenVal,
                (key) -> {
                    slaService.getSlaByToken(tokenVal).whenCompleteAsync( (sla, ex) -> {cache.put(tokenVal, sla);} );
                    return new SlaService.SLA(tokenVal, guestRPS);
        });
    }
}
