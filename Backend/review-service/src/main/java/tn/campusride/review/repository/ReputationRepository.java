
package tn.campusride.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.campusride.review.entity.Reputation;

public interface ReputationRepository
        extends JpaRepository<Reputation, Long> {
}
