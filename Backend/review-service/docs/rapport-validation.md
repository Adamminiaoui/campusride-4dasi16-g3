# Rapport de validation CampusRide Review Service

Date : 26 novembre 2026, Europe/Berlin. Dépôt : `C:\Users\laptop\campusride-4dasi16-g3`. Branche : `feature/review-service`.

## Résultat

La page `http://localhost:8086/reviews.html` a été servie par le service relancé depuis **IntelliJ Run**, avec `-Dspring.profiles.active=demo`. Les quatre opérations CRUD ont été réalisées par les boutons de la page et vérifiées sur la **vraie MariaDB 10.4.32**. La réputation a suivi les mutations. Le port 8086 écoute sur **127.0.0.1 uniquement**.

Le dernier `mvn -pl Backend/review-service -am verify` a exécuté **51 tests : 0 échec, 0 erreur, 0 ignoré** et produit le JAR Spring Boot. Le profil normal reste protégé par JWT ; le profil demo réutilise les composants locaux existants et isole l'accès au loopback.

## Diagnostic initial et évolution

| Priorité | Constat initial | Résolution / limite |
| --- | --- | --- |
| Haute | Création, modération et sécurité absentes ; seulement deux GET publics | Services complétés, JWT, client Booking et routes documentées ajoutés pendant l'audit |
| Haute | Contrats et paramètres Keycloak/Booking inconnus | Aucune configuration supposée ; mode normal refuse les jetons non configurés et renvoie 503 si Booking ne peut être vérifié |
| Haute | CRUD de présentation et profil demo ajoutés aux fichiers avant la demande d'interface | Ajouts conservés ; page branchée sur leurs routes réelles, protection locale renforcée |
| Moyenne | Réputation non matérialisée et risque de concurrence | Mise à jour transactionnelle, verrouillage, contraintes, tests concurrents |
| Moyenne | Avertissement OpenJDK aux tests | Reproduit puis supprimé par le moteur Mockito subclass |
| Moyenne | README obsolète et références à Notification | README du service corrigé ; références historiques partagées signalées, aucun autre microservice modifié |
| Environnement | Maven absent du PATH et confiance TLS Java insuffisante | Distribution Maven existante utilisée explicitement ; magasin Windows limité au processus |

Le dépôt était propre au début de l'audit. Des modifications utilisateur sont apparues pendant la session, notamment le profil demo, les routes CRUD et la journalisation des exceptions. Elles ont été conservées. La liste Git finale comprend donc le travail de l'audit et ces ajouts utilisateur, pas seulement la page web.

## Cause et correction OpenJDK

Commande initiale : `mvn -pl Backend/review-service -am test -B -ntp` avec Maven 3.9.16 et Java 17.0.21.1. Après résolution du problème TLS, **11 tests passaient**, dont les **5 de ReportServiceImplTest**, avec :

