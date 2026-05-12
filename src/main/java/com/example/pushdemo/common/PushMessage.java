package com.example.pushdemo.common;

public record PushMessage(
        Long userId,
        String deviceToken,
        Priority priority,
        NotificationType type,
        String title,
        String body,
        String idempotencyKey
) {}
