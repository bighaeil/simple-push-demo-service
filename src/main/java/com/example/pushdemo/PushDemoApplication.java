package com.example.pushdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PushDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(PushDemoApplication.class, args);
    }
}
