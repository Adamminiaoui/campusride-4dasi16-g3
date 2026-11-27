package tn.campusride.booking.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.campusride.booking.entity.BookingStatusHistory;

@Profile("crud")
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findByBookingIdOrderByChangedAtAscIdAsc(Long bookingId);
    Optional<BookingStatusHistory> findByIdAndBookingId(Long id, Long bookingId);
    void deleteAllByBookingId(Long bookingId);
}
