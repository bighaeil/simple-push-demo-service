package com.example.pushdemo.orchestrator;

import com.example.pushdemo.common.*;
import com.example.pushdemo.user.DeviceTokenService;
import com.example.pushdemo.user.UserSettings;
import com.example.pushdemo.user.UserSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    private final UserSettingsService userSettings;
    private final DeviceTokenService deviceTokens;
    private final TemplateRenderer templates;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationOrchestrator(UserSettingsService userSettings,
                                    DeviceTokenService deviceTokens,
                                    TemplateRenderer templates,
                                    KafkaTemplate<String, Object> kafkaTemplate) {
        this.userSettings = userSettings;
        this.deviceTokens = deviceTokens;
        this.templates = templates;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "${push-demo.topics.notification-requested}",
            groupId = "orchestrator",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onRequested(NotificationRequestedMessage msg) {
        log.info("[Orchestrator] received userId={} type={} priority={} key={}",
                msg.userId(), msg.type(), msg.priority(), msg.idempotencyKey());

        Optional<UserSettings> settingsOpt = userSettings.find(msg.userId());
        if (settingsOpt.isEmpty()) {
            log.warn("[Orchestrator] unknown userId={} → SKIP", msg.userId());
            return;
        }
        UserSettings settings = settingsOpt.get();

        boolean bypass = msg.priority().bypassUserSettings();

        if (!bypass && !settings.pushEnabled()) {
            log.info("[Orchestrator] userId={} 푸시 OFF → SKIP", msg.userId());
            return;
        }

        if (!bypass && settings.inDnd(LocalTime.now())) {
            log.info("[Orchestrator] userId={} DND 시간 → SKIP", msg.userId());
            return;
        }

        List<String> tokens = deviceTokens.active(msg.userId());
        if (tokens.isEmpty()) {
            log.info("[Orchestrator] userId={} 활성 디바이스 없음 → SKIP", msg.userId());
            return;
        }

        TemplateRenderer.Rendered rendered = templates.render(msg.type(), settings.language(), msg.data());
        String targetTopic = msg.priority().topic();

        for (String token : tokens) {
            PushMessage push = new PushMessage(
                    msg.userId(), token, msg.priority(), msg.type(),
                    rendered.title(), rendered.body(), msg.idempotencyKey());
            kafkaTemplate.send(targetTopic, String.valueOf(msg.userId()), push);
            log.info("[Orchestrator] → {} userId={} token={}", targetTopic, msg.userId(), token);
        }
    }
}
