# Ride Service

**Responsable : Ahmed Aatrous.** Java 17, Spring Boot 4.0.10, Spring Cloud 2025.1.5. Port **8083**, nom Eureka **ride-service**, base **ride_db** (compte `ride_svc`).

- Sans profil : le service demarre sans base et s'enregistre sur Eureka.
- Profil `crud` : en plus, CRUD local de la table de reference `cities` (id, name, governorate) et interface sur **http://localhost:8083/crud**.

## Lancer le CRUD des villes

1. Demarrer MySQL (XAMPP) sur 3306. Dans phpMyAdmin > SQL :
   ```sql
   CREATE DATABASE IF NOT EXISTS ride_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER IF NOT EXISTS 'ride_svc'@'localhost' IDENTIFIED BY 'VotreMotDePasse';
   GRANT ALL PRIVILEGES ON ride_db.* TO 'ride_svc'@'localhost';
   ```
2. Copier `application-local.properties.example` en `application-local.properties` (meme dossier) et mettre le mot de passe. Ce fichier est ignore par Git.
3. IntelliJ : Run > Edit Configurations > RideApplication > Active profiles : `crud`. Lancer.
4. Ouvrir http://localhost:8083/crud. Les 24 villes de `data.sql` sont chargees au premier demarrage (INSERT IGNORE : pas de doublon au redemarrage).

## Endpoints du CRUD local (profil crud uniquement, sans JWT)

| Methode | Endpoint | Resultat |
| --- | --- | --- |
| GET | `/dev/cities` | Liste triee par nom (200) |
| GET | `/dev/cities/{id}` | Detail (200, 404) |
| POST | `/dev/cities` | Creation (201 + Location, 400, 409 nom deja pris) |
| PUT | `/dev/cities/{id}` | Modification (200, 400, 404, 409) |
| DELETE | `/dev/cities/{id}` | Suppression (204, 404) |

Corps : `{"name":"Hammamet","governorate":"Nabeul"}`. Erreurs au format `{status,error,message,timestamp}`.

Le contrat public `GET /api/cities` (via la Gateway) reste a implementer separement. Les noms de champs JSON `name` / `governorate` et la liste des villes sont A CONFIRMER AVEC L'EQUIPE.

## Eureka

`EUREKA_SERVER_URL` change le serveur ; `PORT` change le port. Si le dashboard affiche une mauvaise IP : `--eureka.instance.ip-address=<IP LAN>`.
