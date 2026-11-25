
package tn.campusride.review.service;

import tn.campusride.review.dto.CreateReportRequest;
import tn.campusride.review.entity.Report;
import tn.campusride.review.entity.ReportStatus;

import java.util.List;

public interface ReportService {

    Report createReport(
            Long reviewId,
            Long reporterId,
            CreateReportRequest request
    );

    List<Report> getReportsByStatus(
            ReportStatus status
    );
}
