package tn.campusride.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import tn.campusride.booking.entity.Booking;
import tn.campusride.booking.entity.BookingStatus;

public record BookingResponse(Long id, Long rideId, Long driverId, Long passengerId,
                              int seats, BigDecimal totalPrice, BookingStatus status,
                              LocalDateTime createdAt) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getRideId(), booking.getDriverId(),
            booking.getPassengerId(), booking.getSeats(), booking.getTotalPrice(),
            booking.getStatus(), booking.getCreatedAt());
    }
}
