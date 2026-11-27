# Booking Service

**Responsable : Adam Miniaoui.** Java 17, Spring Boot 4.0.10, Spring Cloud 2025.1.5. Port **8084**, nom Eureka **booking-service**.

Le profil `crud` propose une interface locale pour les tables `bookings` et `booking_status_history` dans `booking_db`. JWT, Gateway, Ride, Payment, Notification et Cancellation restent a integrer.

Le parcours passager autonome est accessible sur http://localhost:8084/crud avec le profil `crud`. Il utilise des trajets et passagers fictifs selectionnes par nom. Une reservation cree un statut PENDING et son historique ; le prix est calcule depuis le trajet fictif. La page technique CRUD des deux tables reste sur `/crud/tables`.

Le passager de test peut modifier les places d'une reservation PENDING ou supprimer celle-ci avec son historique. La suppression physique sert uniquement a cet atelier ; l'annulation et le remboursement du vrai parcours restent a implementer. Modifier les places recalcule le prix mais ne change pas le statut ni l'historique.

Ce test ne simule ni paiement, ni annulation, ni decompte partage des places : le nombre de places indique est seulement la capacite de chaque trajet fictif. L'authentification JWT n'est pas appliquee. Les verifications de doublon utilisent les donnees locales et ne constituent pas une garantie contre les requetes simultanees. Le profil `crud` n'a pas besoin d'Eureka ; definir `BOOKING_EUREKA_ENABLED=true` si vous souhaitez l'enregistrer pendant le test.

## Lancement simple

1. Demarrer MySQL (ou le module MySQL de XAMPP) sur 3306, et creer `booking_db` dans phpMyAdmin si elle n'existe pas.
2. Copier `application-local.properties.example` vers `application-local.properties` dans ce dossier et renseigner la connexion. Le fichier local est ignore par Git. Le compte cible est `booking_svc`, limite a cette base.
3. Depuis la racine du depot :

```bash
mvn -pl Backend/eureka-server,Backend/booking-service -am verify
```

4. Demarrer Eureka, puis Booking dans deux terminaux :

```bash
java -jar Backend/eureka-server/target/eureka-server-0.1.0-SNAPSHOT.jar
```

```bash
java -jar Backend/booking-service/target/booking-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=crud
```

5. Ouvrir **http://localhost:8084/crud**. Dans phpMyAdmin, ouvrir `booking_db` : JPA cree les deux tables au demarrage (`ddl-auto=update`). Les donnees persistent apres redemarrage.

Sur ce poste, le module MySQL de XAMPP utilise MariaDB 10.4.32. Les verifications HTTP locales utilisent ce serveur et le pilote MySQL ; elles ne constituent pas une validation sur MySQL 8. La configuration cible reste une base MySQL booking_db.

Dans IntelliJ : choisir `BookingApplication`, puis **Run > Edit Configurations > Active profiles : crud** ; ou ajouter `--spring.profiles.active=crud` dans **Program arguments**. Arreter la copie deja lancee sur 8084 avant Run.

Sans fichier local, la connexion utilise `BOOKING_DB_URL`, `BOOKING_DB_USERNAME`, `BOOKING_DB_PASSWORD` (defaut : localhost:3306/booking_db, booking_svc, mot de passe vide). Ne pas versionner les secrets. Le script facultatif `scripts/Initialize-BookingDatabase.ps1` prepare un compte dedie sur un serveur neuf et ne remplace pas un compte existant. Pour cette demonstration XAMPP, une connexion locale peut etre utilisee ; root ne convient pas a l'integration d'equipe.

## Utilisation

- **Reservations** : creer, lister/rechercher, modifier et supprimer. Exemple : trajet 101, conducteur 2, passager 7, places 2, prix total 24.50, statut PENDING, auteur 7.
- **Historique des statuts** : choisir une reservation, consulter, ajouter, modifier ou supprimer une entree.
- La creation ajoute une ligne initiale (`previousStatus=null`). Chaque changement de statut ajoute une ligne dans la **meme transaction** ; modifier d'autres champs n'ajoute pas de ligne.
- Supprimer Booking supprime son historique. Une entree n'est accessible que sous sa propre reservation.
- Dates UTC ISO 8601 sans suffixe de fuseau (`LocalDateTime`). Les dates de Booking sont generees ; une date facultative peut etre saisie pour l'historique.

