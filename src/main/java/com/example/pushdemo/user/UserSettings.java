package com.example.pushdemo.user;

import java.time.LocalTime;

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
