package tn.campusride.review.config;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import static org.assertj.core.api.Assertions.*;

class SecurityConfigTest {
    @Test void missingIssuerRejectsAllTokens() {
        var decoder = new SecurityConfig().jwtDecoder("", "");
        assertThatThrownBy(() -> decoder.decode("arbitrary-token")).isInstanceOf(BadJwtException.class);
    }

    @Test void requiresExpiryAndAudience() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("2")
                .issuer("https://issuer.test").claim("aud", "review-test").build();
        assertThat(SecurityConfig.tokenValidator("https://issuer.test", "review-test").validate(jwt).hasErrors()).isTrue();
        Jwt complete = Jwt.withTokenValue("test").header("alg", "RS256").subject("2")
                .issuer("https://issuer.test").claim("aud", "review-test").expiresAt(Instant.now().plusSeconds(300)).build();
        assertThat(SecurityConfig.tokenValidator("https://issuer.test", "").validate(complete).hasErrors()).isTrue();
    }

    @Test void numericIdentityCanUseExplicitMappedClaim() {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "RS256").subject("uuid-subject")
                .claim("campusride_id", 42L).build();
        var environment = new org.springframework.mock.env.MockEnvironment();
        assertThat(new CurrentUser("campusride_id", 1L, environment).id(jwt)).isEqualTo(42L);
        assertThatThrownBy(() -> new CurrentUser("sub", 1L, environment).id(jwt)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
