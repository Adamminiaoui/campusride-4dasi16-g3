
package tn.campusride.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(

        @NotBlank(message = "La raison est obligatoire")
        @Size(max = 500, message = "Maximum 500 caractères")
        String reason

) {
}
