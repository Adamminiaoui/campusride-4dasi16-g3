package tn.campusride.booking.dto;

import java.time.LocalDateTime;
import tn.campusride.booking.entity.BookingStatus;
import tn.campusride.booking.entity.BookingStatusHistory;

public record HistoryResponse(Long id, Long bookingId, BookingStatus previousStatus,
                              BookingStatus newStatus, Long changedBy, LocalDateTime changedAt) {
    public static HistoryResponse from(BookingStatusHistory history) {
        return new HistoryResponse(history.getId(), history.getBooking().getId(),
            history.getPreviousStatus(), history.getNewStatus(), history.getChangedBy(), history.getChangedAt());
    }
}
