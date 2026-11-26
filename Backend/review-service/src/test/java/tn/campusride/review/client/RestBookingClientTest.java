package tn.campusride.review.client;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class RestBookingClientTest {
    HttpServer server;
    String url;
    AtomicReference<String> query = new AtomicReference<>();
    AtomicReference<String> key = new AtomicReference<>();
    String body = "{\"eligible\":true}";
    int status = 200;

    @BeforeEach void startStub() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/bookings/eligibility", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            key.set(exchange.getRequestHeaders().getFirst("X-Internal-Key"));
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, data.length);
            try (var output = exchange.getResponseBody()) { output.write(data); }
            exchange.close();
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach void stopStub() { server.stop(0); }

    RestBookingClient client(String pointer) {
        return new RestBookingClient(RestClient.builder(), url, "test-internal-key", pointer);
    }

    @Test void usesDocumentedEndpointAndExplicitlyConfiguredResponsePointer() {
        assertThat(client("/eligible").isEligible(10L, 2L, 5L)).isTrue();
        assertThat(query.get()).isEqualTo("rideId=10&authorId=2&targetId=5");
        assertThat(key.get()).isEqualTo("test-internal-key");
        body = "{\"eligible\":false}";
        assertThat(client("/eligible").isEligible(10L, 2L, 5L)).isFalse();
    }

    @Test void supportsExplicitBooleanRootWithoutAssumingTeamContract() {
        body = "true";
        assertThat(client("$").isEligible(10L, 2L, 5L)).isTrue();
    }

    @Test void missingContractFailsClosedWithoutCallingBooking() {
        unavailable(client("UNCONFIGURED"));
        assertThat(query.get()).isNull();
        unavailable(new RestBookingClient(RestClient.builder(), url, "", "/eligible"));
        assertThat(query.get()).isNull();
    }

    @Test void rejectsMalformedMissingAndNonBooleanResponses() {
        for (String value : new String[]{"{}", "{\"eligible\":\"true\"}", "{broken", "null"}) {
            body = value;
            unavailable(client("/eligible"));
        }
    }

    @Test void upstreamHttpErrorsBecome503() {
        for (int code : new int[]{401, 403, 404, 500, 503}) {
            status = code;
            unavailable(client("/eligible"));
        }
    }

    @Test void connectionFailureBecomes503() {
        server.stop(0);
        unavailable(client("/eligible"));
    }

    void unavailable(RestBookingClient client) {
        assertThatThrownBy(() -> client.isEligible(10L, 2L, 5L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
