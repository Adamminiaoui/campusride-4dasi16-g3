package tn.campusride.ride.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.campusride.ride.entity.City;

@Profile("crud")
public interface CityRepository extends JpaRepository<City, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
