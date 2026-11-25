
package tn.campusride.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.campusride.review.entity.Review;
import tn.campusride.review.entity.ReviewStatus;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTargetUserIdAndStatus(
            Long targetUserId,
            ReviewStatus status
    );

    boolean existsByRideIdAndAuthorIdAndTargetUserId(
            Long rideId,
            Long authorId,
            Long targetUserId
    );
}
