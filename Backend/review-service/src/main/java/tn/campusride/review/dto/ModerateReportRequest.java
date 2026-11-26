package tn.campusride.review.dto;

import jakarta.validation.constraints.NotNull;
import tn.campusride.review.entity.ReportStatus;

public record ModerateReportRequest(@NotNull ReportStatus status) { }
