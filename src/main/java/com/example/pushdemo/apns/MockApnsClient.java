package com.example.pushdemo.apns;

import com.example.pushdemo.common.PushMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * APNs를 흉내내는 가짜 클라이언트. 실제 외부 의존을 격리하기 위한 데모용 어댑터.
 *
 * <p>분포는 {@code application.yml}로 조절(success-rate / retryable-rate). {@code invalid-}로 시작하는
 * 토큰은 분포와 무관하게 항상 INVALID_TOKEN을 돌려준다 — 시나리오 5(무효 토큰 비활성화)를 결정론적으로 재현.
 *
 * <p>{@code min/max-latency-ms}로 네트워크 지연을 모방한다. 0으로 두면 BULK 처리가 사실상 동시 완료되어
 * Phase 5 우선순위 격리 시연 효과가 보이지 않으므로, 기본값 80~250ms로 약간의 지연을 준다.
 */
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
