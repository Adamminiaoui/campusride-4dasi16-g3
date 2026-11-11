# Backend

Structure des six microservices métier et du service commun de CampusRide. Tous les dossiers sont des emplacements de développement ; aucune application n’est encore exécutable.

| Dossier | Responsabilité | Port prévu | Base MySQL prévue |
| --- | --- | --- | --- |
| [user-service](user-service/) | Comptes, authentification et rôles | 8081 | user_db |
| [vehicle-driver-service](vehicle-driver-service/) | Véhicules et validation des conducteurs | 8082 | vehicle_db |
| [ride-service](ride-service/) | Trajets et places disponibles | 8083 | ride_db |
| [booking-service](booking-service/) | Réservations et annulations | 8084 | booking_db |
| [payment-service](payment-service/) | Paiements simulés et remboursements | 8085 | payment_db |
| [review-service](review-service/) | Avis et réputation | 8086 | review_db |
| [notification-service](notification-service/) | Notifications communes | 8087 | notification_db |

Chaque service possède les dossiers `controller`, `service`, `repository`, `entity`, `dto`, `client`, `security` et `exception`, ainsi que des emplacements pour les ressources et les tests.

Technologies prévues : Java 17, Spring Boot 3, Maven, Spring Security, JWT et MySQL 8. Chaque microservice disposera de sa propre base de données. Les communications prévues utilisent REST.