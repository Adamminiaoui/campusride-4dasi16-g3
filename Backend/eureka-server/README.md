# Eureka Server

Serveur de decouverte local du TP CampusRide, sur le port `8761`.

La classe `EurekaApplication` active le registre avec `@EnableEurekaServer`. Le serveur ne s'enregistre pas lui-meme et ne recupere pas le registre d'un autre serveur (`register-with-eureka=false`, `fetch-registry=false`).

Depuis la racine du depot :

```bash
mvn -pl Backend/eureka-server -am clean verify
java -jar Backend/eureka-server/target/eureka-server-0.1.0-SNAPSHOT.jar
```

Ouvrir [le dashboard Eureka](http://localhost:8761). Avant de lancer Booking, aucune instance n'est enregistree.

Suivre ensuite les [instructions du Booking Service](../booking-service/README.md) pour enregistrer une ou deux instances.

Ce module fournit le serveur minimal necessaire au TP. Sa configuration reste locale jusqu'a l'integration du Config Server par l'equipe.
