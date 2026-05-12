package com.example.pushdemo.api;

import com.example.pushdemo.common.NotificationType;
import com.example.pushdemo.common.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 외부 API의 요청 바디.
 *
 * <p>{@code idempotencyKey}는 클라이언트가 채워서 보내야 한다 — 동일 키로 재호출이 와도
 * 24시간 안에는 한 번만 실제 발송된다(서버 측 안전망).
 */
public record NotificationRequest(
        @NotNull Long userId,
        @NotNull NotificationType type,
        @NotNull Priority priority,
        Map<String, String> data,
        @NotBlank String idempotencyKey
) {}
