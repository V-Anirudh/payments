package com.example.payments;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

// Running net balance per currency.
//
// Thread safety comes from ConcurrentHashMap.merge: the remap runs atomically
// for a given key, so two POSTs to the same currency can't lose an update and
// we don't need our own lock. Different currencies don't contend at all.
public final class PaymentTracker {

    private final ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<>();

    // Idempotency-Key -> the balance that request produced. A retried request
    // with the same key returns the stored result instead of re-applying the amount.
    private final ConcurrentHashMap<String, BigDecimal> idempotencyResults = new ConcurrentHashMap<>();

    public BigDecimal record(String currency, BigDecimal amount) {
        return record(currency, amount, null);
    }

    public BigDecimal record(String currency, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey == null) {
            return balances.merge(currency, amount, BigDecimal::add);
        }
        // computeIfAbsent holds the bin lock for this key, so a concurrent
        // duplicate of the same request can't apply the merge twice.
        return idempotencyResults.computeIfAbsent(idempotencyKey,
                k -> balances.merge(currency, amount, BigDecimal::add));
    }

    // Empty if we've never seen the currency. One that nets back to zero is
    // still present and returns 0 - that's how GET tells "unknown" from "zero".
    public Optional<BigDecimal> balance(String currency) {
        return Optional.ofNullable(balances.get(currency));
    }

    // Sorted copy for the periodic dump. Weakly consistent on purpose: a global
    // point-in-time snapshot would mean locking out writers, not worth it here.
    public Map<String, BigDecimal> snapshot() {
        return new TreeMap<>(balances);
    }
}
