package com.example.pushdemo.user;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
