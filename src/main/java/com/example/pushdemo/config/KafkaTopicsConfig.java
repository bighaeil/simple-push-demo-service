package com.example.pushdemo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
