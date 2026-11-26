# CampusRide Review Service

**Responsable : Amine Bel Haj.** Avis, signalements et réputation. Port **8086**, base **review_db**, nom de service **review-service**.

## Présentation locale avec IntelliJ

1. Démarrer l'instance MariaDB locale habituelle contenant `review_db`.
2. Ouvrir le projet Maven avec le JDK 17. Sélectionner **CampusRide Review Demo** dans les configurations Run, puis **Run ▶️**. La configuration partagée se trouve dans `.run/CampusRide Review Demo.run.xml` à la racine.
3. Autre possibilité : conserver la configuration `ReviewServiceApplication` existante avec l'option VM `-Dspring.profiles.active=demo`, utilisée lors de la validation.
4. Ouvrir **[http://localhost:8086/reviews.html](http://localhost:8086/reviews.html)**.

La page unique contient son HTML, CSS et JavaScript, sans bibliothèque externe. Elle envoie de vraies requêtes REST et les données sont persistées dans MariaDB. Choisir par exemple un trajet `10`, un utilisateur évalué `5`, une note et un commentaire. Le profil existant utilise l'auteur local **1** : ne pas évaluer l'utilisateur 1. Un seul avis est autorisé par auteur/trajet/destinataire.

Après création, le tableau et la réputation sont actualisés. **Modifier** relit l'avis, charge le formulaire et enregistre par PUT ; seuls note et commentaire changent. **Supprimer** demande confirmation, appelle DELETE, puis recalcule l'affichage. **Consulter les avis** charge les avis visibles et la réputation de l'utilisateur saisi. Les erreurs HTTP sont affichées ; les commentaires sont insérés comme texte, jamais comme HTML.

Le profil `demo` utilise l'identité et le client Booking locaux déjà présents. Il n'active pas Keycloak et ne s'enregistre pas dans Eureka. Il est **réservé à cette démonstration locale** : le serveur est forcé sur `127.0.0.1`, même si `server.address` est surchargé. Un filtre rejette les adresses clientes non locales, les hôtes étrangers et les requêtes navigateur d'origine différente. L'administration reste fermée. Ne pas placer cette démonstration derrière un proxy ou un tunnel réseau.

Sans `demo`, la sécurité JWT et le vrai client Booking sont utilisés. La page de démonstration n'est pas rendue publique par la configuration normale. Aucun secret ou jeton n'est stocké dans la page.

## Architecture et versions

```text
controller -> service / service.impl -> repository -> review_db
                    |
                    +-> client.BookingClient
config : JWT, identité, CORS et profil demo
dto : requêtes validées et réponse de réputation
entity : Review, Report, Reputation et statuts
exception : erreurs REST communes
static/reviews.html : interface locale
```

Injection par constructeur ; transactions métier ; aucune relation JPA vers les bases des autres services.

| Technologie | Version validée |
| --- | --- |
| Java cible / JDK exécuté | 17 / Temurin 17.0.21.1 |
| Spring Boot / Cloud | 3.5.16 / 2025.0.3 |
| JUnit Jupiter / Mockito / Byte Buddy | 5.12.2 / 5.17.0 / 1.17.8 |
| Maven / Surefire | 3.9.16 / 3.5.4 |
| MariaDB locale / pilote JDBC | 10.4.32 / 3.5.8 |
| H2, tests uniquement | 2.3.232 |

Les dépendances sont gérées par les BOM Spring Boot/Cloud. Le cache Caffeine évite l'avertissement du cache LoadBalancer par défaut. Aucun contrat RabbitMQ ou client Feign n'existe dans les fichiers inspectés.

## Configuration normale

| Variable | Rôle / défaut |
| --- | --- |
| `DB_URL` | `jdbc:mariadb://localhost:3306/review_db` ; utiliser une URL `jdbc:mysql:` pour MySQL, dont le pilote est aussi fourni |
| `DB_USER`, `DB_PASSWORD` | `root`, vide : valeurs locales existantes ; fournir le compte du service pour un environnement partagé |
| `DB_DDL_AUTO` | `validate` : aucun changement automatique du schéma |
| `EUREKA_ENABLED`, `EUREKA_URL` | `false`, vide ; activer avec l'adresse réelle complète |
| `JWT_ISSUER_URI`, `JWT_AUDIENCE` | Vides ; issuer OIDC et audience à confirmer avec l'équipe |
| `JWT_USER_ID_CLAIM` | `sub`, conformément au rapport historique |
| `FRONTEND_ORIGIN` | `http://localhost:5500` |
| `BOOKING_URL`, `INTERNAL_API_KEY` | Adresse et clé de l'API interne Booking |
| `BOOKING_ELIGIBILITY_POINTER` | `UNCONFIGURED`, à remplacer après confirmation du JSON Booking |

Le profil demo conserve les paramètres MariaDB locaux de `application-demo.properties` et `ddl-auto=validate`. Il ne crée ni n'efface la base. Spring Boot ne lit pas automatiquement les fichiers `.env`.

La sécurité normale valide signature, issuer, audience et expiration. Sans issuer ou audience utilisable, aucun jeton n'est accepté. Les rôles proviennent de `roles` ou `realm_access.roles`. Le claim d'identité doit être un entier positif ; un UUID Keycloak n'est pas converti arbitrairement. Si `sub` est un UUID, un mapper vers l'identifiant numérique CampusRide et `JWT_USER_ID_CLAIM` sont nécessaires. L'ancien JWT HS256 décrit dans le rapport v3 n'est pas validé par ce décodeur OIDC à clés publiques : le contrat d'équipe reste à confirmer.

Les écritures exigent un JWT ; les modifications et suppressions exigent en plus l'auteur réel de l'avis. CSRF est désactivé pour l'API stateless à bearer explicite, sans désactiver l'authentification normale. Le profil demo constitue une exception locale explicite et isolée.

## API

| Méthode et route | Accès normal | Codes principaux |
| --- | --- | --- |
| `POST /api/reviews` | Authentifié | 201, 400, 401, 403, 409, 503 |
| `GET /api/reviews/{id}` | Authentifié ; avis masqué accessible à son auteur | 200, 401, 404 |
| `GET /api/reviews/user/{userId}` | Public, avis visibles | 200, 400 |
| `PUT /api/reviews/{id}` | Auteur | 200, 400, 401, 403, 404, 409 |
| `DELETE /api/reviews/{id}` | Auteur | 204, 401, 403, 404 |
| `GET /api/reviews/reputation/{userId}` | Public | 200, 400 |
| `POST /api/reviews/{id}/report` | Authentifié | 201, 400, 401, 404, 409 |
| `GET /api/admin/reports?status=OPEN` | ADMIN | 200, 400, 401, 403 |
| `PUT /api/admin/reports/{id}` | ADMIN | 200, 400, 401, 403, 404, 409 |

Les routes de consultation unitaire, modification et suppression présentes dans les fichiers lors de la demande d'interface sont utilisées sans duplication. Les autres routes correspondent au [dossier technique v3](../../Documentation/CampusRide_Rapport_technique_v3.docx).

Création :

```json
{"rideId":10,"targetUserId":5,"rating":4,"comment":"Trajet agréable"}
```

Exemple de réponse (identifiant et date illustratifs) :

```json
{"id":1,"rideId":10,"authorId":1,"targetUserId":5,"rating":4,"comment":"Trajet agréable","status":"VISIBLE","createdAt":"2026-11-26T18:00:00"}
```

Modification : `{"rating":3,"comment":"Avis modifié"}`. Signalement : `{"reason":"Commentaire inapproprié"}`. Modération : `{"status":"ACCEPTED"}` ou `{"status":"DISMISSED"}`. Réputation : `{"userId":5,"averageRating":3.00,"reviewCount":1}`.

Les erreurs métier suivent `{status,error,message,timestamp}`. Les réponses de sécurité peuvent avoir un corps vide. Exemple : `{"status":404,"error":"Not Found","message":"Avis introuvable","timestamp":"2026-11-26T17:00:00Z"}`.

## Base, règles et intégrations

`reviews` impose l'unicité `(ride_id,author_id,target_user_id)`, une note de 1 à 5 et un commentaire de 500 caractères maximum. `reports` impose l'unicité `(review_id,reporter_id)`, une clé étrangère locale et une version optimiste. `reputations` contient une ligne par utilisateur.

La réputation est calculée depuis les avis visibles, arrondie à deux décimales `HALF_UP`, et vaut zéro sans avis. Les écritures actualisent aussi la réputation stockée dans la même transaction. Un upsert MySQL/MariaDB et un verrou par destinataire protègent la première création concurrente ; les mutations utilisent `READ_COMMITTED`. Les contraintes uniques restent la protection finale contre les doublons.

Une modération suit `OPEN -> ACCEPTED` (masquage et recalcul) ou `OPEN -> DISMISSED` (état conservé). Une décision déjà traitée ne peut pas être rejouée. Modifier un avis masqué ne le rend pas visible. Supprimer un avis enlève ses signalements associés et recalcule la réputation.

Pour une base existante, inspecter et sauvegarder avant migration : notamment `reports.version`, la clé étrangère et la contrainte de note. Aucune migration destructive n'a été exécutée. Le schéma MariaDB présent a permis le démarrage avec `validate`. H2 `create-drop` est exclusivement utilisé par les tests isolés. Les lignes historiques de réputation ne font pas l'objet d'un recalcul global au démarrage.

En mode normal, Booking est appelé via `GET /internal/bookings/eligibility?rideId=&authorId=&targetId=` avec `X-Internal-Key`. Connexion limitée à 2 s, lecture à 3 s. Le module Booking ne fournit aucun DTO dans cette branche : le corps réel reste inconnu. Un pointeur explicite est requis (`/eligible` uniquement si l'équipe confirme `{"eligible":true}`, `$` si elle confirme un booléen racine). Sans configuration valide, erreurs réseau ou corps invalide : **503 sans écriture** ; éligibilité refusée : 403.

Eureka local indiqué par l'utilisateur : `http://localhost:8761/eureka/`. Aucune adresse distante, dont `10.95.213.175`, n'était configurée dans le dépôt. Les services User et Matching & Recommendation n'offrent aucun contrat exploitable ici. **La cible actuelle est Matching & Recommendation, pas Notification** : aucun appel ou événement Notification n'est réintroduit. Les références dans le README racine, le rapport v3, les diagrammes et l'agrégateur sont historiques ; les fichiers partagés n'ont pas été modifiés.

## Commandes et validation

Depuis la racine, avec Maven installé (aucun wrapper dans ce dépôt) :

```powershell
mvn -pl Backend/review-service -am clean verify
java -jar Backend/review-service/target/review-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo
```

Pour tout le dépôt : `mvn clean verify`. Pour les cinq tests historiques : `mvn -f Backend/review-service/pom.xml -Dtest=ReportServiceImplTest test`.

Maven local utilisé : `C:\Users\laptop\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd`. La résolution TLS a nécessité, uniquement dans le processus Maven, `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE`. Aucune vérification TLS ni configuration globale Windows n'a été désactivée.

Mockito utilise `mock-maker-subclass` dans les ressources de test. Le moteur inline ajoutait son dispatcher au bootstrap et déclenchait l'avertissement OpenJDK CDS. Les mocks utilisés ne nécessitent ni classes finales ni méthodes statiques : le changement supprime l'instrumentation inutile sans `-Xshare:off` ni suppression de tests. Revoir ce choix si de tels mocks deviennent nécessaires.

Voir le [rapport de validation](docs/rapport-validation.md) pour les résultats réels, la liste complète des fichiers et les limites. Références techniques : [Mockito inline](https://github.com/mockito/mockito/blob/main/mockito-core/src/main/java/org/mockito/internal/creation/bytebuddy/InlineDelegateByteBuddyMockMaker.java), [compatibilité Cloud](https://spring.io/projects/spring-cloud/), [Java et Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
