
package tn.campusride.review.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tn.campusride.review.dto.CreateReportRequest;
import tn.campusride.review.entity.Report;
import tn.campusride.review.entity.ReportStatus;
import tn.campusride.review.repository.ReportRepository;
import tn.campusride.review.repository.ReviewRepository;
import tn.campusride.review.service.ReportService;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;

    public ReportServiceImpl(
            ReportRepository reportRepository,
            ReviewRepository reviewRepository) {

        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional
    public Report createReport(
            Long reviewId,
            Long reporterId,
            CreateReportRequest request) {

        // Verifier les identifiants
        if (reviewId == null || reviewId <= 0 ||
                reporterId == null || reporterId <= 0) {

            throw new IllegalArgumentException(
                    "Identifiants invalides"
            );
        }

        // Verifier la raison du signalement
        if (request == null || request.reason() == null ||
                request.reason().isBlank() ||
                request.reason().length() > 500) {

            throw new IllegalArgumentException(
                    "Raison du signalement invalide"
            );
        }

        // Verifier que l'avis existe
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Avis introuvable"
            );
        }

        // Empecher les signalements en double
        if (reportRepository.existsByReviewIdAndReporterId(
                reviewId, reporterId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cet utilisateur a deja signale cet avis"
            );
        }

        // Creer le signalement
        Report report = new Report();

        report.setReviewId(reviewId);
        report.setReporterId(reporterId);
        report.setReason(request.reason().trim());
        report.setStatus(ReportStatus.OPEN);

        // Enregistrer dans MariaDB
        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByStatus(
            ReportStatus status) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Statut du signalement obligatoire"
            );
        }

        return reportRepository.findByStatus(status);
    }
}
