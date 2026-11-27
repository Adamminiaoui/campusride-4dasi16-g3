package tn.campusride.booking.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.campusride.booking.dto.*;
import tn.campusride.booking.service.BookingCrudService;

@Profile("crud")
@RestController
@RequestMapping("/dev/bookings")
public class BookingCrudController {
    private final BookingCrudService service;
    public BookingCrudController(BookingCrudService service) { this.service = service; }

    @GetMapping
    public List<BookingResponse> list() { return service.listBookings(); }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable("id") Long id) { return service.getBooking(id); }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        BookingResponse booking = service.createBooking(request);
        return ResponseEntity.created(URI.create("/dev/bookings/" + booking.id())).body(booking);
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable("id") Long id, @Valid @RequestBody BookingRequest request) {
        return service.updateBooking(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
