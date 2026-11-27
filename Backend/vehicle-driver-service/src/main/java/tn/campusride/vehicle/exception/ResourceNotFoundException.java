package tn.campusride.vehicle.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(Long id) {
        super("Véhicule introuvable avec l'id " + id);
    }
}
