package tn.campusride.ride.exception;

import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Profile("crud")
@RestControllerAdvice(basePackages = "tn.campusride.ride.controller")
public class CrudExceptionHandler {
    public record ApiError(int status, String error, String message, Instant timestamp) {}

    @ExceptionHandler(CrudException.class)
    ResponseEntity<ApiError> handleCrud(CrudException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + " : " + e.getDefaultMessage()).sorted().collect(Collectors.joining(" ; "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleInvalidInput(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Requete invalide. Verifiez le format JSON et les champs name et governorate.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Conflit : cette ville existe deja ou est encore utilisee.");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> handleDatabaseError(DataAccessException exception) {
        org.slf4j.LoggerFactory.getLogger(CrudExceptionHandler.class).error("Erreur de base du CRUD Ride", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur d'acces a la base de donnees. Consultez les journaux du service.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), message, Instant.now()));
    }
}
