# Recommendation Service

Service FastAPI de recommandation, sans base de donnees, sur le port 8087, enregistre sur le meme Eureka `http://localhost:8761/eureka/`.

## Demonstration locale

Depuis ce dossier, sous PowerShell :

```powershell
python -m venv .venv
.\.venv\Scripts\python -m pip install -r requirements.txt
$env:DEMO_MODE='true'
.\.venv\Scripts\python -m uvicorn app:app --port 8087
```

Demarrer Eureka avant le service. Ouvrir http://localhost:8087/docs et tester GET /api/recommendations avec fromCityId=1, toCityId=2, date=2026-12-01, preferredTime=08:00, maxPrice=15, minSeats=2. DEMO_MODE utilise des trajets fictifs et ne verifie pas le JWT ; reserver ce mode a la demonstration locale.

Interface de demonstration : http://localhost:8087/demo. L'interface utilise uniquement les donnees fictives ; le parcours de reservation n'est pas encore implemente.

## Integration

Sans DEMO_MODE, definir JWT_SECRET (cle commune HS256, 32 caracteres minimum) et EUREKA_SERVER_URL si necessaire. Les services Ride et Review sont resolus par leur nom Eureka. Le jeton est transmis aux deux services. Ride indisponible ou reponse incompatible : 503 ; Review indisponible ou sans avis : note neutre 2.5/5.

Le DTO Ride et la pagination sont **a confirmer avec l'equipe** : l'adaptateur attend provisoirement une liste ou un objet `content` avec id, fromCityId, toCityId, departureTime ISO, price, availableSeats, driverId et status. Il ne faut pas considerer ce format comme un contrat approuve. Le DTO Review est issu de la branche feature/review-service : userId, averageRating, reviewCount.

Score : 40 % horaire (ecart de 0 a 180 minutes), 35 % prix (rapport au budget), 25 % reputation (note sur 5). Filtres : villes, date, statut PUBLISHED, budget et places. Les 10 meilleurs resultats sont tries par score decroissant.

La Gateway, le Config Server et la page de recherche ne sont pas implementes dans ce depot. La route a ajouter lorsque la Gateway existe est `Path=/api/recommendations,/api/recommendations/**` vers `lb://recommendation-service`. Les parametres de configuration du service doivent ensuite etre fournis par le Config Server. Aucun appel direct a une base d'un autre service n'est effectue.

Tests : `.venv\Scripts\python -m pytest -q`. L'API OpenAPI interactive sur `/docs` permet aussi de generer des requetes Postman.
