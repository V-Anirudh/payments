package com.example.payments;

import io.muserver.Method;
import io.muserver.MuResponse;
import io.muserver.MuServer;
import io.muserver.MuServerBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Pattern;

// The HTTP endpoints. Split out from App mainly so a test can start it on a
// random port and drive it over real HTTP (start() returns the handle).
final class PaymentServer {

    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

    private PaymentServer() {
    }

    static MuServer start(PaymentTracker tracker, int port) {
        return MuServerBuilder.httpServer()
                .withHttpPort(port) // 0 -> bind a free port
                .addHandler(Method.POST, "/payments/{currency}/{amount}", (req, resp, pathParams) -> {
                    var currency = pathParams.get("currency");
                    if (!CURRENCY.matcher(currency).matches()) {
                        json(resp, 400, Json.error("currency must be 3 uppercase letters, got: " + currency));
                        return;
                    }
                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(pathParams.get("amount"));
                    } catch (NumberFormatException e) {
                        json(resp, 400, Json.error("amount is not a number: " + pathParams.get("amount")));
                        return;
                    }
                    String idempotencyKey = req.headers().get("Idempotency-Key");
                    BigDecimal balance;
                    try {
                        balance = tracker.record(currency, amount, idempotencyKey);
                    } catch (IdempotencyConflictException e) {
                        json(resp, 409, Json.error(e.getMessage()));
                        return;
                    }
                    json(resp, 200, Json.balance(currency, money(balance)));
                })
                .addHandler(Method.GET, "/payments/{currency}", (req, resp, pathParams) -> {
                    var currency = pathParams.get("currency");
                    if (!CURRENCY.matcher(currency).matches()) {
                        json(resp, 400, Json.error("currency must be 3 uppercase letters, got: " + currency));
                        return;
                    }
                    Optional<BigDecimal> balance = tracker.balance(currency);
                    if (balance.isEmpty()) {
                        json(resp, 404, Json.error("no payments recorded for " + currency));
                        return;
                    }
                    json(resp, 200, Json.balance(currency, money(balance.get())));
                })
                .start();
    }

    // Display only: 2 dp to match the spec's examples. Sums keep full precision.
    static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    private static void json(MuResponse resp, int status, String body) {
        resp.status(status);
        resp.contentType("application/json");
        resp.write(body);
    }
}
