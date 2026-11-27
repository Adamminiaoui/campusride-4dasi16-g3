# Frontend

Emplacement de la future application cliente CampusRide, prévue en HTML5, CSS3 et JavaScript natif avec Fetch API.

| Dossier | Contenu à développer |
| --- | --- |
| `pages/` | Pages HTML de l’application |
| `css/` | Feuilles de style |
| `js/` | Configuration et modules JavaScript communs |
| `js/pages/` | Scripts propres à chaque page |
| `assets/images/` | Images et ressources graphiques |

La future page d'accueil pourra etre placee dans `Frontend/index.html`. Le port local envisage dans le rapport est 5500.

Une interface de test Booking est disponible sur **http://localhost:8084/crud**, avec le profil `crud`. Ses fichiers se trouvent dans `Backend/booking-service/src/main/resources/static/booking-crud/` et sont servis par Booking ; aucun serveur frontend supplementaire n'est necessaire. Voir le README du Booking Service pour le lancement et MySQL.

Cette page sert uniquement au CRUD local des reservations et de leur historique. Le futur frontend CampusRide utilisera la Gateway et l'authentification selon le rapport.
