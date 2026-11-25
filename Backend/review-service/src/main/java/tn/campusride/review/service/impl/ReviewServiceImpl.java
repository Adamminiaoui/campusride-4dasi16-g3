
package tn.campusride.review.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.campusride.review.entity.Review;
import tn.campusride.review.entity.ReviewStatus;
import tn.campusride.review.repository.ReviewRepository;
import tn.campusride.review.service.ReviewService;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getVisibleReviewsForUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "Identifiant utilisateur invalide"
            );
        }

        return reviewRepository.findByTargetUserIdAndStatus(
                userId,
                ReviewStatus.VISIBLE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean reviewExists(
            Long rideId,
            Long authorId,
            Long targetUserId) {

        if (rideId == null || rideId <= 0 ||
                authorId == null || authorId <= 0 ||
                targetUserId == null || targetUserId <= 0) {

            throw new IllegalArgumentException(
                    "Identifiants invalides"
            );
        }

        return reviewRepository
                .existsByRideIdAndAuthorIdAndTargetUserId(
                        rideId,
                        authorId,
                        targetUserId
                );
    }
}
