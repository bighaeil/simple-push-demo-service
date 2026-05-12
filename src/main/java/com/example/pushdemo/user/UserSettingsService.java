package com.example.pushdemo.user;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSettingsService {

    private final Map<Long, UserSettings> store = new ConcurrentHashMap<>();

    @PostConstruct
    void seed() {
        // 1000: normal user, no DND
        store.put(1000L, new UserSettings(1000L, true, null, null, "ko"));
        // 1001: normal user with overnight DND 22:00 ~ 07:00
        store.put(1001L, new UserSettings(1001L, true, LocalTime.of(22, 0), LocalTime.of(7, 0), "ko"));
        // 1002: push OFF
        store.put(1002L, new UserSettings(1002L, false, null, null, "ko"));
        // 1003: push ON (token is intentionally invalid — handled in DeviceTokenService)
        store.put(1003L, new UserSettings(1003L, true, null, null, "ko"));
        // 1004: push ON, DND 23:00 ~ 06:00 (for DND test window)
        store.put(1004L, new UserSettings(1004L, true, LocalTime.of(23, 0), LocalTime.of(6, 0), "en"));
    }

    public Optional<UserSettings> find(Long userId) {
        return Optional.ofNullable(store.get(userId));
    }
}
