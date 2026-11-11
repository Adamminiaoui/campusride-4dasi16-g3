# Payment Service

**Responsable : Aziz Soltani.**

Paiement simulé, remboursements et historique des transactions.

Ce dossier initialise uniquement la structure du futur microservice. Il ne contient aucune implémentation ni configuration de lancement.

Le fichier `pom.xml` initialise ce service comme module Maven JAR, avec Java 17 hérité du parent. Depuis la racine du dépôt : `mvn -pl Backend/payment-service -am clean verify`. Les dépendances Spring Boot seront ajoutées lors de l’implémentation.

- Technologies prévues : Java 17, Spring Boot 3, Maven et MySQL 8.
- Port prévu : 8085.
- Base MySQL prévue : `payment_db`.
- Sources Java : `src/main/java/tn/campusride/payment/`.
- Ressources : `src/main/resources/`.
- Tests futurs : `src/test/java/tn/campusride/payment/`.

La conception est décrite dans le [dossier technique](../../Documentation/CampusRide_Rapport_technique_v3.docx).
