package tn.campusride.ride.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.campusride.ride.dto.CityRequest;
import tn.campusride.ride.dto.CityResponse;
import tn.campusride.ride.service.CityCrudService;

@Profile("crud")
@RestController
@RequestMapping("/dev/cities")
public class CityCrudController {
    private final CityCrudService service;
    public CityCrudController(CityCrudService service) { this.service = service; }

    @GetMapping
    public List<CityResponse> list() { return service.listCities(); }

    @GetMapping("/{id}")
    public CityResponse get(@PathVariable("id") Long id) { return service.getCity(id); }

    @PostMapping
    public ResponseEntity<CityResponse> create(@Valid @RequestBody CityRequest request) {
        CityResponse city = service.createCity(request);
        return ResponseEntity.created(URI.create("/dev/cities/" + city.id())).body(city);
    }

    @PutMapping("/{id}")
    public CityResponse update(@PathVariable("id") Long id, @Valid @RequestBody CityRequest request) {
        return service.updateCity(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}
