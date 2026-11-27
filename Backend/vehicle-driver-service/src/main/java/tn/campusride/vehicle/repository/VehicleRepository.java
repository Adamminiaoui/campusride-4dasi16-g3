package tn.campusride.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.campusride.vehicle.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByPlateIgnoreCase(String plate);
    boolean existsByPlateIgnoreCaseAndIdNot(String plate, Long id);
}
