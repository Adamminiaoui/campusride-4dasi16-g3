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
@RequestMapping("/dev/standalone")
public class StandaloneReservationController {
    private final BookingCrudService service;
    public StandaloneReservationController(BookingCrudService service) { this.service = service; }
    @GetMapping("/rides") public List<BookingCrudService.SampleRide> rides() { return service.sampleRides(); }
    @PostMapping("/bookings") public ResponseEntity<BookingResponse> reserve(@Valid @RequestBody StandaloneReservationRequest request) {
        BookingResponse result = service.createStandalone(request);
        return ResponseEntity.created(URI.create("/dev/bookings/" + result.id())).body(result);
    }
    @PutMapping("/bookings/{id}") public BookingResponse update(@PathVariable("id") Long id, @Valid @RequestBody StandaloneReservationRequest request) {
        return service.updateStandalone(id, request);
    }
    @DeleteMapping("/bookings/{id}") public ResponseEntity<Void> delete(@PathVariable("id") Long id, @RequestParam("passengerId") @jakarta.validation.constraints.Positive Long passengerId) {
        service.deleteStandalone(id, passengerId);
        return ResponseEntity.noContent().build();
    }
}
