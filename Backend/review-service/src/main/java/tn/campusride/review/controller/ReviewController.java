
package tn.campusride.review.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import tn.campusride.review.entity.Review;
import tn.campusride.review.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

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
}
