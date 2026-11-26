
package tn.campusride.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(

        @NotNull
        @Positive
        Long rideId,

        @NotNull
        @Positive
        Long targetUserId,

        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @Size(max = 500)
        String comment

) {
}
