
package tn.campusride.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.campusride.review.entity.Review;
import tn.campusride.review.entity.ReviewStatus;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findForUpdate(@Param("id") Long id);

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
