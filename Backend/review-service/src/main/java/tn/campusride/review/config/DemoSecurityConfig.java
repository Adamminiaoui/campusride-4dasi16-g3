
package tn.campusride.review.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("demo")
public class DemoSecurityConfig {

    @Bean
    public SecurityFilterChain demoSecurityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth

                        // Interface web de demonstration
                        .requestMatchers("/reviews.html")
                        .permitAll()

                        // CRUD Review Service
                        .requestMatchers(
                                "/api/reviews",
                                "/api/reviews/**"
                        )
                        .permitAll()

                        // Endpoints Actuator
                        .requestMatchers(
                                "/actuator",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/metrics",
                                "/actuator/metrics/**"
                        )
                        .permitAll()

                        // Bloquer les autres requetes
                        .anyRequest()
                        .denyAll()
                )
                .build();
    }
}
