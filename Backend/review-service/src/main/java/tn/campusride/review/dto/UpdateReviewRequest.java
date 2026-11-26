package tn.campusride.review.dto;

import jakarta.validation.constraints.*;

public record UpdateReviewRequest(@NotNull @Min(1) @Max(5) Integer rating, @Size(max = 500) String comment) { }
