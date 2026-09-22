package com.example.payments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTest {

    @Test
    void usesDefaultsWhenNoArgs() {
        Config c = Config.parse(new String[]{});
        assertEquals(60, c.intervalSeconds);
        assertEquals(8080, c.port);
    }

    @Test
    void parsesIntervalAndPort() {
        Config c = Config.parse(new String[]{"--interval=10", "--port=9090"});
        assertEquals(10, c.intervalSeconds);
        assertEquals(9090, c.port);
    }

    @Test
    void rejectsNonPositiveInterval() {
        assertThrows(IllegalArgumentException.class, () -> Config.parse(new String[]{"--interval=0"}));
        assertThrows(IllegalArgumentException.class, () -> Config.parse(new String[]{"--interval=-5"}));
    }

    @Test
    void rejectsNonIntegerValue() {
        assertThrows(IllegalArgumentException.class, () -> Config.parse(new String[]{"--interval=abc"}));
    }

    @Test
    void rejectsUnknownFlag() {
        assertThrows(IllegalArgumentException.class, () -> Config.parse(new String[]{"--bogus=1"}));
    }
}
