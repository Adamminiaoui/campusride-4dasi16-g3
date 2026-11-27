package tn.campusride.booking.repository;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.campusride.booking.entity.Booking;
import tn.campusride.booking.entity.BookingStatus;
import java.util.Collection;

@Profile("crud")
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // Verrou de ligne compatible MySQL 8 et MariaDB fourni par XAMPP.
    @Query(value = "select * from bookings where id = :id for update", nativeQuery = true)
    Optional<Booking> findForUpdate(@Param("id") Long id);
    boolean existsByRideIdAndPassengerIdAndStatusIn(Long rideId, Long passengerId, Collection<BookingStatus> statuses);
}
