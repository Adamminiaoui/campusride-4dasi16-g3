
package tn.campusride.review.service;

import tn.campusride.review.dto.ReputationResponse;

public interface ReputationService {

    ReputationResponse getReputation(Long userId);
}
