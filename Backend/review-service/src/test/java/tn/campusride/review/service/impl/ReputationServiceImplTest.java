
package tn.campusride.review.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.campusride.review.dto.ReputationResponse;
import tn.campusride.review.entity.Review;
import tn.campusride.review.entity.ReviewStatus;
import tn.campusride.review.repository.ReviewRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReputationServiceImpl reputationService;

    @Test
    void shouldReturnZeroWhenNoReviews() {

        when(reviewRepository.findByTargetUserIdAndStatus(
                5L, ReviewStatus.VISIBLE
        )).thenReturn(List.of());

        ReputationResponse result =
                reputationService.getReputation(5L);

        assertEquals(5L, result.userId());
        assertEquals(0, result.reviewCount());
        assertEquals(0, result.averageRating()
                .compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldCalculateAverageRating() {

        Review review1 = new Review();
        review1.setRating(5);

        Review review2 = new Review();
        review2.setRating(4);

        Review review3 = new Review();
        review3.setRating(3);

        when(reviewRepository.findByTargetUserIdAndStatus(
                5L, ReviewStatus.VISIBLE
        )).thenReturn(List.of(
                review1, review2, review3
        ));

        ReputationResponse result =
                reputationService.getReputation(5L);

        assertEquals(3, result.reviewCount());

        assertEquals(
                new BigDecimal("4.00"),
                result.averageRating()
        );
    }

    @Test
    void shouldRejectInvalidUserId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> reputationService.getReputation(0L)
        );

        verifyNoInteractions(reviewRepository);
    }
}
