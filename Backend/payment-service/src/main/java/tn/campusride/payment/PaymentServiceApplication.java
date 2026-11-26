package tn.campusride.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Point d'entrée du payment-service CampusRide.
 *
 * @EnableDiscoveryClient : facultatif avec Spring Cloud récent (l'auto-configuration
 * suffit dès que spring-cloud-starter-netflix-eureka-client est dans le classpath),
 * mais conservé ici pour la lisibilité — le prof et les membres d'équipe voient
 * immédiatement que ce service est un client de découverte.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

