package tn.campusride.booking.dto;

import jakarta.validation.constraints.*;

public record StandaloneReservationRequest(@NotNull @Positive Long rideId, @NotNull @Positive Long passengerId,
                                            @NotNull @Min(1) Integer seats) {}
