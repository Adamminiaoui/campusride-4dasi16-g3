package tn.campusride.review;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.math.BigDecimal;
import java.security.KeyPairGenerator;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import tn.campusride.review.client.BookingClient;
import tn.campusride.review.config.SecurityConfig;
import tn.campusride.review.dto.*;
import tn.campusride.review.entity.*;
import tn.campusride.review.repository.*;
import tn.campusride.review.service.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ReviewApplicationTest.TestJwtConfig.class)
class ReviewApplicationTest {
    @Autowired MockMvc mvc;
    @Autowired TestRestTemplate http;
    @Autowired ReviewService reviews;
    @Autowired ReportService reports;
    @Autowired ReputationService reputation;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ReportRepository reportRepository;
    @Autowired ReputationRepository reputationRepository;
    @Autowired JwtEncoder encoder;
    @MockitoBean BookingClient booking;

    @TestConfiguration
    static class TestJwtConfig {
        @Bean RSAKey testKey() throws Exception {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate()).keyID("isolated-test").build();
        }
        @Bean @Primary JwtDecoder testDecoder(RSAKey key) throws Exception {
            var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
            decoder.setJwtValidator(SecurityConfig.tokenValidator("https://issuer.test", "review-test"));
            return decoder;
        }
        @Bean JwtEncoder testEncoder(RSAKey key) {
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        }
    }

    @BeforeEach
    void cleanIsolatedDatabase() {
        reportRepository.deleteAll();
        reviewRepository.deleteAll();
        reputationRepository.deleteAll();
    }

    String token(String subject, String role) {
        return token(subject, role, "https://issuer.test", "review-test", Instant.now().plusSeconds(300));
    }

    String token(String subject, String role, String issuer, String audience, Instant expiry) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(subject).audience(List.of(audience))
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiry)
                .claim("realm_access", Map.of("roles", List.of(role))).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    Review create(Long author, Long ride, int rating) {
        when(booking.isEligible(ride, author, 5L)).thenReturn(true);
        return reviews.createReview(author, new CreateReviewRequest(ride, 5L, rating, "Avis"));
    }

    @Test void startsRealHttpServerAndExposesPublicReputation() {
        var response = http.getForEntity("/api/reviews/reputation/5", ReputationResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().reviewCount()).isZero();
    }

    @Test void createsReviewUsingSignedIdentityAndStoresReputation() throws Exception {
        when(booking.isEligible(10L, 2L, 5L)).thenReturn(true);
        mvc.perform(post("/api/reviews").header("Authorization", "Bearer " + token("2", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rideId\":10,\"targetUserId\":5,\"rating\":4,\"comment\":\"Bien\",\"authorId\":999}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.authorId").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
        assertThat(reputationRepository.findById(5L).orElseThrow().getAverageRating()).isEqualByComparingTo("4.00");
        verify(booking).isEligible(10L, 2L, 5L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"rideId\":0,\"targetUserId\":5,\"rating\":3}",
            "{\"rideId\":10,\"targetUserId\":5,\"rating\":6}", "{\"rideId\":10,\"targetUserId\":5,\"rating\":0}", "not-json"})
    void rejectsInvalidReviewBodies(String body) throws Exception {
        mvc.perform(post("/api/reviews").header("Authorization", "Bearer " + token("2", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(reviewRepository.count()).isZero();
        verifyNoInteractions(booking);
    }

    @Test void anonymousWritesAreUnauthorized() throws Exception {
        mvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/reviews/1/report").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void invalidExpiredIssuerAndAudienceTokensAreUnauthorized() throws Exception {
        for (String jwt : List.of("not-a-jwt", token("2", "ADMIN", "https://issuer.test", "review-test", Instant.now().minusSeconds(120)),
                token("2", "ADMIN", "https://wrong.test", "review-test", Instant.now().plusSeconds(300)),
                token("2", "ADMIN", "https://issuer.test", "wrong", Instant.now().plusSeconds(300)))) {
            mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + jwt)).andExpect(status().isUnauthorized());
        }
    }

    @Test void tamperedSignatureIsUnauthorized() throws Exception {
        String valid = token("2", "ADMIN");
        int start = valid.lastIndexOf('.') + 1;
        char replacement = valid.charAt(start) == 'A' ? 'B' : 'A';
        String changed = valid.substring(0, start) + replacement + valid.substring(start + 1);
        mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + changed)).andExpect(status().isUnauthorized());
    }

    @Test void keycloakUuidCannotBecomeNumericUserId() throws Exception {
        mvc.perform(post("/api/reviews").header("Authorization", "Bearer " + token("c895c626-61c7-47af-9abb-4b22a197dbe3", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rideId\":10,\"targetUserId\":5,\"rating\":4}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(booking);
    }

    @Test void rejectsSelfReviewIneligibleAndBookingUnavailable() {
        assertStatus(HttpStatus.FORBIDDEN, () -> reviews.createReview(5L, new CreateReviewRequest(10L, 5L, 3, null)));
        assertStatus(HttpStatus.FORBIDDEN, () -> reviews.createReview(2L, new CreateReviewRequest(10L, 5L, 3, null)));
        when(booking.isEligible(10L, 2L, 5L)).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> reviews.createReview(2L, new CreateReviewRequest(10L, 5L, 3, null)));
        assertThat(reviewRepository.count()).isZero();
        assertThat(reputationRepository.count()).isZero();
    }

    @Test void rejectsDuplicateReviewAtServiceAndDatabase() {
        var original = create(2L, 10L, 5);
        assertStatus(HttpStatus.CONFLICT, () -> reviews.createReview(2L, new CreateReviewRequest(10L, 5L, 2, null)));
        Review duplicate = new Review();
        duplicate.setRideId(original.getRideId()); duplicate.setAuthorId(2L); duplicate.setTargetUserId(5L); duplicate.setRating(2);
        assertThatThrownBy(() -> reviewRepository.saveAndFlush(duplicate))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(reviewRepository.count()).isEqualTo(1);
        assertThat(reputation.getReputation(5L).averageRating()).isEqualByComparingTo("5.00");
    }

    @Test void readsExistingAndRejectsMissingReview() {
        Review review = create(2L, 10L, 4);
        assertThat(reviews.getReview(review.getId(), null).getRating()).isEqualTo(4);
        assertStatus(HttpStatus.NOT_FOUND, () -> reviews.getReview(Long.MAX_VALUE, 2L));
    }

    @Test void onlyAuthorCanUpdateOrDeleteAndReputationIsRecalculated() {
        Review first = create(2L, 10L, 5);
        create(3L, 10L, 2);
        assertStatus(HttpStatus.FORBIDDEN, () -> reviews.updateReview(first.getId(), 3L, new UpdateReviewRequest(1, null)));
        assertStatus(HttpStatus.FORBIDDEN, () -> reviews.deleteReview(first.getId(), 3L));
        reviews.updateReview(first.getId(), 2L, new UpdateReviewRequest(4, "Modifie"));
        assertThat(reputationRepository.findById(5L).orElseThrow().getAverageRating()).isEqualByComparingTo("3.00");
        reviews.deleteReview(first.getId(), 2L);
        assertThat(reputation.getReputation(5L).reviewCount()).isEqualTo(1);
        assertThat(reputationRepository.findById(5L).orElseThrow().getAverageRating()).isEqualByComparingTo("2.00");
    }

    @Test void deletingLastReviewRemovesReportsAndResetsReputation() {
        Review review = create(2L, 10L, 5);
        reports.createReport(review.getId(), 3L, new CreateReportRequest("Inapproprie"));
        reviews.deleteReview(review.getId(), 2L);
        assertThat(reportRepository.count()).isZero();
        assertThat(reputationRepository.findById(5L).orElseThrow().getReviewCount()).isZero();
        assertThat(reputation.getReputation(5L).averageRating()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test void reportsAndModeratesThroughRestWithAdminRole() throws Exception {
        Review review = create(2L, 10L, 5);
        mvc.perform(post("/api/reviews/{id}/report", review.getId()).header("Authorization", "Bearer " + token("3", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" Insulte \"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.reason").value("Insulte"));
        long reportId = reportRepository.findAll().get(0).getId();
        mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + token("3", "PASSENGER")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/reports/{id}", reportId).header("Authorization", "Bearer " + token("3", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + token("9", "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("OPEN"));
        mvc.perform(put("/api/admin/reports/{id}", reportId).header("Authorization", "Bearer " + token("9", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(get("/api/reviews/user/5")).andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(reputationRepository.findById(5L).orElseThrow().getReviewCount()).isZero();
    }

    @Test void rejectsDuplicateReportAndInvalidTransitions() {
        Review review = create(2L, 10L, 5);
        Report report = reports.createReport(review.getId(), 3L, new CreateReportRequest("Insulte"));
        assertStatus(HttpStatus.CONFLICT, () -> reports.createReport(review.getId(), 3L, new CreateReportRequest("Insulte")));
        assertThatThrownBy(() -> reports.moderateReport(report.getId(), ReportStatus.OPEN)).isInstanceOf(IllegalArgumentException.class);
        reports.moderateReport(report.getId(), ReportStatus.DISMISSED);
        assertStatus(HttpStatus.CONFLICT, () -> reports.moderateReport(report.getId(), ReportStatus.ACCEPTED));
        assertStatus(HttpStatus.NOT_FOUND, () -> reports.moderateReport(Long.MAX_VALUE, ReportStatus.ACCEPTED));
        assertThat(reviews.getVisibleReviewsForUser(5L)).hasSize(1);
    }

    @Test void dismissingAnotherReportAndEditingDoNotUnhideReview() {
        Review review = create(2L, 10L, 5);
        Report first = reports.createReport(review.getId(), 3L, new CreateReportRequest("Premier"));
        Report second = reports.createReport(review.getId(), 4L, new CreateReportRequest("Second"));
        reports.moderateReport(first.getId(), ReportStatus.ACCEPTED);
        reports.moderateReport(second.getId(), ReportStatus.DISMISSED);
        reviews.updateReview(review.getId(), 2L, new UpdateReviewRequest(4, "Corrige"));
        assertThat(reviews.getVisibleReviewsForUser(5L)).isEmpty();
        assertStatus(HttpStatus.NOT_FOUND, () -> reviews.getReview(review.getId(), 3L));
        assertThat(reviews.getReview(review.getId(), 2L).getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(reputationRepository.findById(5L).orElseThrow().getReviewCount()).isZero();
    }

    @Test void mapsInvalidPathsReportsAndStatusesToHttpErrors() throws Exception {
        mvc.perform(get("/api/reviews/user/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reviews/reputation/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/reports?status=UNKNOWN").header("Authorization", "Bearer " + token("9", "ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reviews/999/report").header("Authorization", "Bearer " + token("3", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Test\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(post("/api/reviews/999/report").header("Authorization", "Bearer " + token("3", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void concurrentFirstReviewsMaintainSingleCorrectReputation() throws Exception {
        when(booking.isEligible(10L, 2L, 5L)).thenReturn(true);
        when(booking.isEligible(10L, 3L, 5L)).thenReturn(true);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var first = pool.submit(() -> { start.await(); return reviews.createReview(2L, new CreateReviewRequest(10L, 5L, 5, null)); });
            var second = pool.submit(() -> { start.await(); return reviews.createReview(3L, new CreateReviewRequest(10L, 5L, 2, null)); });
            start.countDown(); first.get(15, TimeUnit.SECONDS); second.get(15, TimeUnit.SECONDS);
            var result = reputationRepository.findById(5L).orElseThrow();
            assertThat(result.getReviewCount()).isEqualTo(2);
            assertThat(result.getAverageRating()).isEqualByComparingTo("3.50");
            assertThat(reputationRepository.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    private void assertStatus(HttpStatus expected, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(expected));
    }

    @Test void rejectsInvalidContentAtServiceBoundary() {
        for (Integer rating : new Integer[]{null, 0, 6}) {
            assertThatThrownBy(() -> reviews.createReview(2L, new CreateReviewRequest(10L, 5L, rating, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> reviews.createReview(2L, new CreateReviewRequest(10L, 5L, 3, "x".repeat(501))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reports.createReport(0L, 2L, new CreateReportRequest("raison")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reports.createReport(1L, 2L, new CreateReportRequest("x".repeat(501))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(booking);
    }

    @Test void databaseRejectsOrphanReport() {
        Report report = new Report();
        report.setReviewId(Long.MAX_VALUE); report.setReporterId(2L); report.setReason("Test");
        assertThatThrownBy(() -> reportRepository.saveAndFlush(report))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void concurrentDuplicateCreationCommitsExactlyOneReview() throws Exception {
        when(booking.isEligible(10L, 2L, 5L)).thenReturn(true);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> action = () -> {
            start.await();
            try {
                reviews.createReview(2L, new CreateReviewRequest(10L, 5L, 4, null));
                return true;
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                return false;
            } catch (ResponseStatusException ex) {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                return false;
            }
        };
        try {
            var first = pool.submit(action); var second = pool.submit(action); start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(reviewRepository.count()).isEqualTo(1);
            assertThat(reputationRepository.findById(5L).orElseThrow().getReviewCount()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void frontendCorsAllowsOnlyConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/reviews").header("Origin", "http://localhost:5500")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"));
        mvc.perform(options("/api/reviews").header("Origin", "https://untrusted.test")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test void normalProfileProtectsCrudEndpointsAndAuthorOwnership() throws Exception {
        Review review = create(2L, 10L, 4);
        mvc.perform(put("/api/reviews/{id}", review.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":3,\"comment\":\"Edit\"}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/reviews/{id}", review.getId())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/reviews/{id}", review.getId()).header("Authorization", "Bearer " + token("3", "PASSENGER")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/reviews/{id}", review.getId()).header("Authorization", "Bearer " + token("2", "PASSENGER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":3,\"comment\":\"Edit\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(3));
        mvc.perform(delete("/api/reviews/{id}", review.getId()).header("Authorization", "Bearer " + token("2", "PASSENGER")))
                .andExpect(status().isNoContent());
    }
}
