
package tn.campusride.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.campusride.review.entity.Report;
import tn.campusride.review.entity.ReportStatus;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatus(ReportStatus status);

    boolean existsByReviewIdAndReporterId(
            Long reviewId,
            Long reporterId
    );
}
