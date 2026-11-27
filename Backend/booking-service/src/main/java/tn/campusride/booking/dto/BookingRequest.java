package tn.campusride.booking.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import tn.campusride.booking.entity.BookingStatus;

public record BookingRequest(
    @NotNull @Positive Long rideId,
    @NotNull @Positive Long driverId,
    @NotNull @Positive Long passengerId,
    @NotNull @Min(1) Integer seats,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal totalPrice,
    @NotNull BookingStatus status,
    @NotNull @Positive Long changedBy
) {}
