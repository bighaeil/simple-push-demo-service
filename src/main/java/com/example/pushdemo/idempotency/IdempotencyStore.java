package com.example.pushdemo.idempotency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis {@code SETNX} + TTL로 멱등 키를 예약한다.
 *
 * <p>{@link #reserve(String)}가 true면 첫 호출, false면 이미 처리된 키.
 * 데모 본체에서 Outbox 패턴을 뺀 대신 이 상자가 클라이언트 재시도(네트워크 글리치, 타임아웃 후 재요청 등)에서
 * 중복 발송을 막는 안전망 역할을 한다.
 */
@Component
public class IdempotencyStore {

    private static final String PREFIX = "idem:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public IdempotencyStore(StringRedisTemplate redis,
                            @Value("${push-demo.idempotency.ttl-hours:24}") long ttlHours) {
        this.redis = redis;
        this.ttl = Duration.ofHours(ttlHours);
    }

    /** @return true when the key was reserved (first-time), false when already present. */
    public boolean reserve(String idempotencyKey) {
        Boolean ok = redis.opsForValue().setIfAbsent(PREFIX + idempotencyKey, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }
}
