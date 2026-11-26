package tn.campusride.review;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.net.InetAddress;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tn.campusride.review.entity.Review;
import tn.campusride.review.dto.ReputationResponse;

@ActiveProfiles("demo")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "server.address=0.0.0.0", "eureka.client.enabled=false"
})
class DemoApplicationTest {
    @Autowired TestRestTemplate http;
    @Autowired MockMvc mvc;
    @Autowired ConfigurableServletWebServerFactory factory;

    @Test void demoCrudUsesRealServiceAndRecalculatesReputation() {
        var page = http.getForEntity("/reviews.html", String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody()).contains("Créer un avis", "/api/reviews");
        var created = http.postForEntity("/api/reviews", Map.of("rideId", 700, "targetUserId", 8,
                "rating", 5, "comment", "Démonstration"), Review.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = created.getBody().getId();
        assertThat(created.getBody().getAuthorId()).isEqualTo(1L);
        assertThat(http.getForEntity("/api/reviews/" + id, Review.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        var edited = http.exchange("/api/reviews/" + id, HttpMethod.PUT,
                new HttpEntity<>(Map.of("rating", 3, "comment", "Modifié")), Review.class);
        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edited.getBody().getRating()).isEqualTo(3);
        assertThat(http.getForObject("/api/reviews/reputation/8", ReputationResponse.class).averageRating()).isEqualByComparingTo("3.00");
        assertThat(http.exchange("/api/reviews/" + id, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.getForEntity("/api/reviews/" + id, String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(http.getForObject("/api/reviews/reputation/8", ReputationResponse.class).reviewCount()).isZero();
    }

    @Test void forcesLoopbackEvenWhenAddressIsOverridden() throws Exception {
        var tomcat = (org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory) factory;
        assertThat(tomcat.getAddress()).isEqualTo(InetAddress.getByName("127.0.0.1"));
    }

    @Test void rejectsRemoteAddressesAndBrowserCrossOriginCalls() throws Exception {
        mvc.perform(get("/reviews.html").with(request -> { request.setRemoteAddr("192.0.2.1"); return request; }))
                .andExpect(status().isForbidden());
        mvc.perform(get("/reviews.html").header("Host", "untrusted.test").with(request -> {
            request.setServerName("untrusted.test"); return request;
        })).andExpect(status().isForbidden());
        mvc.perform(post("/api/reviews").header("Origin", "https://untrusted.test")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test void demoDoesNotOpenAdministration() throws Exception {
        mvc.perform(get("/api/admin/reports")).andExpect(status().isForbidden());
    }
}
