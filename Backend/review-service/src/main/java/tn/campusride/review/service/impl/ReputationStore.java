package tn.campusride.review.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tn.campusride.review.repository.ReputationRepository;
import tn.campusride.review.repository.ReviewRepository;
import tn.campusride.review.service.ReputationService;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ReputationStore {
    private final ReputationRepository reputations;
    private final ReviewRepository reviews;
    private final ReputationService calculator;

    public ReputationStore(ReputationRepository reputations, ReviewRepository reviews, ReputationService calculator) {
        this.reputations = reputations;
        this.reviews = reviews;
        this.calculator = calculator;
    }

    public void lock(Long userId) {
        reputations.initialize(userId);
        reputations.lock(userId);
    }

    public void refresh(Long userId) {
        reviews.flush();
        var value = calculator.getReputation(userId);
        var stored = reputations.lock(userId);
        stored.setAverageRating(value.averageRating());
        stored.setReviewCount(value.reviewCount());
    }
}