Champs Booking : `id`, `rideId`, `driverId`, `passengerId`, `seats`, `totalPrice` (BigDecimal), `status`, `createdAt`.
Champs historique : `id`, `bookingId`, `previousStatus`, `newStatus`, `changedBy`, `changedAt`.
Identifiants Long ; statuts PENDING, CONFIRMED, CANCELLED, COMPLETED.

## Contrat du CRUD local

Ces endpoints ne sont actifs qu'avec `crud`, sans login, sur **127.0.0.1** par defaut. L'interface est servie par Booking et utilise des URL relatives ; aucun serveur frontend ni CORS supplementaire n'est necessaire.

| Methode | Endpoint | Resultat |
| --- | --- | --- |
| GET | `/dev/bookings` | Liste par ID decroissant (200) |
| POST | `/dev/bookings` | Creation avec historique initial (201 + Location) |
| GET | `/dev/bookings/{id}` | Detail (200) |
| PUT | `/dev/bookings/{id}` | Modification et trace du changement de statut (200) |
| DELETE | `/dev/bookings/{id}` | Suppression physique avec historique (204) |
| GET | `/dev/bookings/{bookingId}/history` | Historique par date puis ID croissants (200) |
| POST | `/dev/bookings/{bookingId}/history` | Ajout manuel (201 + Location) |
| GET | `/dev/bookings/{bookingId}/history/{historyId}` | Detail (200) |
| PUT | `/dev/bookings/{bookingId}/history/{historyId}` | Modification manuelle (200) |
| DELETE | `/dev/bookings/{bookingId}/history/{historyId}` | Suppression d'une entree (204) |

Corps POST/PUT Booking :

```json
{"rideId":101,"driverId":2,"passengerId":7,"seats":2,"totalPrice":24.50,"status":"PENDING","changedBy":7}
```

Corps POST/PUT historique :

```json
{"previousStatus":"PENDING","newStatus":"CONFIRMED","changedBy":7,"changedAt":null}
```

`changedAt` absent/null utilise l'heure serveur ; `previousStatus` absent/null represente une creation. Les deux statuts doivent differer. Identifiants positifs, places >= 1, prix >= 0 avec deux decimales maximum ; conducteur et passager differents. Erreurs 400 (entree invalide), 404 (introuvable), 409 (conflit SQL), au format `{status,error,message,timestamp}`.

L'auteur et le prix sont saisis par le testeur. Les modifications manuelles de l'historique ne changent pas Booking. Cet atelier accepte les quatre statuts sans validation du cycle metier, ni disponibilite, paiement, remboursement ou unicite des reservations actives. Le futur workflow utilisera le JWT et Ride pour l'identite et le prix. L'edition de l'historique et la suppression physique sont reservees a la demonstration ; le contrat public `/api/bookings/**` du rapport, ou DELETE signifie annuler, reste distinct.

## Verification

```bash
mvn -pl Backend/booking-service -am verify
npm exec --yes --package=newman -- newman run Backend/booking-service/api/booking-crud.postman_collection.json
```

Six tests JPA utilisent H2 uniquement en scope test : persistance/trace, CRUD historique, isolation par reservation, suppression associee, rollback si l'historique echoue, transition identique rejetee. H2 n'est pas inclus dans le JAR executable.
La collection Postman couvre 20 requetes sur le vrai service et nettoie ses donnees a la fin. Elle peut aussi etre importee et lancee dans Postman.

## Eureka seul

Sans profil, Booking demarre comme avant sans base :

```bash
java -jar Backend/booking-service/target/booking-service-0.1.0-SNAPSHOT.jar
```

Le dashboard http://localhost:8761 affiche BOOKING-SERVICE, UP. `PORT` et `EUREKA_SERVER_URL` permettent de changer le port ou le serveur. Deuxieme instance de demonstration : ajouter `--server.port=8094`.

La configuration reste locale jusqu'a l'integration du Config Server. Le futur frontend de l'equipe utilisera la Gateway.
