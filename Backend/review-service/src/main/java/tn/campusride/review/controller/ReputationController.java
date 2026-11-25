
package tn.campusride.review.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import tn.campusride.review.dto.ReputationResponse;
import tn.campusride.review.service.ReputationService;

@RestController
@RequestMapping("/api/reviews/reputation")
public class ReputationController {

    private final ReputationService reputationService;

    public ReputationController(ReputationService reputationService) {
        this.reputationService = reputationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ReputationResponse> getReputation(
            @PathVariable("userId") Long userId) {

        if (userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identifiant utilisateur invalide"
            );
        }

        return ResponseEntity.ok(
                reputationService.getReputation(userId)
        );
    }
}
