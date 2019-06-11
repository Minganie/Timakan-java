package com.hydrometeocharlevoix.timakan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void formatTime() {
        long one = 1L;
        assertEquals(Main.formatTime(one), "1ns");
        long milleun = 1001L;
        assertEquals(Main.formatTime(milleun), "1us 1ns");
        long million = 1001001L;
        assertEquals(Main.formatTime(million), "1ms 1us 1ns");
        long milliard = 1001001001L;
        assertEquals(Main.formatTime(milliard), "1s 1ms 1us 1ns");
    }
}