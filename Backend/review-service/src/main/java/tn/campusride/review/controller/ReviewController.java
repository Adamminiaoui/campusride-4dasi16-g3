
package tn.campusride.review.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import tn.campusride.review.config.CurrentUser;
import tn.campusride.review.dto.CreateReviewRequest;
import tn.campusride.review.dto.UpdateReviewRequest;
import tn.campusride.review.entity.Review;
import tn.campusride.review.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    public ReviewController(
            ReviewService reviewService,
            CurrentUser currentUser) {

        this.reviewService = reviewService;
        this.currentUser = currentUser;
    }

    // CREATE : Creer un avis
    @PostMapping
    public ResponseEntity<Review> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReviewRequest request) {

        Review review = reviewService.createReview(
                currentUser.id(jwt),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(review);
    }

    // READ : Consulter les avis d'un utilisateur
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> getUserReviews(
            @PathVariable("userId") Long userId) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiant utilisateur invalide"
            );
        }

        List<Review> reviews =
                reviewService.getVisibleReviewsForUser(userId);

        return ResponseEntity.ok(reviews);
    }

    // READ : Consulter un avis par son identifiant
    @GetMapping("/{id}")
    public ResponseEntity<Review> getById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Review review = reviewService.getReview(
                id,
                currentUser.id(jwt)
        );

        return ResponseEntity.ok(review);
    }

    // UPDATE : Modifier un avis
    @PutMapping("/{id}")
    public ResponseEntity<Review> update(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateReviewRequest request) {

        Review review = reviewService.updateReview(
                id,
                currentUser.id(jwt),
                request
        );

        return ResponseEntity.ok(review);
    }

    // DELETE : Supprimer un avis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal Jwt jwt) {

        reviewService.deleteReview(
                id,
                currentUser.id(jwt)
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
