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
@RequestMapping("/dev/bookings/{bookingId}/history")
public class BookingStatusHistoryCrudController {
    private final BookingCrudService service;
    public BookingStatusHistoryCrudController(BookingCrudService service) { this.service = service; }

    @GetMapping
    public List<HistoryResponse> list(@PathVariable("bookingId") Long bookingId) {
        return service.listHistory(bookingId);
    }

    @GetMapping("/{historyId}")
    public HistoryResponse get(@PathVariable("bookingId") Long bookingId, @PathVariable("historyId") Long historyId) {
        return service.getHistory(bookingId, historyId);
    }

    @PostMapping
    public ResponseEntity<HistoryResponse> create(@PathVariable("bookingId") Long bookingId,
                                                 @Valid @RequestBody HistoryRequest request) {
        HistoryResponse history = service.createHistory(bookingId, request);
        return ResponseEntity.created(URI.create("/dev/bookings/" + bookingId + "/history/" + history.id())).body(history);
    }

    @PutMapping("/{historyId}")
    public HistoryResponse update(@PathVariable("bookingId") Long bookingId, @PathVariable("historyId") Long historyId,
                                  @Valid @RequestBody HistoryRequest request) {
        return service.updateHistory(bookingId, historyId, request);
    }

    @DeleteMapping("/{historyId}")
    public ResponseEntity<Void> delete(@PathVariable("bookingId") Long bookingId, @PathVariable("historyId") Long historyId) {
        service.deleteHistory(bookingId, historyId);
        return ResponseEntity.noContent().build();
    }
}
