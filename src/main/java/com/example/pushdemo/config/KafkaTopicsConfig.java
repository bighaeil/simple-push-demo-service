package com.example.pushdemo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 부팅 시 필요한 Kafka 토픽을 {@link NewTopic} 빈으로 선언한다.
 *
 * <p>broker 쪽 auto-create가 켜져 있어도 명시적으로 선언해 파티션/리플리카 수를 통제한다.
 * <ul>
 *   <li>{@code push.bulk}만 의도적으로 1 파티션 — BULK 워커가 직렬로 처리되도록 강제하기 위해.
 *       (Phase 5 우선순위 격리 시연의 토대)</li>
 *   <li>나머지는 3 파티션 — HIGH/NORMAL 풀의 concurrency를 살리기 위해.</li>
 * </ul>
 *
 * <p>retry-0..3 토픽과 *.dlt 토픽은 {@code @RetryableTopic}이 런타임에 자동 생성하므로
 * 여기서 선언하지 않는다.
 */
@Configuration
public class KafkaTopicsConfig {

    @Value("${push-demo.topics.notification-requested}")
    private String notificationRequested;

    @Value("${push-demo.topics.push-critical}")
    private String pushCritical;

    @Value("${push-demo.topics.push-high}")
    private String pushHigh;

    @Value("${push-demo.topics.push-normal}")
    private String pushNormal;

    @Value("${push-demo.topics.push-bulk}")
    private String pushBulk;

    @Bean
    NewTopic notificationRequestedTopic() {
        return TopicBuilder.name(notificationRequested).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic pushCriticalTopic() {
        return TopicBuilder.name(pushCritical).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic pushHighTopic() {
        return TopicBuilder.name(pushHigh).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic pushNormalTopic() {
        return TopicBuilder.name(pushNormal).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic pushBulkTopic() {
        return TopicBuilder.name(pushBulk).partitions(1).replicas(1).build();
    }
}
