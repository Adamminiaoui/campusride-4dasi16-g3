package tn.campusride.ride.dto;

import tn.campusride.ride.entity.City;

public record CityResponse(Long id, String name, String governorate) {
    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getGovernorate());
    }
}
