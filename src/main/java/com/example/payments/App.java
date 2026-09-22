package com.example.payments;

import io.muserver.MuServer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class App {

    public static void main(String[] args) {
        Config config;
        try {
            config = Config.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return; // not reached, but keeps `config` definitely assigned
        }

        var tracker = new PaymentTracker();
        MuServer server = PaymentServer.start(tracker, config.port);
        System.out.println("listening on " + server.uri() + " (summary every " + config.intervalSeconds + "s)");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "summary");
            t.setDaemon(true); // don't keep the JVM alive on its own
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> printSummary(tracker),
                config.intervalSeconds, config.intervalSeconds, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdownNow();
            server.stop();
        }));
    }

    private static void printSummary(PaymentTracker tracker) {
        var sb = new StringBuilder();
        for (Map.Entry<String, BigDecimal> e : tracker.snapshot().entrySet()) {
            sb.append(e.getKey()).append(' ').append(PaymentServer.money(e.getValue())).append('\n');
        }
        System.out.print(sb); // single write so concurrent output can't split a summary
    }
}
