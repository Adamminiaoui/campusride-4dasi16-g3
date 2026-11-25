
package tn.campusride.review.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.campusride.review.repository.ReviewRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void shouldDetectExistingReview() {
        when(reviewRepository
                .existsByRideIdAndAuthorIdAndTargetUserId(
                        10L, 2L, 5L
                ))
                .thenReturn(true);

        assertTrue(
                reviewService.reviewExists(10L, 2L, 5L)
        );
    }

    @Test
    void shouldAllowNewReviewCombination() {
        when(reviewRepository
                .existsByRideIdAndAuthorIdAndTargetUserId(
                        10L, 2L, 5L
                ))
                .thenReturn(false);

        assertFalse(
                reviewService.reviewExists(10L, 2L, 5L)
        );
    }

    @Test
    void shouldRejectInvalidIdentifiers() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.reviewExists(
                        10L, null, 5L
                )
        );

        verifyNoInteractions(reviewRepository);
    }
}
