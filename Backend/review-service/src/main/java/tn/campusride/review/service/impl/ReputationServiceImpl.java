
package tn.campusride.review.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.campusride.review.dto.ReputationResponse;
import tn.campusride.review.entity.Review;
import tn.campusride.review.entity.ReviewStatus;
import tn.campusride.review.repository.ReviewRepository;
import tn.campusride.review.service.ReputationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReputationServiceImpl implements ReputationService {

    private final ReviewRepository reviewRepository;

    public ReputationServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReputationResponse getReputation(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "Identifiant utilisateur invalide"
            );
        }

        List<Review> reviews =
                reviewRepository.findByTargetUserIdAndStatus(
                        userId, ReviewStatus.VISIBLE
                );

        int count = reviews.size();

        if (count == 0) {
            return new ReputationResponse(
                    userId, BigDecimal.ZERO, 0
            );
        }

        int total = reviews.stream()
                .mapToInt(Review::getRating)
                .sum();

        BigDecimal average = BigDecimal.valueOf(total)
                .divide(
                        BigDecimal.valueOf(count),
                        2,
                        RoundingMode.HALF_UP
                );

        return new ReputationResponse(userId, average, count);
    }
}
