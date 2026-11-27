package tn.campusride.booking.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import tn.campusride.booking.entity.BookingStatus;

public record HistoryRequest(BookingStatus previousStatus,
                             @NotNull BookingStatus newStatus,
                             @NotNull @Positive Long changedBy,
                             LocalDateTime changedAt) {}
