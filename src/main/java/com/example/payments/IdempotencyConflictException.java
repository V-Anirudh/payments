package com.example.payments;

// Thrown when an Idempotency-Key is reused with a different currency or
// amount than the request that first claimed it.
final class IdempotencyConflictException extends RuntimeException {
    IdempotencyConflictException(String idempotencyKey) {
        super("idempotency key already used with different parameters: " + idempotencyKey);
    }
}
