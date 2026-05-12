package com.example.pushdemo.common;

/**
 * 알림 우선순위 — 라우팅(어느 토픽) + 사용자 설정 우회 규칙을 한 곳에 묶는다.
 *
 * <p>{@link #CRITICAL}만 {@link #bypassUserSettings()}=true 이며, DND/푸시 OFF를 무시하고 발송된다.
 * (예: 결제 알림은 사용자가 푸시를 꺼놨어도 도달해야 한다는 비즈니스 합의)
 */
public enum Priority {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW;

    public String topic() {
        return switch (this) {
            case CRITICAL -> "push.critical";
            case HIGH -> "push.high";
            case NORMAL -> "push.normal";
            case LOW -> "push.bulk";
        };
    }

    public boolean bypassUserSettings() {
        return this == CRITICAL;
    }
}
