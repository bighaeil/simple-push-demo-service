package com.example.pushdemo.apns;

public class RetryableApnsException extends RuntimeException {
    public RetryableApnsException(String message) {
        super(message);
    }
}
