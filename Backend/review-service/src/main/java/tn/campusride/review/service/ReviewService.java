
package tn.campusride.review.service;

import tn.campusride.review.entity.Review;
import java.util.List;

public interface ReviewService {

    List<Review> getVisibleReviewsForUser(Long userId);

    boolean reviewExists(
            Long rideId,
            Long authorId,
            Long targetUserId
    );
}
