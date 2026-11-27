package tn.campusride.ride.exception;

import org.springframework.http.HttpStatus;

public class CrudException extends RuntimeException {
    private final HttpStatus status;
    public CrudException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
