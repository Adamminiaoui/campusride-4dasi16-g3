
package tn.campusride.review.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;

@Configuration
@Profile("demo")
public class DemoSecurityConfig {

    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> demoLoopbackOnly() {
        // Enforce loopback even if SERVER_ADDRESS or a command-line argument overrides the profile.
        return factory -> {
            try { factory.setAddress(InetAddress.getByName("127.0.0.1")); }
            catch (java.net.UnknownHostException ex) { throw new IllegalStateException(ex); }
        };
    }

    @Bean
    public SecurityFilterChain demoSecurityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                            FilterChain chain) throws ServletException, IOException {
                        String host = request.getServerName();
                        String origin = request.getHeader("Origin");
                        boolean localHost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
                        String expectedOrigin = request.getScheme() + "://" + request.getHeader("Host");
                        if (!localHost || !InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()
                                || (origin != null && !origin.equalsIgnoreCase(expectedOrigin))) {
                            response.sendError(403);
                            return;
                        }
                        chain.doFilter(request, response);
                    }
                }, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/reviews.html")
                        .permitAll()
                        .requestMatchers("/api/reviews/**")
                        .permitAll()
                        .anyRequest()
                        .denyAll()
                )
                .build();
    }
}
