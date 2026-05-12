package com.example.pushdemo.user;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자별 디바이스 토큰 보관.
 *
 * <p>APNs가 INVALID_TOKEN(BadDeviceToken/Unregistered)을 돌려주면 Worker가
 * {@link #deactivate(Long, String)}을 호출해 제거한다. 제거 후 같은 사용자에게 재발송 시
 * Orchestrator에서 "활성 디바이스 없음 → SKIP" 으로 끝난다(시나리오 6).
 *
 * <p>실서비스라면 DB. 여기서는 ConcurrentHashMap 인메모리.
 */
@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);

    private final Map<Long, Set<String>> tokens = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        tokens.put(1000L, new HashSet<>(Set.of("device-1000-a")));
        tokens.put(1001L, new HashSet<>(Set.of("device-1001-a")));
        tokens.put(1002L, new HashSet<>(Set.of("device-1002-a")));
        tokens.put(1003L, new HashSet<>(Set.of("invalid-1003-token")));
        tokens.put(1004L, new HashSet<>(Set.of("device-1004-a", "device-1004-b")));
    }

    public List<String> active(Long userId) {
        Set<String> s = tokens.get(userId);
        if (s == null) return List.of();
        return List.copyOf(s);
    }

    public void deactivate(Long userId, String token) {
        Set<String> s = tokens.get(userId);
        if (s != null && s.remove(token)) {
            log.warn("[DeviceToken] deactivated userId={} token={}", userId, token);
        }
    }
}
