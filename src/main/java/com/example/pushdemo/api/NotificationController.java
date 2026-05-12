package com.example.pushdemo.api;

import com.example.pushdemo.common.NotificationType;
import com.example.pushdemo.common.Priority;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 외부 진입점. 단일 알림 발송과 벌크 발송(Phase 5 격리 시연용)을 노출한다.
 *
 * <p>- {@code POST /api/notifications} — 단건. 첫 호출이면 202 Accepted, 멱등 차단이면 200 OK.<br>
 * - {@code POST /api/notifications/bulk?count=N&priority=LOW} — N건을 같은 사용자에게 일괄 발송.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationApiService service;

    public NotificationController(NotificationApiService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse resp = service.submit(request);
        HttpStatus status = "duplicate".equals(resp.status()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(resp);
    }

    /** Phase 5: 벌크 발송 — count 건을 priority로 발행. */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulk(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "LOW") Priority priority,
            @RequestParam(defaultValue = "1000") Long userId) {

        int published = 0;
        for (int i = 0; i < count; i++) {
            NotificationRequest req = new NotificationRequest(
                    userId,
                    NotificationType.MARKETING_PROMO,
                    priority,
                    Map.of("idx", String.valueOf(i)),
                    "bulk-" + UUID.randomUUID()
            );
            service.submit(req);
            published++;
        }
        return ResponseEntity.accepted().body(Map.of(
                "count", count,
                "priority", priority,
                "published", published
        ));
    }
}
