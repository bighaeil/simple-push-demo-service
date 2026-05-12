package com.example.pushdemo.common;

/**
 * 우선순위 토픽({@code push.critical/high/normal/bulk})의 메시지 바디.
 *
 * <p>Orchestrator → Worker 형식. 이 시점이면 디바이스 토큰 / 렌더된 제목·본문 /
 * 우선순위가 모두 확정되어 있어서 Worker는 그대로 APNs로 던지기만 하면 된다.
 */
public record PushMessage(
        Long userId,
        String deviceToken,
        Priority priority,
        NotificationType type,
        String title,
        String body,
        String idempotencyKey
) {}
