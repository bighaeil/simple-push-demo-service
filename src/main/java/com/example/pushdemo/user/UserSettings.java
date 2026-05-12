package com.example.pushdemo.user;

import java.time.LocalTime;

/**
 * 사용자 알림 설정. 푸시 ON/OFF + DND(방해금지) 시간대 + 언어.
 *
 * <p>{@link #inDnd(LocalTime)}는 자정을 넘어가는 윈도우(예: 22:00~07:00)도 올바르게 판단한다 —
 * 단순히 {@code start <= now < end}만 보면 야간 DND가 깨진다.
 */
public record UserSettings(
        Long userId,
        boolean pushEnabled,
        LocalTime dndStart,
        LocalTime dndEnd,
        String language
) {
    public boolean inDnd(LocalTime now) {
        if (dndStart == null || dndEnd == null) return false;
        if (dndStart.equals(dndEnd)) return false;
        if (dndStart.isBefore(dndEnd)) {
            return !now.isBefore(dndStart) && now.isBefore(dndEnd);
        }
        // overnight window (e.g., 22:00 ~ 07:00)
        return !now.isBefore(dndStart) || now.isBefore(dndEnd);
    }
}
