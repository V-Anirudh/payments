package com.example.payments;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentTrackerTest {

    @Test
    void recordAccumulatesAndReturnsNewBalance() {
        PaymentTracker tracker = new PaymentTracker();
        assertEquals(0, tracker.record("USD", new BigDecimal("1000")).compareTo(new BigDecimal("1000")));
        assertEquals(0, tracker.record("USD", new BigDecimal("-100")).compareTo(new BigDecimal("900")));
        assertEquals(0, tracker.balance("USD").orElseThrow().compareTo(new BigDecimal("900")));
    }

    @Test
    void decimalPrecisionIsPreservedInSums() {
        PaymentTracker tracker = new PaymentTracker();
        tracker.record("CNY", new BigDecimal("99.50"));
        tracker.record("CNY", new BigDecimal("0.005"));
        assertEquals(0, tracker.balance("CNY").orElseThrow().compareTo(new BigDecimal("99.505")));
    }

    @Test
    void currencyThatNetsToZeroIsStillPresent() {
        PaymentTracker tracker = new PaymentTracker();
        tracker.record("HKD", new BigDecimal("100"));
        tracker.record("HKD", new BigDecimal("-100"));
        assertTrue(tracker.balance("HKD").isPresent());
        assertEquals(0, tracker.balance("HKD").orElseThrow().compareTo(BigDecimal.ZERO));
    }

    @Test
    void unknownCurrencyIsEmpty() {
        PaymentTracker tracker = new PaymentTracker();
        assertTrue(tracker.balance("EUR").isEmpty());
    }

    @Test
    void snapshotIsSortedByCurrency() {
        PaymentTracker tracker = new PaymentTracker();
        tracker.record("USD", BigDecimal.ONE);
        tracker.record("CNY", BigDecimal.ONE);
        tracker.record("HKD", BigDecimal.ONE);
        assertEquals(List.of("CNY", "HKD", "USD"), new ArrayList<>(tracker.snapshot().keySet()));
    }

    @Test
    void concurrentRecordsDoNotLoseUpdates() throws Exception {
        PaymentTracker tracker = new PaymentTracker();
        int threads = 8;
        int itersPerThread = 100_000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int k = 0; k < itersPerThread; k++) {
                    tracker.record("USD", BigDecimal.ONE);
                }
            }));
        }
        startGate.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        BigDecimal expected = new BigDecimal((long) threads * itersPerThread);
        assertEquals(0, tracker.balance("USD").orElseThrow().compareTo(expected),
                "all concurrent increments must be reflected exactly");
    }
}
