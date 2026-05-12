package com.example.pushdemo.api;

import com.example.pushdemo.common.NotificationType;
import com.example.pushdemo.common.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record NotificationRequest(
        @NotNull Long userId,
        @NotNull NotificationType type,
        @NotNull Priority priority,
        Map<String, String> data,
        @NotBlank String idempotencyKey
) {}
