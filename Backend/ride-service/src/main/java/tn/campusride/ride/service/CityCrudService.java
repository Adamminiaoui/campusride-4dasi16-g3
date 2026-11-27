package tn.campusride.ride.service;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.campusride.ride.dto.CityRequest;
import tn.campusride.ride.dto.CityResponse;
import tn.campusride.ride.entity.City;
import tn.campusride.ride.exception.CrudException;
import tn.campusride.ride.repository.CityRepository;

@Profile("crud")
@Service
@Transactional(readOnly = true)
public class CityCrudService {
    private final CityRepository cities;

    public CityCrudService(CityRepository cities) { this.cities = cities; }

    public List<CityResponse> listCities() {
        return cities.findAll(Sort.by("name")).stream().map(CityResponse::from).toList();
    }

    public CityResponse getCity(Long id) { return CityResponse.from(requireCity(id)); }

    @Transactional
    public CityResponse createCity(CityRequest request) {
        String name = request.name().trim();
        if (cities.existsByNameIgnoreCase(name)) throw duplicate(name);
        City city = new City();
        apply(city, request);
        return CityResponse.from(cities.save(city));
    }

    @Transactional
    public CityResponse updateCity(Long id, CityRequest request) {
        City city = requireCity(id);
        String name = request.name().trim();
        if (cities.existsByNameIgnoreCaseAndIdNot(name, id)) throw duplicate(name);
        apply(city, request);
        return CityResponse.from(city);
    }

    @Transactional
    public void deleteCity(Long id) {
        cities.delete(requireCity(id));
    }

    private void apply(City city, CityRequest request) {
        city.setName(request.name().trim());
        city.setGovernorate(request.governorate().trim());
    }

    private City requireCity(Long id) {
        return cities.findById(id)
            .orElseThrow(() -> new CrudException(HttpStatus.NOT_FOUND, "Ville introuvable : " + id));
    }

    private CrudException duplicate(String name) {
        return new CrudException(HttpStatus.CONFLICT, "La ville " + name + " existe deja.");
    }
}
