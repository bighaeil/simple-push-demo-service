package com.example.pushdemo.api;

import com.example.pushdemo.common.NotificationRequestedMessage;
import com.example.pushdemo.idempotency.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * API 레이어의 비즈니스 로직: 멱등 체크 → {@code notification.requested} 토픽 발행.
 *
 * <p>멱등 키가 이미 예약되어 있으면 발행을 건너뛰고 즉시 duplicate 응답을 반환한다.
 * Outbox 패턴을 생략한 데모이므로 "예약 성공 직후 → 발행 실패" 사이의 좁은 창은 받아들이는 트레이드오프.
 * 컨슈머 쪽이 idempotent하다는 전제로 at-least-once 발송을 보장한다.
 */
@Service
public class NotificationApiService {

    private static final Logger log = LoggerFactory.getLogger(NotificationApiService.class);

    private final IdempotencyStore idempotencyStore;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public NotificationApiService(IdempotencyStore idempotencyStore,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  @Value("${push-demo.topics.notification-requested}") String topic) {
        this.idempotencyStore = idempotencyStore;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public NotificationResponse submit(NotificationRequest req) {
        boolean reserved = idempotencyStore.reserve(req.idempotencyKey());
        if (!reserved) {
            log.info("[API] duplicate idempotencyKey={} → SKIP", req.idempotencyKey());
            return NotificationResponse.duplicate(req.idempotencyKey());
        }

        NotificationRequestedMessage msg = new NotificationRequestedMessage(
                req.userId(), req.type(), req.priority(), req.data(), req.idempotencyKey());

        kafkaTemplate.send(topic, String.valueOf(req.userId()), msg);
        log.info("[API] published userId={} type={} priority={} key={}",
                req.userId(), req.type(), req.priority(), req.idempotencyKey());
        return NotificationResponse.accepted(req.idempotencyKey());
    }
}
