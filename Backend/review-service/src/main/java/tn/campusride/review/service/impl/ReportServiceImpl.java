
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
import org.springframework.transaction.annotation.Isolation;
import tn.campusride.review.entity.ReviewStatus;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final ReputationStore reputationStore;

    public ReportServiceImpl(
            ReportRepository reportRepository,
            ReviewRepository reviewRepository, ReputationStore reputationStore) {

        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
        this.reputationStore = reputationStore;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Report moderateReport(Long id, ReportStatus status) {
        if (id == null || id <= 0 || status == null || status == ReportStatus.OPEN) {
            throw new IllegalArgumentException("Decision invalide");
        }
        Report report = reportRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Signalement introuvable"));
        if (report.getStatus() != ReportStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Signalement deja traite");
        }
        var review = reviewRepository.findForUpdate(report.getReviewId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Avis introuvable"));
        if (status == ReportStatus.ACCEPTED) {
            reputationStore.lock(review.getTargetUserId());
            review.setStatus(ReviewStatus.HIDDEN);
            reputationStore.refresh(review.getTargetUserId());
        }
        // Dismissal leaves the review unchanged, including an earlier hiding by another report.
        report.setStatus(status);
        return reportRepository.saveAndFlush(report);
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
        if (reviewRepository.findForUpdate(reviewId).isEmpty()) {
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
