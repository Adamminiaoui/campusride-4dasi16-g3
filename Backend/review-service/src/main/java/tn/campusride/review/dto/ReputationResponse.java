
package tn.campusride.review.dto;

import java.math.BigDecimal;

public record ReputationResponse(
        Long userId,
        BigDecimal averageRating,
        Integer reviewCount
) {
}
