
package tn.campusride.review.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("!demo")
public class RestBookingClient implements BookingClient {

    private final RestClient client;
    private final String baseUrl;
    private final String internalKey;
    private final String eligibilityPointer;

    public RestBookingClient(
            RestClient.Builder builder,
            @Value("${review.booking.base-url:}") String baseUrl,
            @Value("${review.booking.internal-key:}") String internalKey,
            @Value("${review.booking.eligibility-pointer:UNCONFIGURED}")
            String eligibilityPointer) {

        this.baseUrl = baseUrl;
        this.internalKey = internalKey;
        this.eligibilityPointer = eligibilityPointer;

        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );

        factory.setReadTimeout(Duration.ofSeconds(3));

        this.client = builder.requestFactory(factory).build();
    }

    @Override
    public boolean isEligible(
            Long rideId,
            Long authorId,
            Long targetId) {

        if (baseUrl.isBlank()
                || internalKey.isBlank()
                || eligibilityPointer.equals("UNCONFIGURED")) {
            throw unavailable();
        }

        try {
            JsonNode body = client.get()
                    .uri(
                            baseUrl
                                    + "/internal/bookings/eligibility"
                                    + "?rideId={ride}&authorId={author}&targetId={target}",
                            rideId,
                            authorId,
                            targetId
                    )
                    .header("X-Internal-Key", internalKey)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode eligible =
                    body == null ? null
                            : eligibilityPointer.equals("$")
                            ? body
                            : body.at(eligibilityPointer);

            if (eligible == null || !eligible.isBoolean()) {
                throw unavailable();
            }

            return eligible.booleanValue();

        } catch (RestClientException | IllegalArgumentException ex) {
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Verification Booking indisponible"
        );
    }
}
