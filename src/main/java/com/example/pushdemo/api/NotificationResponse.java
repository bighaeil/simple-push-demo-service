package com.example.pushdemo.api;

/**
 * API 응답 바디. status는 {@code "accepted"}(첫 발행) 또는 {@code "duplicate"}(멱등 차단).
 */
public record NotificationResponse(String status, String idempotencyKey, String message) {
    public static NotificationResponse accepted(String key) {
        return new NotificationResponse("accepted", key, "queued");
    }
    public static NotificationResponse duplicate(String key) {
        return new NotificationResponse("duplicate", key, "already processed");
    }
}
