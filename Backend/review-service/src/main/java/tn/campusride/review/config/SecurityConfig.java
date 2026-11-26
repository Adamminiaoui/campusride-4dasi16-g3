
package tn.campusride.review.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile("!demo")
public class SecurityConfig {

    // Validation des tokens JWT
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${review.security.issuer-uri:}") String issuer,
            @Value("${review.security.audience:}") String audience) {

        if (issuer.isBlank()) {
            return token -> {
                throw new BadJwtException(
                        "JWT issuer is not configured"
                );
            };
        }

        NimbusJwtDecoder decoder =
                JwtDecoders.fromIssuerLocation(issuer);

        decoder.setJwtValidator(
                tokenValidator(issuer, audience)
        );

        return decoder;
    }

    // Verification des informations du token
    public static OAuth2TokenValidator<Jwt> tokenValidator(
            String issuer,
            String audience) {

        OAuth2TokenValidator<Jwt> requiredClaims = token -> {

            boolean valid =
                    token.getExpiresAt() != null
                            && token.getSubject() != null
                            && !audience.isBlank()
                            && token.getAudience().contains(audience);

            if (valid) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            "Required JWT claims missing",
                            null
                    )
            );
        };

        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                requiredClaims
        );
    }

    // Configuration Spring Security
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            var authorities =
                    new LinkedHashSet<GrantedAuthority>();

            addRoles(
                    authorities,
                    jwt.getClaims().get("roles")
            );

            Object realm =
                    jwt.getClaims().get("realm_access");

            if (realm instanceof Map<?, ?> map) {
                addRoles(authorities, map.get("roles"));
            }

            return authorities;
        });

        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth

                        // GET public : avis d'un utilisateur
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/user/*",
                                "/api/reviews/reputation/*"
                        ).permitAll()

                        // Administration : role ADMIN
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // CREATE : creer un avis
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reviews",
                                "/api/reviews/*/report"
                        ).authenticated()

                        // READ : consulter un avis par ID
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reviews/{id}"
                        ).authenticated()

                        // UPDATE : modifier un avis
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/reviews/{id}"
                        ).authenticated()

                        // DELETE : supprimer un avis
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/reviews/{id}"
                        ).authenticated()

                        // Autres routes interdites
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(converter)
                        )
                )
                .build();
    }

    // Configuration CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${review.frontend-origin:http://localhost:5500}")
            String origin) {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(
                java.util.List.of(origin)
        );

        config.setAllowedMethods(
                java.util.List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                java.util.List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                config
        );

        return source;
    }

    // Conversion des roles JWT
    private static void addRoles(
            Collection<GrantedAuthority> result,
            Object roles) {

        if (roles instanceof Collection<?> values) {

            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role ->
                            new SimpleGrantedAuthority(
                                    role.startsWith("ROLE_")
                                            ? role
                                            : "ROLE_" + role
                            )
                    )
                    .forEach(result::add);
        }
    }
}
