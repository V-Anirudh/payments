package com.example.payments;

import io.muserver.MuServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Starts the real server on a random port and hits it with the JDK HTTP client.
// Each test uses its own currency so order doesn't matter.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentServerIntegrationTest {

    private MuServer server;
    private HttpClient client;
    private URI base;

    @BeforeAll
    void startServer() {
        server = PaymentServer.start(new PaymentTracker(), 0); // 0 = random free port
        base = server.uri();
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    private HttpResponse<String> post(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(base.resolve(path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(base.resolve(path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void postReturnsNewBalance() throws Exception {
        HttpResponse<String> resp = post("/payments/AAA/100");
        assertEquals(200, resp.statusCode());
        assertEquals("{\"currency\":\"AAA\",\"amount\":100.00}", resp.body());
    }

    @Test
    void postsAccumulateAndGetReflectsRunningTotal() throws Exception {
        post("/payments/BBB/1000");
        post("/payments/BBB/-100");
        HttpResponse<String> resp = get("/payments/BBB");
        assertEquals(200, resp.statusCode());
        assertEquals("{\"currency\":\"BBB\",\"amount\":900.00}", resp.body());
    }

    @Test
    void negativeAmountIsAccepted() throws Exception {
        HttpResponse<String> resp = post("/payments/CCC/-100");
        assertEquals(200, resp.statusCode());
        assertEquals("{\"currency\":\"CCC\",\"amount\":-100.00}", resp.body());
    }

    @Test
    void getUnknownCurrencyReturns404() throws Exception {
        HttpResponse<String> resp = get("/payments/ZZZ");
        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""));
    }

    @Test
    void invalidCurrencyReturns400() throws Exception {
        HttpResponse<String> resp = post("/payments/us/100");
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""));
    }

    @Test
    void invalidAmountReturns400() throws Exception {
        HttpResponse<String> resp = post("/payments/USD/abc");
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""));
    }
}
