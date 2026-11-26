
package tn.campusride.review.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tn.campusride.review.dto.CreateReportRequest;
import tn.campusride.review.entity.Report;
import tn.campusride.review.entity.ReportStatus;
import tn.campusride.review.repository.ReportRepository;
import tn.campusride.review.repository.ReviewRepository;

import java.util.List;
import java.util.Optional;
import tn.campusride.review.entity.Review;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void shouldCreateReportSuccessfully() {

        CreateReportRequest request =
                new CreateReportRequest(" Commentaire inapproprie ");

        when(reviewRepository.findForUpdate(10L))
                .thenReturn(Optional.of(new Review()));

        when(reportRepository.save(any(Report.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Report result = reportService.createReport(
                10L, 2L, request
        );

        assertEquals(10L, result.getReviewId());
        assertEquals(2L, result.getReporterId());
        assertEquals("Commentaire inapproprie",
                result.getReason());
        assertEquals(ReportStatus.OPEN,
                result.getStatus());

        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void shouldRejectNonExistingReview() {

        when(reviewRepository.findForUpdate(99L))
                .thenReturn(Optional.empty());

        CreateReportRequest request =
                new CreateReportRequest("Avis incorrect");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reportService.createReport(
                                99L, 2L, request
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(reportRepository, never())
                .save(any(Report.class));
    }

    @Test
    void shouldRejectDuplicateReport() {

        when(reviewRepository.findForUpdate(10L))
                .thenReturn(Optional.of(new Review()));

        when(reportRepository
                .existsByReviewIdAndReporterId(10L, 2L))
                .thenReturn(true);

        CreateReportRequest request =
                new CreateReportRequest("Avis inapproprie");

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> reportService.createReport(
                                10L, 2L, request
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(reportRepository, never())
                .save(any(Report.class));
    }

    @Test
    void shouldRejectInvalidReason() {

        CreateReportRequest request =
                new CreateReportRequest("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> reportService.createReport(
                        10L, 2L, request
                )
        );

        verifyNoInteractions(
                reviewRepository, reportRepository
        );
    }

    @Test
    void shouldGetOpenReports() {

        Report report = new Report();
        report.setStatus(ReportStatus.OPEN);

        when(reportRepository.findByStatus(ReportStatus.OPEN))
                .thenReturn(List.of(report));

        List<Report> results =
                reportService.getReportsByStatus(
                        ReportStatus.OPEN
                );

        assertEquals(1, results.size());
        assertEquals(
                ReportStatus.OPEN,
                results.get(0).getStatus()
        );
    }
}
