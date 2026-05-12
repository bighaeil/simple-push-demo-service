package com.example.pushdemo;

import com.example.pushdemo.common.Priority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriorityTest {

    @Test
    void topicMapping() {
        assertEquals("push.critical", Priority.CRITICAL.topic());
        assertEquals("push.high", Priority.HIGH.topic());
        assertEquals("push.normal", Priority.NORMAL.topic());
        assertEquals("push.bulk", Priority.LOW.topic());
    }

    @Test
    void onlyCriticalBypassesUserSettings() {
        assertTrue(Priority.CRITICAL.bypassUserSettings());
        assertFalse(Priority.HIGH.bypassUserSettings());
        assertFalse(Priority.NORMAL.bypassUserSettings());
        assertFalse(Priority.LOW.bypassUserSettings());
    }
}
