package com.example.pushdemo.api;

import com.example.pushdemo.common.NotificationRequestedMessage;
import com.example.pushdemo.idempotency.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
