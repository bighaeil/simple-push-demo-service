package com.example.pushdemo.api;

public record NotificationResponse(String status, String idempotencyKey, String message) {
    public static NotificationResponse accepted(String key) {
        return new NotificationResponse("accepted", key, "queued");
    }
    public static NotificationResponse duplicate(String key) {
        return new NotificationResponse("duplicate", key, "already processed");
    }
}
