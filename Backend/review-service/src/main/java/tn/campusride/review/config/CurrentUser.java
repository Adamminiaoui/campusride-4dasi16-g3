
package tn.campusride.review.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

    private final String idClaim;
    private final Environment environment;
    private final Long demoAuthorId;

    public CurrentUser(
            @Value("${review.security.user-id-claim:sub}")
            String idClaim,
            @Value("${review.demo.author-id:1}")
            Long demoAuthorId,
            Environment environment) {

        this.idClaim = idClaim;
        this.demoAuthorId = demoAuthorId;
        this.environment = environment;
    }

    public Long id(Jwt jwt) {

        // Identite fictive uniquement avec le profil demo actif.
        if (environment.acceptsProfiles(Profiles.of("demo"))) {
            return demoAuthorId;
        }

        // Fonctionnement normal : JWT obligatoire.
        Object value =
                jwt == null ? null : jwt.getClaims().get(idClaim);

        try {
            String text = String.valueOf(value);

            if (!text.matches("[1-9][0-9]*")) {
                throw new NumberFormatException();
            }

            return Long.valueOf(text);

        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Identifiant CampusRide absent du jeton"
            );
        }
    }
}
