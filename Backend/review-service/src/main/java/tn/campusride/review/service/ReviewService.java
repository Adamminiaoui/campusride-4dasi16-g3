
package tn.campusride.review.service;

import tn.campusride.review.entity.Review;
import tn.campusride.review.dto.CreateReviewRequest;
import tn.campusride.review.dto.UpdateReviewRequest;
import java.util.List;

public interface ReviewService {
    Review createReview(Long authorId, CreateReviewRequest request);
    Review getReview(Long id, Long requesterId);
    Review updateReview(Long id, Long authorId, UpdateReviewRequest request);
    void deleteReview(Long id, Long authorId);

    List<Review> getVisibleReviewsForUser(Long userId);

    boolean reviewExists(
            Long rideId,
            Long authorId,
            Long targetUserId
    );
}
