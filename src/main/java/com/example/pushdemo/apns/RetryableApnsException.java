package com.example.pushdemo.apns;

/**
 * {@code @RetryableTopic(include = ...)}에 명시된 마커 예외.
 *
 * <p>이 예외가 던져진 경우에만 재시도 토픽으로 흘러간다. INVALID_TOKEN처럼 재시도가 무의미한 실패는
 * 예외를 던지지 않고 곧장 종료해 DLQ까지 가지 않도록 분리하기 위한 장치.
 */
public class RetryableApnsException extends RuntimeException {
    public RetryableApnsException(String message) {
        super(message);
    }
}
