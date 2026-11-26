
package tn.campusride.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import tn.campusride.review.entity.Reputation;

public interface ReputationRepository
        extends JpaRepository<Reputation, Long> {
    // MySQL/MariaDB atomic initialization also serializes concurrent first reviews.
    @Modifying
    @Query(value = "insert into reputations (user_id, average_rating, review_count) values (:userId, 0, 0) "
            + "on duplicate key update user_id = user_id", nativeQuery = true)
    void initialize(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reputation r where r.userId = :userId")
    Reputation lock(@Param("userId") Long userId);
}