```text
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

Mockito 5.17.0 sélectionnait le moteur inline ; Byte Buddy 1.17.8 participait à l'instrumentation. Le code Mockito appelle `appendToBootstrapClassLoaderSearch` pour son dispatcher. Un agent chargé explicitement ne supprime pas cet ajout au bootstrap. Les mocks du projet n'exigent pas les capacités inline.

Correction : `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` contient `mock-maker-subclass`. La même commande a ensuite exécuté **11 tests réussis sans cet avertissement**. Aucun `-Xshare:off`, aucune désactivation CDS, aucun test supprimé. Les builds suivants ne présentent plus l'avertissement.

## Commandes réellement exécutées

Les commandes Maven ci-dessous utilisent le chemin complet de la distribution, présenté dans le README. Les options TLS ont été définies dans le processus PowerShell seulement.

```powershell
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
$mvn = 'C:\Users\laptop\.m2\wrapper\dists\apache-maven-3.9.16-bin\5grr65jo27hi51sujmtcldfovl\apache-maven-3.9.16\bin\mvn.cmd'
& $mvn -version
& $mvn -pl Backend/review-service -am test -B -ntp
& $mvn -pl Backend/review-service -am verify -B -ntp
& $mvn clean verify -B -ntp
```

Résultats successifs : 11 tests initiaux ; 11 après correction Mockito ; 32 puis 41 après extension ; **46 et succès des neuf modules Maven** lors du `clean verify` global ; **51** après intégration du profil demo et de la page. Les autres microservices sont encore des structures sans tests : leur succès de build ne valide pas leur fonctionnement métier.

Dernière répartition :

| Classe | Tests réussis |
| --- | ---: |
| ReviewApplicationTest | 26 |
| DemoApplicationTest | 4 |
| RestBookingClientTest | 6 |
| SecurityConfigTest | 3 |
| ReportServiceImplTest | 5 |
| ReputationServiceImplTest | 4 |
| ReviewServiceImplTest | 3 |
| Total | 51 |

Couverture : création valide/invalide, auto-évaluation, inéligibilité, panne Booking, doublons métier et SQL, consultation existante/inexistante, propriété des modifications/suppressions, recalcul et arrondi, cas sans avis, signalements, transitions et masquage, clé étrangère, concurrence et rollback, 400/401/403/404/409/503, signature JWT altérée, expiration, issuer/audience, UUID refusé et claim numérique configurable, CORS, démarrage HTTP réel, CRUD demo, adresses/origines étrangères refusées. Les tests automatiques utilisent exclusivement H2 en mémoire ou des serveurs HTTP de test locaux, jamais la MariaDB existante.

Preuves locales non versionnées : `target/final-verify.log`, `target/demo-verify.log`, `target/surefire-reports/`. Le dernier ajustement CSS (`min-width: 0` sur les cartes) a ensuite été vérifié sur la page servie ; il ne change aucune logique Java.

Après cet ajustement CSS, `mvn -f Backend/review-service/pom.xml jar:jar spring-boot:repackage -B -ntp` a réussi et actualisé le JAR sans relancer les tests Java inchangés. Le contenu de `BOOT-INF/classes/static/reviews.html` correspond exactement au fichier source final. La fenêtre étroite ne présente plus de débordement horizontal global ; le tableau reste défilable dans sa carte. Les contrôles `git diff --check` et `git diff --cached --check` ont tous deux terminé avec le code 0.

## Vérification manuelle MariaDB et navigateur

Inspection en lecture seule avec `C:\xampp\mysql\bin\mysql.exe`, hôte `127.0.0.1`, compte local existant. `SELECT VERSION()` a retourné `10.4.32-MariaDB`. Le schéma `review_db` contient `reviews`, `reports`, `reputations`, dont `reports.version`. Avant les tests de page : 0 avis et 0 signalement.

1. **Create** : formulaire de la page, trajet `2026112601`, destinataire de test `900000261126`, note 5. Retour de l'avis **#2**, auteur demo 1 ; moyenne affichée 5,00, compteur 1. Le commentaire contenant des balises a été rendu littéralement, sans interprétation HTML.
2. **Read** : bouton Modifier chargeant `GET /api/reviews/2`, puis bouton Consulter les avis chargeant liste et réputation.
3. **Update** : bouton Enregistrer, note ramenée à 3 et commentaire modifié. Lecture SQL réelle : `id=2`, `ride_id=2026112601`, `author_id=1`, `target_user_id=900000261126`, `rating=3`. La ligne de réputation avait `average_rating=3.00`, `review_count=1`.
4. **Delete** : bouton Supprimer et confirmation, uniquement pour cet avis de test. Message de succès, tableau vide et moyenne 0,00, compteur 0.
5. **Contrôle final SQL** : 0 avis, 0 signalement, 0 avis de test restant ; réputation de la cible de test `0.00 / 0`. La ligne de réputation vide reste, conformément au comportement du service. Aucun DELETE global, DROP, TRUNCATE, migration ou effacement de données préexistantes.

La page retourne HTTP 200. Le navigateur n'a signalé aucune erreur JavaScript pendant le parcours. Une requête POST portant `Origin: https://untrusted.test` a retourné **403**. `Get-NetTCPConnection` a confirmé `127.0.0.1:8086`. La propriété active du processus Java relancé par IntelliJ était `-Dspring.profiles.active=demo`.

## Services externes et limites

| Élément | État réellement vérifié |
| --- | --- |
| MariaDB locale | Accessible dans la deuxième partie de la session ; CRUD réel et démarrage validés |
| MySQL | Pilote disponible ; pas d'instance MySQL distincte testée |
| Eureka local | Essai initial `/eureka/apps` : connexion refusée sur localhost:8761 |
| Eureka distant proposé | `10.95.213.175:8761` : délai de 5 secondes dépassé ; adresse absente des configurations du dépôt |
| Enregistrement Eureka | Non validé ; volontairement désactivé en demo |
| Keycloak réel | Issuer/realm, audience et mapping numérique inconnus ; aucune authentification réelle validée |
| JWT isolés | Signatures RSA réelles et validateurs de production testés avec clés éphémères |
| Booking réel | Module sans implémentation ni DTO ; JSON d'éligibilité inconnu ; client normal testé contre serveur local de test |
| User et Matching & Recommendation | Pas de contrat exploitable ni communication validée |
| Docker | CLI présente, moteur Docker Desktop indisponible au contrôle ; non nécessaire à la démo |

