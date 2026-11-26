
package tn.campusride.review.exception;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Structure de la réponse d'erreur
    public record ApiError(
            int status,
            String error,
            String message,
            Instant timestamp
    ) {
    }

    // Gestion des erreurs métier
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> business(
            ResponseStatusException ex) {

        System.err.println("ERREUR METIER REVIEW SERVICE :");
        ex.printStackTrace();

        return error(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                ex.getReason()
        );
    }

    // Gestion des requêtes invalides (HTTP 400)
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> invalid(Exception ex) {

        System.err.println("=================================");
        System.err.println("ERREUR REVIEW SERVICE :");
        System.err.println("Type : " + ex.getClass().getName());
        System.err.println("Message : " + ex.getMessage());
        System.err.println("=================================");

        ex.printStackTrace();

        return error(
                HttpStatus.BAD_REQUEST,
                "Donnees de requete invalides"
        );
    }

    // Gestion des conflits (HTTP 409)
    @ExceptionHandler({
            DataIntegrityViolationException.class,
            OptimisticLockingFailureException.class
    })
    public ResponseEntity<ApiError> conflict(Exception ex) {

        System.err.println("CONFLIT REVIEW SERVICE :");
        ex.printStackTrace();

        return error(
                HttpStatus.CONFLICT,
                "Conflit de donnees ou modification concurrente"
        );
    }

    // Construction standard de la réponse JSON
    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String message) {

        ApiError apiError = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                Instant.now()
        );

        return ResponseEntity
                .status(status)
                .body(apiError);
    }
}
