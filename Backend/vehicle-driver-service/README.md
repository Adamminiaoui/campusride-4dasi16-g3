# Vehicle & Driver Service

Le service implémente le CRUD de l'entité `Vehicle` (table `vehicles`) : `driverId`, `brand`, `model`, `plate`, `seats`, `color` et `status` (`PENDING`, `VERIFIED`, `REJECTED`). La plaque est unique. Les autres tables prévues pour ce microservice ne sont pas encore implémentées.

## Démarrer en local

Depuis ce dossier, avec Java 17 et Maven :

```bash
mvn org.springframework.boot:spring-boot-maven-plugin:4.0.10:run
```

Le profil par défaut utilise une base H2 en mémoire sur le port `8082`. Pour démarrer avec la configuration MySQL déjà fournie dans `application-local.properties`, démarrez avec `--spring.profiles.active=mysql` et définissez `VEHICLE_DB_URL`, `VEHICLE_DB_USERNAME` et `VEHICLE_DB_PASSWORD` selon votre installation.

## Endpoints

| Méthode | URL | Résultat |
| --- | --- | --- |
| POST | `/api/vehicles` | Créer, HTTP 201 |
| GET | `/api/vehicles` | Lister, HTTP 200 |
| GET | `/api/vehicles/{id}` | Lire un véhicule, HTTP 200 ou 404 |
| PUT | `/api/vehicles/{id}` | Remplacer les champs, HTTP 200 ou 404 |
| DELETE | `/api/vehicles/{id}` | Supprimer, HTTP 204 ou 404 |

Exemple de corps JSON pour POST :

```json
{
  "driverId": 1,
  "brand": "Toyota",
  "model": "Yaris",
  "plate": "123 TUN 4567",
  "seats": 5,
  "color": "Blanche"
}
```

Le statut initial est `PENDING`. Un doublon de plaque renvoie HTTP 409 et une requête invalide renvoie HTTP 400.

## Essais Postman

Importer `postman/Vehicle-CRUD.postman_collection.json` dans Postman, démarrer le service puis exécuter les six requêtes dans l'ordre. La collection vérifie la création, la liste, la lecture, la modification, la suppression et le HTTP 404 attendu après suppression. Elle enregistre automatiquement l'identifiant créé dans la variable `vehicleId`.
