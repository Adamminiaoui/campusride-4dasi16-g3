package tn.campusride.vehicle.exception;

public class DuplicatePlateException extends RuntimeException {
    public DuplicatePlateException(String plate) {
        super("La plaque " + plate + " est déjà utilisée");
    }
}
