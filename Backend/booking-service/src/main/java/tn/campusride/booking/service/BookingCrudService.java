package tn.campusride.booking.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.campusride.booking.dto.*;
import tn.campusride.booking.entity.*;
import tn.campusride.booking.exception.CrudException;
import tn.campusride.booking.repository.*;

/** Operations de demonstration locale, sans orchestration Ride/Payment ni identite JWT. */
@Profile("crud")
@Service
@Transactional(readOnly = true)
public class BookingCrudService {
    public record SampleRide(long id, String departure, String destination, long driverId, BigDecimal pricePerSeat, int seats) {}
    private static final Map<Long, SampleRide> SAMPLE_RIDES = Map.of(
        101L, new SampleRide(101L, "Tunis", "Ariana", 2L, new BigDecimal("5.00"), 3),
        102L, new SampleRide(102L, "Sousse", "Monastir", 3L, new BigDecimal("8.50"), 4));
    private final BookingRepository bookings;
    private final BookingStatusHistoryRepository histories;

    public BookingCrudService(BookingRepository bookings, BookingStatusHistoryRepository histories) {
        this.bookings = bookings;
        this.histories = histories;
    }

    public List<BookingResponse> listBookings() {
        return bookings.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(BookingResponse::from).toList();
    }

    public BookingResponse getBooking(Long id) { return BookingResponse.from(requireBooking(id)); }

    public List<SampleRide> sampleRides() { return SAMPLE_RIDES.values().stream().sorted(java.util.Comparator.comparingLong(SampleRide::id)).toList(); }

    @Transactional
    public BookingResponse createStandalone(StandaloneReservationRequest request) {
        SampleRide ride = SAMPLE_RIDES.get(request.rideId());
        if (ride == null) throw notFound("Trajet de demonstration", request.rideId());
        if (ride.driverId() == request.passengerId()) throw new CrudException(HttpStatus.BAD_REQUEST, "Un conducteur ne peut pas reserver son trajet.");
        if (request.seats() > ride.seats()) throw new CrudException(HttpStatus.CONFLICT, "Pas assez de places dans le trajet de demonstration.");
        if (bookings.existsByRideIdAndPassengerIdAndStatusIn(request.rideId(), request.passengerId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))
            throw new CrudException(HttpStatus.CONFLICT, "Ce passager a deja une reservation active sur ce trajet.");
        return createBooking(new BookingRequest(ride.id(), ride.driverId(), request.passengerId(), request.seats(),
                ride.pricePerSeat().multiply(BigDecimal.valueOf(request.seats())), BookingStatus.PENDING, request.passengerId()));
    }

    @Transactional
    public BookingResponse updateStandalone(Long id, StandaloneReservationRequest request) {
        Booking booking = lockBooking(id);
        if (!booking.getPassengerId().equals(request.passengerId()))
            throw new CrudException(HttpStatus.FORBIDDEN, "Cette reservation appartient a un autre passager.");
        if (booking.getStatus() != BookingStatus.PENDING)
            throw new CrudException(HttpStatus.CONFLICT, "Seule une reservation en attente peut etre modifiee.");
        if (!booking.getRideId().equals(request.rideId()))
            throw new CrudException(HttpStatus.BAD_REQUEST, "Le trajet ne peut pas etre modifie.");
        SampleRide ride = SAMPLE_RIDES.get(booking.getRideId());
        if (ride == null) throw notFound("Trajet de demonstration", booking.getRideId());
        if (request.seats() > ride.seats())
            throw new CrudException(HttpStatus.CONFLICT, "Pas assez de places dans le trajet de demonstration.");
        booking.setSeats(request.seats());
        booking.setTotalPrice(ride.pricePerSeat().multiply(BigDecimal.valueOf(request.seats())));
        return BookingResponse.from(booking);
    }

