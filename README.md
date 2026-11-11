# CampusRide

Plateforme de covoiturage étudiant — **classe 4DASi16, groupe 3**.

CampusRide vise à mettre en relation des étudiants conducteurs et passagers pour publier des trajets, réserver des places, simuler des paiements et partager des avis.

## État du projet

Ce dépôt initialise uniquement la structure du projet et rassemble les documents de conception. Les dossiers de développement sont prêts à être complétés ; aucune logique métier, application exécutable ou configuration de base de données n’est encore implémentée. Il n’y a donc pas de commande de compilation ou de lancement à cette étape.

## Organisation du dépôt

```text
campusride-4dasi16-g3/
├── Backend/
│   ├── user-service/
│   ├── vehicle-driver-service/
│   ├── ride-service/
│   ├── booking-service/
│   ├── payment-service/
│   ├── review-service/
│   └── notification-service/
├── Frontend/
│   ├── pages/
│   ├── css/
│   ├── js/pages/
│   └── assets/images/
├── Documentation/
│   ├── Presentation/
│   │   └── CampusRide_Presentation_Seance4_FR.pptx
│   ├── Diagrams/
│   │   ├── Diagrammes-de-classes/ (7 diagrammes)
│   │   ├── 07-architecture-globale.png
│   │   └── ... 12 autres diagrammes et leur index
│   └── CampusRide_Rapport_technique_v3.docx
├── .gitattributes
├── .gitignore
└── README.md
```

Les fichiers `.gitkeep` permettent à Git de conserver les dossiers encore vides.

## Architecture prévue

Le projet comprend six microservices métier et un service commun de notification. Chaque service dispose de sa propre base MySQL et communique par API REST. Le frontend prévu utilise HTML, CSS et JavaScript natif avec Fetch API.

![Architecture globale de CampusRide](Documentation/Diagrams/07-architecture-globale.png)

| Microservice | Responsabilité | Port prévu | Base prévue |
| --- | --- | --- | --- |
| User | Comptes, authentification et rôles | 8081 | user_db |
| Vehicle & Driver | Véhicules et validation des conducteurs | 8082 | vehicle_db |
| Ride | Trajets et places disponibles | 8083 | ride_db |
| Booking | Réservations et annulations | 8084 | booking_db |
| Payment | Paiements simulés et remboursements | 8085 | payment_db |
| Review | Avis et réputation | 8086 | review_db |
| Notification | Notifications dans l’application | 8087 | notification_db |

Technologies backend prévues : Java 17, Spring Boot 3, Maven, Spring Security, BCrypt, JWT et MySQL 8. Le frontend est prévu sur le port local 5500. Ces choix décrivent la cible de développement.

## Documents du projet

- [Présentation de la séance 4](Documentation/Presentation/CampusRide_Presentation_Seance4_FR.pptx).
- [Diagramme d’architecture globale](Documentation/Diagrams/07-architecture-globale.png).
- [Principaux diagrammes et index complet](Documentation/Diagrams/README.md).
- [Dossier technique Word](Documentation/CampusRide_Rapport_technique_v3.docx).

## Équipe et encadrement

Équipe : Yesmine Chourou, Ines Mzoughi, Ahmed Aatrous, Adam Miniaoui, Aziz Soltani et Amine Bel Hadj.

Encadrant : M. Ghassen Hamouda.

## Ouvrir le projet dans IntelliJ IDEA

1. Cloner le dépôt :

   ```bash
   git clone https://github.com/Adamminiaoui/campusride-4dasi16-g3.git
   ```

2. Dans IntelliJ IDEA, choisir **File > Open** et sélectionner le dossier `campusride-4dasi16-g3`.
3. Lors de l’implémentation, initialiser chaque microservice comme projet Spring Boot/Maven dans son dossier existant.

## Collaboration

Créer une branche par fonctionnalité ou microservice, puis proposer une pull request vers `main`. Ne pas versionner les mots de passe, jetons ou fichiers de configuration personnels.

L’accès de l’enseignant se gère dans les paramètres GitHub du dépôt : **Settings > Collaborators**. Une invitation doit être acceptée par son destinataire pour devenir effective.
