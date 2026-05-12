package com.example.pushdemo.common;

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
