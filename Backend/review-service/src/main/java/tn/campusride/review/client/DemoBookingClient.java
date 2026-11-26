
package tn.campusride.review.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoBookingClient implements BookingClient {

    @Override
    public boolean isEligible(
            Long rideId,
            Long authorId,
            Long targetId) {

        // Simulation locale uniquement.
        // Aucun appel au vrai Booking Service.
        return rideId != null
                && rideId > 0
                && authorId != null
                && authorId > 0
                && targetId != null
                && targetId > 0
                && !authorId.equals(targetId);
    }
}
