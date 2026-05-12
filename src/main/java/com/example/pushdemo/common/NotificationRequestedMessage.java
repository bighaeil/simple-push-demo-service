package com.example.pushdemo.common;

import java.util.Map;

/**
 * {@code notification.requested} 토픽의 메시지 바디.
 *
 * <p>API → Orchestrator 사이의 1차 형식. 이 시점에서는 아직 디바이스 토큰도 모르고
 * 본문도 렌더링되지 않은 상태(요청 원형).
 */
public record NotificationRequestedMessage(
        Long userId,
        NotificationType type,
        Priority priority,
        Map<String, String> data,
        String idempotencyKey
) {}
