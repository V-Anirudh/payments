package com.example.payments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    void balanceEmitsAmountAsNumber() {
        assertEquals("{\"currency\":\"USD\",\"amount\":900.00}", Json.balance("USD", "900.00"));
    }

    @Test
    void balanceHandlesNegativeAmount() {
        assertEquals("{\"currency\":\"USD\",\"amount\":-100.00}", Json.balance("USD", "-100.00"));
    }

    @Test
    void errorEscapesQuotesAndBackslashes() {
        assertEquals("{\"error\":\"bad \\\"x\\\"\"}", Json.error("bad \"x\""));
        assertEquals("{\"error\":\"a\\\\b\"}", Json.error("a\\b"));
    }
}