La démo locale est utilisable sans Keycloak ni Booking selon la demande. Cela ne prouve pas l'intégration distribuée en mode normal. Pour celle-ci : confirmer issuer/audience/claim et rôles Keycloak, adresse/clé/JSON Booking, adresse Eureka disponible et contrat Matching si un échange est prévu. Les courses ont été testées sur H2 ; la vérification MariaDB porte sur le CRUD séquentiel, pas sur un test de charge concurrente.

Les erreurs SQL attendues des tests de doublons et de clé étrangère peuvent apparaître dans les logs. Le gestionnaire d'exceptions modifié par l'utilisateur imprime également les piles des erreurs métier attendues. Ces traces ne signifient pas que les tests ont échoué ; le résumé Surefire fait foi. Les informations Hibernate sur JTA absent ne sont pas des erreurs bloquantes. Aucun audit CVE exhaustif n'a été exécuté.

## Livraison Git

Branche `feature/review-service`. Aucun commit, push, reset ni modification de l'index effectués par cette intervention. Les ajouts déjà indexés par l'utilisateur sont conservés. Aucun code d'un autre microservice modifié. Les sorties `target/`, logs et `.idea/` restent exclus ; `.run/CampusRide Review Demo.run.xml` est une configuration partageable sans secret.

Relire le diff, puis créer un commit seulement après autorisation du responsable. Le service lancé depuis IntelliJ reste disponible pour la présentation. La liste complète des fichiers de travail suit ci-dessous.

## Liste complète des fichiers créés ou modifiés

État de travail final : M = modifié ; AM = ajouté puis modifié ; ?? = nouveau non indexé. Chemins relatifs à la racine.

~~~text
 M Backend/review-service/README.md
 M Backend/review-service/pom.xml
AM Backend/review-service/src/main/java/tn/campusride/review/client/DemoBookingClient.java
AM Backend/review-service/src/main/java/tn/campusride/review/config/DemoSecurityConfig.java
 M Backend/review-service/src/main/java/tn/campusride/review/controller/ReviewController.java
 M Backend/review-service/src/main/java/tn/campusride/review/dto/CreateReviewRequest.java
 M Backend/review-service/src/main/java/tn/campusride/review/entity/Report.java
 M Backend/review-service/src/main/java/tn/campusride/review/entity/Review.java
 M Backend/review-service/src/main/java/tn/campusride/review/repository/ReportRepository.java
 M Backend/review-service/src/main/java/tn/campusride/review/repository/ReputationRepository.java
 M Backend/review-service/src/main/java/tn/campusride/review/repository/ReviewRepository.java
 M Backend/review-service/src/main/java/tn/campusride/review/service/ReportService.java
 M Backend/review-service/src/main/java/tn/campusride/review/service/ReviewService.java
 M Backend/review-service/src/main/java/tn/campusride/review/service/impl/ReportServiceImpl.java
 M Backend/review-service/src/main/java/tn/campusride/review/service/impl/ReputationServiceImpl.java
 M Backend/review-service/src/main/java/tn/campusride/review/service/impl/ReviewServiceImpl.java
AM Backend/review-service/src/main/resources/application-demo.properties
 M Backend/review-service/src/main/resources/application.properties
 M Backend/review-service/src/test/java/tn/campusride/review/service/impl/ReportServiceImplTest.java
 M Backend/review-service/src/test/java/tn/campusride/review/service/impl/ReputationServiceImplTest.java
?? ".run/CampusRide Review Demo.run.xml"
?? Backend/review-service/docs/rapport-validation.md
?? Backend/review-service/src/main/java/tn/campusride/review/client/BookingClient.java
?? Backend/review-service/src/main/java/tn/campusride/review/client/RestBookingClient.java
?? Backend/review-service/src/main/java/tn/campusride/review/config/CurrentUser.java
?? Backend/review-service/src/main/java/tn/campusride/review/config/SecurityConfig.java
?? Backend/review-service/src/main/java/tn/campusride/review/controller/ReportController.java
?? Backend/review-service/src/main/java/tn/campusride/review/dto/ModerateReportRequest.java
?? Backend/review-service/src/main/java/tn/campusride/review/dto/UpdateReviewRequest.java
?? Backend/review-service/src/main/java/tn/campusride/review/exception/GlobalExceptionHandler.java
?? Backend/review-service/src/main/java/tn/campusride/review/service/impl/ReputationStore.java
?? Backend/review-service/src/main/resources/static/reviews.html
?? Backend/review-service/src/test/java/tn/campusride/review/DemoApplicationTest.java
?? Backend/review-service/src/test/java/tn/campusride/review/ReviewApplicationTest.java
?? Backend/review-service/src/test/java/tn/campusride/review/client/RestBookingClientTest.java
?? Backend/review-service/src/test/java/tn/campusride/review/config/SecurityConfigTest.java
?? Backend/review-service/src/test/resources/application-test.properties
?? Backend/review-service/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
~~~
