package tn.campusride.review.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.campusride.review.config.CurrentUser;
import tn.campusride.review.dto.*;
import tn.campusride.review.entity.*;
import tn.campusride.review.service.ReportService;

@RestController
public class ReportController {
    private final ReportService reports;
    private final CurrentUser currentUser;

    public ReportController(ReportService reports, CurrentUser currentUser) {
        this.reports = reports;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/reviews/{id}/report")
    ResponseEntity<Report> create(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reports.createReport(id, currentUser.id(jwt), request));
    }

    @GetMapping("/api/admin/reports")
    List<Report> list(@RequestParam(name = "status", defaultValue = "OPEN") ReportStatus status) {
        return reports.getReportsByStatus(status);
    }

    @PutMapping("/api/admin/reports/{id}")
    Report moderate(@PathVariable("id") Long id, @Valid @RequestBody ModerateReportRequest request) {
        return reports.moderateReport(id, request.status());
    }
}
