package com.kugmax.throttling;

import com.kugmax.throttling.metrics.RpcMetricService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;

public class TestThrottlingServiceImpl {
    private ThrottlingServiceImpl service;

    @Mock SlaService mockSlaService;
    @Mock RpcMetricService mockRpcMetricService;

    int guestRPS;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        guestRPS = 20;
        service = new ThrottlingServiceImpl(guestRPS, mockSlaService, mockRpcMetricService);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(mockSlaService, mockRpcMetricService);
    }

    @Test
    public void whenNoTokenUseGuest_allow() {
        when(mockRpcMetricService.computeAngGetRpcByUser("")).thenReturn(5);

        assertTrue(service.isRequestAllowed(Optional.empty()));

        verify(mockRpcMetricService).computeAngGetRpcByUser("");
    }

    @Test
    public void whenNoTokenUseGuest_reject() {
        when(mockRpcMetricService.computeAngGetRpcByUser("")).thenReturn(20);

        assertFalse(service.isRequestAllowed(Optional.empty()));

        verify(mockRpcMetricService).computeAngGetRpcByUser("");
    }

    @Test
    public void whenNewUserUseGuest_allow() {
        CompletableFuture<SlaService.SLA> future = CompletableFuture.completedFuture(new SlaService.SLA("user1", 55)).toCompletableFuture();

        when(mockRpcMetricService.computeAngGetRpcByUser("user1")).thenReturn(10);
        when(mockSlaService.getSlaByToken("token1")).thenReturn(future);

        assertTrue(service.isRequestAllowed(Optional.of("token1")));

        verify(mockRpcMetricService).computeAngGetRpcByUser("token1");
        verify(mockSlaService).getSlaByToken("token1");
    }

    @Test
    public void whenNewUserUseGuest_rejected() {
        CompletableFuture<SlaService.SLA> future = CompletableFuture.supplyAsync(
                () -> {
                    try {Thread.sleep(2_000);} catch (Exception e){e.printStackTrace();}
                    return new SlaService.SLA("user1", 55);
                }
        );

        when(mockRpcMetricService.computeAngGetRpcByUser("token1")).thenReturn(21);
        when(mockSlaService.getSlaByToken("token1")).thenReturn(future);

        assertFalse(service.isRequestAllowed(Optional.of("token1")));

        verify(mockRpcMetricService).computeAngGetRpcByUser("token1");
        verify(mockSlaService).getSlaByToken("token1");
    }

    @Test
    public void whenCallSlaServiceOnlyOnce_allow() throws Exception {
        CompletableFuture<SlaService.SLA> future = CompletableFuture.supplyAsync(
                () -> {
                    try {Thread.sleep(500);} catch (Exception e){e.printStackTrace();}
                    return new SlaService.SLA("user1", 2);
                }
        );

        when(mockRpcMetricService.computeAngGetRpcByUser("token1")).thenReturn(1).thenReturn(2).thenReturn(3);
        when(mockRpcMetricService.computeAngGetRpcByUser("user1")).thenReturn(3);
        when(mockSlaService.getSlaByToken("token1")).thenReturn(future);

        assertTrue(service.isRequestAllowed(Optional.of("token1")));
        assertTrue(service.isRequestAllowed(Optional.of("token1")));

        Thread.sleep(1000);
        assertFalse(service.isRequestAllowed(Optional.of("token1")));

        verify(mockRpcMetricService, times(2)).computeAngGetRpcByUser("token1");
        verify(mockRpcMetricService, times(1)).computeAngGetRpcByUser("user1");
        verify(mockSlaService, times(1)).getSlaByToken("token1");
    }
}
