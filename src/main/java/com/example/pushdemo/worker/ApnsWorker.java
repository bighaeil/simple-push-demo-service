package com.example.pushdemo.worker;

import com.example.pushdemo.apns.ApnsResult;
import com.example.pushdemo.apns.MockApnsClient;
import com.example.pushdemo.apns.RetryableApnsException;
import com.example.pushdemo.common.PushMessage;
import com.example.pushdemo.user.DeviceTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class ApnsWorker {

    private static final Logger log = LoggerFactory.getLogger(ApnsWorker.class);

    private final MockApnsClient apns;
    private final DeviceTokenService deviceTokens;

    public ApnsWorker(MockApnsClient apns, DeviceTokenService deviceTokens) {
        this.apns = apns;
        this.deviceTokens = deviceTokens;
    }

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 8000, random = true),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".dlt",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            include = { RetryableApnsException.class }
    )
    @KafkaListener(
            topics = { "${push-demo.topics.push-critical}", "${push-demo.topics.push-high}" },
            groupId = "apns-high-pool",
            concurrency = "3",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onHigh(PushMessage msg) {
        process("HIGH 풀", msg);
    }

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 8000, random = true),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".dlt",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            include = { RetryableApnsException.class }
    )
    @KafkaListener(
            topics = "${push-demo.topics.push-normal}",
            groupId = "apns-normal-pool",
            concurrency = "2",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onNormal(PushMessage msg) {
        process("NORMAL 풀", msg);
    }

    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 500, multiplier = 2.0, maxDelay = 8000, random = true),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = ".dlt",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            include = { RetryableApnsException.class }
    )
    @KafkaListener(
            topics = "${push-demo.topics.push-bulk}",
            groupId = "apns-bulk-pool",
            concurrency = "1",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBulk(PushMessage msg) {
        process("BULK 풀", msg);
    }

    private void process(String pool, PushMessage msg) {
        log.info("[{}] APNs 호출 userId={} token={} type={}", pool, msg.userId(), msg.deviceToken(), msg.type());
        ApnsResult result = apns.send(msg);
        switch (result) {
            case SUCCESS -> log.info("[{}] ✓ 도달 userId={} token={}", pool, msg.userId(), msg.deviceToken());
            case INVALID_TOKEN -> {
                log.warn("[{}] ✗ 무효 토큰 userId={} token={} → 비활성화", pool, msg.userId(), msg.deviceToken());
                deviceTokens.deactivate(msg.userId(), msg.deviceToken());
                // 종료 — 재시도 X
            }
            case RETRYABLE_FAILURE -> {
                log.warn("[{}] ⟳ 재시도 가능 실패 userId={} token={}", pool, msg.userId(), msg.deviceToken());
                throw new RetryableApnsException("transient APNs failure");
            }
        }
    }

    @DltHandler
    public void onDlt(PushMessage msg) {
        log.error("[DLQ] 최종 실패 userId={} token={} type={} title={}",
                msg.userId(), msg.deviceToken(), msg.type(), msg.title());
        // 운영자 알림 + 분석 저장 모킹 지점
    }
}