    @Transactional
    public void deleteStandalone(Long id, Long passengerId) {
        Booking booking = lockBooking(id);
        if (!booking.getPassengerId().equals(passengerId))
            throw new CrudException(HttpStatus.FORBIDDEN, "Cette reservation appartient a un autre passager.");
        if (booking.getStatus() != BookingStatus.PENDING)
            throw new CrudException(HttpStatus.CONFLICT, "Seule une reservation en attente peut etre supprimee dans ce test.");
        histories.deleteAllByBookingId(id);
        histories.flush();
        bookings.delete(booking);
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Booking booking = new Booking();
        applyBooking(booking, request);
        booking.setCreatedAt(now());
        bookings.save(booking);
        addHistory(booking, new HistoryRequest(null, booking.getStatus(), request.changedBy(), now()));
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = lockBooking(id);
        BookingStatus previousStatus = booking.getStatus();
        applyBooking(booking, request);
        if (previousStatus != booking.getStatus()) {
            addHistory(booking, new HistoryRequest(previousStatus, booking.getStatus(), request.changedBy(), now()));
        }
        return BookingResponse.from(booking);
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = lockBooking(id);
        histories.deleteAllByBookingId(id);
        histories.flush();
        bookings.delete(booking);
    }

    public List<HistoryResponse> listHistory(Long bookingId) {
        requireBooking(bookingId);
        return histories.findByBookingIdOrderByChangedAtAscIdAsc(bookingId).stream().map(HistoryResponse::from).toList();
    }

    public HistoryResponse getHistory(Long bookingId, Long historyId) {
        requireBooking(bookingId);
        return HistoryResponse.from(requireHistory(bookingId, historyId));
    }

    @Transactional
    public HistoryResponse createHistory(Long bookingId, HistoryRequest request) {
        return HistoryResponse.from(addHistory(lockBooking(bookingId), request));
    }

    @Transactional
    public HistoryResponse updateHistory(Long bookingId, Long historyId, HistoryRequest request) {
        lockBooking(bookingId);
        BookingStatusHistory history = requireHistory(bookingId, historyId);
        applyHistory(history, request);
        return HistoryResponse.from(history);
    }

    @Transactional
    public void deleteHistory(Long bookingId, Long historyId) {
        lockBooking(bookingId);
        histories.delete(requireHistory(bookingId, historyId));
    }

    private Booking requireBooking(Long id) {
        return bookings.findById(id).orElseThrow(() -> notFound("Reservation", id));
    }

    private Booking lockBooking(Long id) {
        return bookings.findForUpdate(id).orElseThrow(() -> notFound("Reservation", id));
    }

    private BookingStatusHistory requireHistory(Long bookingId, Long historyId) {
        return histories.findByIdAndBookingId(historyId, bookingId)
            .orElseThrow(() -> notFound("Historique de cette reservation", historyId));
    }

    private BookingStatusHistory addHistory(Booking booking, HistoryRequest request) {
        BookingStatusHistory history = new BookingStatusHistory();
        history.setBooking(booking);
        applyHistory(history, request);
        return histories.save(history);
    }

    private void applyBooking(Booking booking, BookingRequest request) {
        if (request.driverId().equals(request.passengerId())) {
            throw new CrudException(HttpStatus.BAD_REQUEST, "Le conducteur et le passager doivent etre differents.");
        }
        booking.setRideId(request.rideId());
        booking.setDriverId(request.driverId());
        booking.setPassengerId(request.passengerId());
        booking.setSeats(request.seats());
        booking.setTotalPrice(request.totalPrice());
        booking.setStatus(request.status());
    }

    private void applyHistory(BookingStatusHistory history, HistoryRequest request) {
        if (request.previousStatus() == request.newStatus()) {
            throw new CrudException(HttpStatus.BAD_REQUEST, "Les statuts avant et apres doivent etre differents.");
        }
        history.setPreviousStatus(request.previousStatus());
        history.setNewStatus(request.newStatus());
        history.setChangedBy(request.changedBy());
        history.setChangedAt(request.changedAt() == null ? now() : request.changedAt());
    }

    private CrudException notFound(String resource, Long id) {
        return new CrudException(HttpStatus.NOT_FOUND, resource + " introuvable : " + id);
    }

    private LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS); }
}
