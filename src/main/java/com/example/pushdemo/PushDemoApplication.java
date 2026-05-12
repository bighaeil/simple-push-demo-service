package com.example.pushdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 애플리케이션 진입점.
 *
 * <p>{@code @EnableKafka}로 {@code @KafkaListener} / {@code @RetryableTopic} 인프라를 활성화한다.
 * 액추에이터 헬스 엔드포인트는 {@code /actuator/health} (docker-compose healthcheck가 사용).
 */
@SpringBootApplication
@EnableKafka
public class PushDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(PushDemoApplication.class, args);
    }
}
