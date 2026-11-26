# payment-service

## Rôle
Service de paiement de la plateforme CampusRide.  
Gère les transactions de paiement des réservations de covoiturage.  
**Membre 5** — Base de données : MySQL (`payment_db`).

## Nom Eureka
`payment-service`  
*(visible en majuscules dans le dashboard : `PAYMENT-SERVICE`)*

## Port par défaut
`8085`

## Prérequis
| Outil | Version minimale |
|---|---|
| Java | 17 |
| Maven | 3.6+ (ou utiliser `mvnw.cmd` embarqué) |
| Eureka Server | démarré sur `localhost:8761` |

> [!NOTE]
> Pour l'atelier Eureka, le Config Server (Membre 3) et la base MySQL ne sont
> pas encore nécessaires. Les dépendances JPA et MySQL sont absentes du `pom.xml`
> à ce stade.

## Lancement

### Instance unique (port 8085)
```bash
# Depuis le dossier payment-service/
mvn spring-boot:run
```

### Deux instances simultanées (démonstration multi-instances Eureka)
Ouvre deux terminaux :

**Terminal 1 — instance 1 (port 8085)**
```bash
mvn spring-boot:run
```

**Terminal 2 — instance 2 (port 8095)**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8095
```

Les deux instances apparaissent dans le dashboard Eureka sous `PAYMENT-SERVICE`.

## Vérification

```bash
# Endpoint hello (TEMPORAIRE atelier — à supprimer avant intégration)
curl http://localhost:8085/api/payments/hello
curl http://localhost:8095/api/payments/hello

# Health check Actuator
curl http://localhost:8085/actuator/health

# Vérification dans le registre Eureka (JSON)
curl -H "Accept: application/json" http://localhost:8761/eureka/apps/PAYMENT-SERVICE
```

**Résultat attendu dashboard Eureka :**  
`PAYMENT-SERVICE` — 2 instances UP (payment-service:8085 et payment-service:8095)

## Configuration temporaire

> [!IMPORTANT]
> Le fichier `src/main/resources/application.properties` contient une configuration
> **temporaire pour l'atelier Eureka**. Lorsque le Config Server (Membre 3) sera
> disponible, ce fichier devra être réduit à :
> ```properties
> spring.application.name=payment-service
> spring.config.import=configserver:http://localhost:8888
> ```
> Toutes les autres propriétés (port, Eureka, base de données) migreront vers
> `config-repo/payment-service.properties` géré par Membre 3.

## Comportement Eureka — ce qu'il faut savoir

- **Heartbeat** : toutes les 30 secondes, le service envoie un signal « je suis vivant » à Eureka.
- **Expiration** : si Eureka ne reçoit pas de heartbeat pendant **90 secondes**, il retire l'instance du registre.
- Lorsque tu arrêtes une instance avec `Ctrl+C`, Spring Boot envoie un signal d'arrêt propre et **désenregistre immédiatement** l'instance (pas besoin d'attendre 90 s).
- En mode `eureka.server.enable-self-preservation=false` (non activé ici), Eureka retire les instances mortes plus vite, mais ce n'est pas recommandé en production.

## Structure du projet

```
payment-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/tn/campusride/payment/
    │   │   ├── PaymentServiceApplication.java   # Point d'entrée Spring Boot
    │   │   ├── controller/
    │   │   │   └── HelloController.java         # TEMPORAIRE atelier
    │   │   ├── service/                         # (vide — intégration future)
    │   │   ├── repository/                      # (vide — intégration future)
    │   │   ├── entity/                          # (vide — intégration future)
    │   │   ├── dto/                             # (vide — intégration future)
    │   │   ├── client/                          # (vide — intégration future)
    │   │   ├── security/                        # (vide — intégration future)
    │   │   └── exception/                       # (vide — intégration future)
    │   └── resources/
    │       └── application.properties           # TEMPORAIRE — voir note ci-dessus
    └── test/
        └── java/tn/campusride/payment/
            └── PaymentServiceApplicationTests.java
```

## Capture du dashboard Eureka

> [!TIP]
> Fais une capture du dashboard après avoir lancé 2 instances, puis place l'image ici :

![Dashboard Eureka — PAYMENT-SERVICE](docs/eureka-dashboard.png)

*(emplacement réservé — capture à effectuer par le Membre 5)*

## Équipe CampusRide

| Membre | Rôle |
|---|---|
| [NOMS DES 6 MEMBRES À COMPLÉTER] | |
| Membre 1 | user-service |
| Membre 2 | Eureka Server + vehicle-driver-service |
| Membre 3 | Config Server + ride-service |
| Membre 4 | booking-service + review-service |
| **Membre 5** | **payment-service + notification-service** |
| Membre 6 | API Gateway + frontend |

