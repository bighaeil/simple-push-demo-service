package com.example.pushdemo.apns;

/**
 * APNs 호출 결과 3분류.
 *
 * <ul>
 *   <li>{@link #SUCCESS} — 정상 도달. 더 할 일 없음.</li>
 *   <li>{@link #RETRYABLE_FAILURE} — 일시 장애. Worker가 {@code RetryableApnsException}을 던져 재시도 토픽으로 흘려보낸다.</li>
 *   <li>{@link #INVALID_TOKEN} — 토큰이 죽었음(BadDeviceToken/Unregistered). 재시도해도 의미 없으니
 *       토큰만 비활성화하고 즉시 종료.</li>
 * </ul>
 */
public enum ApnsResult {
    SUCCESS,
    RETRYABLE_FAILURE,
    INVALID_TOKEN
}
