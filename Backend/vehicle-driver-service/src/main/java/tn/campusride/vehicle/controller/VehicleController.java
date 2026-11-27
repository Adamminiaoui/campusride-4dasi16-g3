package tn.campusride.vehicle.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.campusride.vehicle.entity.Vehicle;
import tn.campusride.vehicle.exception.DuplicatePlateException;
import tn.campusride.vehicle.exception.ResourceNotFoundException;
import tn.campusride.vehicle.repository.VehicleRepository;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleRepository vehicles;

    public VehicleController(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }
    @GetMapping("/hello")
    public String hello() {
        return "Vehicle & Driver Service OK";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle create(@Valid @RequestBody Vehicle vehicle) {
        ensurePlateAvailable(vehicle.getPlate(), null);
        return vehicles.save(vehicle);
    }

    @GetMapping
    public List<Vehicle> getAll() {
        return vehicles.findAll();
    }

    @GetMapping("/{id}")
    public Vehicle getOne(@PathVariable("id") Long id) {
        return findVehicle(id);
    }

    @PutMapping("/{id}")
    public Vehicle update(@PathVariable("id") Long id, @Valid @RequestBody Vehicle input) {
        Vehicle vehicle = findVehicle(id);
        ensurePlateAvailable(input.getPlate(), id);
        vehicle.setDriverId(input.getDriverId());
        vehicle.setBrand(input.getBrand());
        vehicle.setModel(input.getModel());
        vehicle.setPlate(input.getPlate());
        vehicle.setSeats(input.getSeats());
        vehicle.setColor(input.getColor());
        vehicle.setStatus(input.getStatus());
        return vehicles.save(vehicle);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        vehicles.delete(findVehicle(id));
    }

    private Vehicle findVehicle(Long id) {
        return vehicles.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private void ensurePlateAvailable(String plate, Long currentId) {
        boolean exists = currentId == null
                ? vehicles.existsByPlateIgnoreCase(plate)
                : vehicles.existsByPlateIgnoreCaseAndIdNot(plate, currentId);
        if (exists) throw new DuplicatePlateException(plate);
    }
}
