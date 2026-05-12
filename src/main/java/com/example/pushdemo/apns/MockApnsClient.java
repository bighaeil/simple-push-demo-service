package com.example.pushdemo.apns;

import com.example.pushdemo.common.PushMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class MockApnsClient {

    private final double successRate;
    private final double retryableRate;
    private final long minLatencyMs;
    private final long maxLatencyMs;

    public MockApnsClient(@Value("${push-demo.mock-apns.success-rate:0.75}") double successRate,
                         @Value("${push-demo.mock-apns.retryable-rate:0.20}") double retryableRate,
                         @Value("${push-demo.mock-apns.min-latency-ms:80}") long minLatencyMs,
                         @Value("${push-demo.mock-apns.max-latency-ms:250}") long maxLatencyMs) {
        this.successRate = successRate;
        this.retryableRate = retryableRate;
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
    }

    public ApnsResult send(PushMessage msg) {
        simulateNetwork();
        if (msg.deviceToken() != null && msg.deviceToken().startsWith("invalid-")) {
            return ApnsResult.INVALID_TOKEN;
        }
        double r = ThreadLocalRandom.current().nextDouble();
        if (r < successRate) return ApnsResult.SUCCESS;
        if (r < successRate + retryableRate) return ApnsResult.RETRYABLE_FAILURE;
        return ApnsResult.INVALID_TOKEN;
    }

    private void simulateNetwork() {
        long lo = Math.max(0, minLatencyMs);
        long hi = Math.max(lo + 1, maxLatencyMs);
        long delay = ThreadLocalRandom.current().nextLong(lo, hi);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
