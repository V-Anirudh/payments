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

    // Idempotency-Key -> the request that first claimed it, plus the balance
    // it produced. A retried request with the same key and the same
    // currency/amount returns the stored balance instead of re-applying it;
    // the same key reused with different parameters is a conflict.
    private final ConcurrentHashMap<String, IdempotentRequest> idempotencyResults = new ConcurrentHashMap<>();

    private record IdempotentRequest(String currency, BigDecimal amount, BigDecimal balance) {
    }

    public BigDecimal record(String currency, BigDecimal amount) {
        return record(currency, amount, null);
    }

    public BigDecimal record(String currency, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey == null) {
            return balances.merge(currency, amount, BigDecimal::add);
        }
        // computeIfAbsent holds the bin lock for this key, so a concurrent
        // duplicate of the same request can't apply the merge twice.
        IdempotentRequest first = idempotencyResults.computeIfAbsent(idempotencyKey,
                k -> new IdempotentRequest(currency, amount, balances.merge(currency, amount, BigDecimal::add)));

        boolean sameRequest = first.currency().equals(currency) && first.amount().compareTo(amount) == 0;
        if (!sameRequest) {
            throw new IdempotencyConflictException(idempotencyKey);
        }
        return first.balance();
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
