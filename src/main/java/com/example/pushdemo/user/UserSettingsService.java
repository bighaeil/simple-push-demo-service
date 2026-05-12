package com.example.pushdemo.user;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 시드 5명. README 검증 시나리오용으로 의도된 프로필을 들고 있다.
 *
 * <ul>
 *   <li>1000 — 정상 (DND 없음)</li>
 *   <li>1001 — 정상 + 야간 DND 22:00~07:00</li>
 *   <li>1002 — 푸시 OFF (시나리오 3/4)</li>
 *   <li>1003 — 푸시 ON, 토큰만 invalid (시나리오 5)</li>
 *   <li>1004 — 푸시 ON + DND 23:00~06:00, 디바이스 2대</li>
 * </ul>
 *
 * <p>실서비스라면 PostgreSQL + Redis 캐시. 이 데모의 관심사는 메시징 흐름이므로 영속성은 뺐다.
 */
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
