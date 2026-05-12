package com.example.pushdemo.idempotency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

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
