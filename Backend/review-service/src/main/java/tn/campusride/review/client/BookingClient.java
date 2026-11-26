package tn.campusride.review.client;

public interface BookingClient {
    boolean isEligible(Long rideId, Long authorId, Long targetId);
}
