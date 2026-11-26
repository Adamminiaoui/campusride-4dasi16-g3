
package tn.campusride.review.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.campusride.review.entity.Review;
import tn.campusride.review.entity.ReviewStatus;
import tn.campusride.review.repository.ReviewRepository;
import tn.campusride.review.service.ReviewService;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import tn.campusride.review.client.BookingClient;
import tn.campusride.review.dto.CreateReviewRequest;
import tn.campusride.review.dto.UpdateReviewRequest;
import tn.campusride.review.repository.ReportRepository;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingClient bookingClient;
    private final ReputationStore reputationStore;
    private final ReportRepository reportRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, BookingClient bookingClient,
                             ReputationStore reputationStore, ReportRepository reportRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingClient = bookingClient;
        this.reputationStore = reputationStore;
        this.reportRepository = reportRepository;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Review createReview(Long authorId, CreateReviewRequest request) {
        validId(authorId);
        if (request == null) throw new IllegalArgumentException("Avis obligatoire");
        validId(request.rideId());
        validId(request.targetUserId());
        validContent(request.rating(), request.comment());
        if (authorId.equals(request.targetUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Auto-evaluation interdite");
        }
        if (reviewExists(request.rideId(), authorId, request.targetUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Avis deja publie");
        }
        if (!bookingClient.isEligible(request.rideId(), authorId, request.targetUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Participants non eligibles");
        }
        reputationStore.lock(request.targetUserId());
        Review review = new Review();
        review.setRideId(request.rideId());
        review.setAuthorId(authorId);
        review.setTargetUserId(request.targetUserId());
        review.setRating(request.rating());
        review.setComment(request.comment());
        Review saved = reviewRepository.saveAndFlush(review);
        reputationStore.refresh(request.targetUserId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Review getReview(Long id, Long requesterId) {
        validId(id);
        Review review = reviewRepository.findById(id).orElseThrow(ReviewServiceImpl::missing);
        if (review.getStatus() == ReviewStatus.HIDDEN && !review.getAuthorId().equals(requesterId)) throw missing();
        return review;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Review updateReview(Long id, Long authorId, UpdateReviewRequest request) {
        if (request == null) throw new IllegalArgumentException("Avis obligatoire");
        validContent(request.rating(), request.comment());
        Review review = ownedReview(id, authorId);
        reputationStore.lock(review.getTargetUserId());
        review.setRating(request.rating());
        review.setComment(request.comment());
        // Editing never reverses an administrator's moderation decision.
        reputationStore.refresh(review.getTargetUserId());
        return review;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteReview(Long id, Long authorId) {
        Review review = ownedReview(id, authorId);
        reputationStore.lock(review.getTargetUserId());
        reportRepository.deleteByReviewId(id);
        reviewRepository.delete(review);
        reputationStore.refresh(review.getTargetUserId());
    }

    private Review ownedReview(Long id, Long authorId) {
        validId(id);
        validId(authorId);
        Review review = reviewRepository.findForUpdate(id).orElseThrow(ReviewServiceImpl::missing);
        if (!review.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'auteur peut modifier cet avis");
        }
        return review;
    }

    private static void validId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Identifiant invalide");
    }

    private static void validContent(Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5 || (comment != null && comment.length() > 500)) {
            throw new IllegalArgumentException("Note ou commentaire invalide");
        }
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable");
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
