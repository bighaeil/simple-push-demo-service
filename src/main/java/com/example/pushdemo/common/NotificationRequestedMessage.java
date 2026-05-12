package com.example.pushdemo.common;

import java.util.Map;

public record NotificationRequestedMessage(
        Long userId,
        NotificationType type,
        Priority priority,
        Map<String, String> data,
        String idempotencyKey
) {}
