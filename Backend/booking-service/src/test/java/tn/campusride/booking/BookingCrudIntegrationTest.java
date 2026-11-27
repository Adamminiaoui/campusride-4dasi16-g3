package tn.campusride.booking;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.campusride.booking.dto.*;
import tn.campusride.booking.entity.BookingStatus;
import tn.campusride.booking.exception.CrudException;
import tn.campusride.booking.repository.*;
import tn.campusride.booking.service.BookingCrudService;

@ActiveProfiles("crud")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "spring.datasource.url=jdbc:h2:mem:booking-tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.config.import=",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class BookingCrudIntegrationTest {
    @Autowired BookingCrudService service;
    @Autowired BookingRepository bookings;
    @Autowired BookingStatusHistoryRepository histories;

    @BeforeEach
    void cleanDatabase() { histories.deleteAll(); bookings.deleteAll(); }

    @Test
    void passengerCanReserveWithoutOtherServices() {
        BookingResponse booking = service.createStandalone(new StandaloneReservationRequest(101L, 7L, 2));
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.totalPrice()).isEqualByComparingTo("10.00");
        assertThatThrownBy(() -> service.createStandalone(new StandaloneReservationRequest(101L, 7L, 1))).isInstanceOf(CrudException.class);
        assertThat(service.listHistory(booking.id())).hasSize(1);
        service.deleteBooking(booking.id());
        assertThat(service.listBookings()).isEmpty();
    }

    @Test
    void passengerEditsAndDeletesPendingReservation() {
        BookingResponse booking = service.createStandalone(new StandaloneReservationRequest(101L, 7L, 1));
        assertThatThrownBy(() -> service.updateStandalone(booking.id(), new StandaloneReservationRequest(101L, 8L, 2))).isInstanceOf(CrudException.class);
        assertThatThrownBy(() -> service.updateStandalone(booking.id(), new StandaloneReservationRequest(101L, 7L, 4))).isInstanceOf(CrudException.class);
        BookingResponse updated = service.updateStandalone(booking.id(), new StandaloneReservationRequest(101L, 7L, 2));
        assertThat(updated.totalPrice()).isEqualByComparingTo("10.00");
        assertThat(updated.seats()).isEqualTo(2);
        assertThat(service.listHistory(booking.id())).hasSize(1);
        assertThatThrownBy(() -> service.deleteStandalone(booking.id(), 8L)).isInstanceOf(CrudException.class);
        service.deleteStandalone(booking.id(), 7L);
        assertThat(bookings.findById(booking.id())).isEmpty();
        assertThat(histories.findByBookingIdOrderByChangedAtAscIdAsc(booking.id())).isEmpty();
    }

    @Test
    void bookingChangesArePersistedAndStatusChangesAreRecorded() {
        BookingResponse created = service.createBooking(request(BookingStatus.PENDING));
        assertThat(created.id()).isPositive();
        assertThat(created.createdAt()).isNotNull();
        assertThat(service.getBooking(created.id()).totalPrice()).isEqualByComparingTo("24.50");
        var initial = service.listHistory(created.id());
        assertThat(initial).hasSize(1);
        assertThat(initial.get(0).previousStatus()).isNull();
        assertThat(initial.get(0).newStatus()).isEqualTo(BookingStatus.PENDING);

        BookingResponse confirmed = service.updateBooking(created.id(), request(BookingStatus.CONFIRMED));
        assertThat(confirmed.createdAt()).isEqualTo(created.createdAt());
        var changes = service.listHistory(created.id());
        assertThat(changes).hasSize(2);
        assertThat(changes.get(1).previousStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(changes.get(1).newStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(changes.get(1).changedBy()).isEqualTo(7L);

        service.updateBooking(created.id(), request(BookingStatus.CONFIRMED));
        assertThat(service.listHistory(created.id())).hasSize(2);
    }

    @Test
    void historyCanBeCreatedUpdatedAndDeletedWithinItsBooking() {
        Long bookingId = service.createBooking(request(BookingStatus.PENDING)).id();
        LocalDateTime date = LocalDateTime.of(2026, 11, 25, 12, 0);
        HistoryResponse created = service.createHistory(bookingId,
            new HistoryRequest(BookingStatus.PENDING, BookingStatus.CONFIRMED, 8L, date));
        assertThat(service.getHistory(bookingId, created.id()).changedAt()).isEqualTo(date);
        service.updateHistory(bookingId, created.id(),
            new HistoryRequest(BookingStatus.PENDING, BookingStatus.CANCELLED, 9L, date.plusHours(1)));
        assertThat(service.getHistory(bookingId, created.id()).newStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(service.getHistory(bookingId, created.id()).changedBy()).isEqualTo(9L);
        assertThat(service.getBooking(bookingId).status()).isEqualTo(BookingStatus.PENDING);
        service.deleteHistory(bookingId, created.id());
        assertThatThrownBy(() -> service.getHistory(bookingId, created.id())).isInstanceOf(CrudException.class);
    }

    @Test
    void historyCannotBeAccessedThroughAnotherBooking() {
        Long first = service.createBooking(request(BookingStatus.PENDING)).id();
        Long second = service.createBooking(request(BookingStatus.PENDING)).id();
        Long historyId = service.listHistory(first).get(0).id();
        assertThatThrownBy(() -> service.deleteHistory(second, historyId)).isInstanceOf(CrudException.class);
        assertThat(service.getHistory(first, historyId)).isNotNull();
    }

    @Test
    void deletingBookingAlsoDeletesItsHistory() {
        Long id = service.createBooking(request(BookingStatus.PENDING)).id();
        service.updateBooking(id, request(BookingStatus.CONFIRMED));
        service.deleteBooking(id);
        assertThat(bookings.findById(id)).isEmpty();
        assertThat(histories.findByBookingIdOrderByChangedAtAscIdAsc(id)).isEmpty();
    }

    @Test
    void historyInsertFailureRollsBackBookingCreation() {
        var invalid = new BookingRequest(101L, 2L, 7L, 2, new BigDecimal("24.50"), BookingStatus.PENDING, null);
        assertThatThrownBy(() -> service.createBooking(invalid)).isInstanceOf(RuntimeException.class);
        assertThat(bookings.count()).isZero();
        assertThat(histories.count()).isZero();
    }

    @Test
    void unchangedHistoryStatusIsRejected() {
        Long id = service.createBooking(request(BookingStatus.PENDING)).id();
        assertThatThrownBy(() -> service.createHistory(id,
            new HistoryRequest(BookingStatus.PENDING, BookingStatus.PENDING, 7L, null)))
            .isInstanceOf(CrudException.class);
        assertThat(service.listHistory(id)).hasSize(1);
    }

    private BookingRequest request(BookingStatus status) {
        return new BookingRequest(101L, 2L, 7L, 2, new BigDecimal("24.50"), status, 7L);
    }
}
