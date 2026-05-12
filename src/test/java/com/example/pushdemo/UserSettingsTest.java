package com.example.pushdemo;

import com.example.pushdemo.user.UserSettings;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class UserSettingsTest {

    @Test
    void noDndWindowWhenStartEndNull() {
        UserSettings s = new UserSettings(1L, true, null, null, "ko");
        assertFalse(s.inDnd(LocalTime.of(3, 0)));
    }

    @Test
    void overnightDndCoversMidnight() {
        UserSettings s = new UserSettings(1L, true, LocalTime.of(22, 0), LocalTime.of(7, 0), "ko");
        assertTrue(s.inDnd(LocalTime.of(23, 0)));
        assertTrue(s.inDnd(LocalTime.of(2, 0)));
        assertFalse(s.inDnd(LocalTime.of(12, 0)));
    }

    @Test
    void sameDayDndWindow() {
        UserSettings s = new UserSettings(1L, true, LocalTime.of(13, 0), LocalTime.of(14, 0), "ko");
        assertTrue(s.inDnd(LocalTime.of(13, 30)));
        assertFalse(s.inDnd(LocalTime.of(12, 0)));
        assertFalse(s.inDnd(LocalTime.of(15, 0)));
    }
}
