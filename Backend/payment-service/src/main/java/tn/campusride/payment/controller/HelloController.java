package tn.campusride.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur de vérification pour l'atelier Eureka.
 *
 * TEMPORAIRE atelier : À SUPPRIMER avant l'intégration.
 * Cet endpoint n'est PAS sécurisé et ne fait PAS partie du contrat d'API
 * du payment-service. Il sert uniquement à confirmer que le service démarre
 * correctement et est joignable via son port ou via la Gateway.
 */
@RestController
@RequestMapping("/api/payments")
public class HelloController {

    // TEMPORAIRE atelier : À SUPPRIMER avant l'intégration
    @GetMapping("/hello")
    public String hello() {
        return "payment-service is UP and registered in Eureka!";
    }
}

