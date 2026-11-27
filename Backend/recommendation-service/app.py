"""Recommendation service; demo data is enabled explicitly with DEMO_MODE=true."""
import asyncio
import os
from contextlib import asynccontextmanager
from datetime import date, datetime, time, timezone
from typing import Annotated

import httpx
import jwt
from fastapi import Depends, FastAPI, Header, HTTPException, Query, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, FileResponse
from pydantic import BaseModel, Field, ValidationError
from py_eureka_client.eureka_client import EurekaClient

DEMO = os.getenv("DEMO_MODE", "false").lower() == "true"
PORT = int(os.getenv("PORT", "8087"))
EUREKA = os.getenv("EUREKA_SERVER_URL", "http://localhost:8761/eureka/")
SECRET = os.getenv("JWT_SECRET", "")
NEUTRAL = 2.5


class Ride(BaseModel):
    # Format provisoire : le DTO Ride n'existe pas encore dans le depot.
    id: int
    fromCityId: int
    toCityId: int
    departureTime: datetime
    price: float = Field(ge=0)
    availableSeats: int = Field(ge=0)
    driverId: int
    status: str
    fromCity: str | None = None
    toCity: str | None = None


class Reputation(BaseModel):
    userId: int
    averageRating: float = Field(ge=0, le=5)
    reviewCount: int = Field(ge=0)


@asynccontextmanager
async def lifespan(app: FastAPI):
    if not DEMO and len(SECRET) < 32:
        raise RuntimeError("JWT_SECRET doit contenir au moins 32 caracteres")
    app.state.http = httpx.AsyncClient(timeout=httpx.Timeout(3.0, connect=2.0), trust_env=False)
    app.state.eureka = EurekaClient(eureka_server=EUREKA, app_name="recommendation-service", instance_port=PORT,
                                    instance_ip=os.getenv("INSTANCE_IP", "127.0.0.1"))
    try:
        await app.state.eureka.start()
        yield
    finally:
        await app.state.eureka.stop()
        await app.state.http.aclose()


app = FastAPI(title="CampusRide Recommendation Service", lifespan=lifespan)


@app.get("/demo", include_in_schema=False)
async def demo_page():
    if not DEMO:
        raise HTTPException(404, "Page introuvable")
    return FileResponse(os.path.join(os.path.dirname(__file__), "demo.html"))


def error(status: int, message: str):
    from http import HTTPStatus
    return JSONResponse(status_code=status, content={"status": status, "error": HTTPStatus(status).phrase,
        "message": message, "timestamp": datetime.now(timezone.utc).isoformat()})


@app.exception_handler(HTTPException)
async def http_error(request: Request, exc: HTTPException):
    return error(exc.status_code, str(exc.detail))


@app.exception_handler(RequestValidationError)
async def validation_error(request: Request, exc: RequestValidationError):
    return error(400, "Parametres invalides : " + "; ".join(".".join(map(str, x["loc"])) + ": " + x["msg"] for x in exc.errors()))


def authenticate(authorization: Annotated[str | None, Header()] = None):
    if DEMO:
        return None
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Jeton Bearer requis")
    try:
        jwt.decode(authorization[7:], SECRET, algorithms=["HS256"], options={"require": ["sub", "exp"]})
    except jwt.InvalidTokenError:
        raise HTTPException(401, "Jeton invalide ou expire")
    return authorization


async def request_service(request: Request, name: str, path: str, params=None, authorization=None):
    async def call(url):
        response = await request.app.state.http.get(url, params=params, headers={"Authorization": authorization} if authorization else {})
        response.raise_for_status()
        return response.json()
    return await request.app.state.eureka.walk_nodes(name, path, walker=call)


def score(ride: Ride, preferred: time, budget: float, reputation: float):
    actual = ride.departureTime.hour * 60 + ride.departureTime.minute
    target = preferred.hour * 60 + preferred.minute
    time_score = max(0.0, 1.0 - abs(actual - target) / 180.0)
    price_score = max(0.0, 1.0 - ride.price / budget) if budget > 0 else 1.0
    rating_score = max(0.0, min(1.0, reputation / 5.0))
    return round(100 * (0.4 * time_score + 0.35 * price_score + 0.25 * rating_score), 2)


@app.get("/api/recommendations")
async def recommendations(request: Request, fromCityId: Annotated[int, Query(gt=0)],
    toCityId: Annotated[int, Query(gt=0)], date: date,
    preferredTime: time, maxPrice: Annotated[float, Query(ge=0, allow_inf_nan=False)],
    minSeats: Annotated[int, Query(gt=0)], authorization: str | None = Depends(authenticate)):
    if fromCityId == toCityId:
        raise HTTPException(400, "Les villes de depart et d'arrivee doivent etre differentes")
    if DEMO:
        rides = [Ride(id=i, fromCityId=fromCityId, toCityId=toCityId,
            departureTime=datetime.combine(date, time(hour)), price=price, availableSeats=seats,
            driverId=driver, status="PUBLISHED", fromCity="Départ", toCity="Destination")
            for i, hour, price, seats, driver in [(1, 8, 12, 3, 11), (2, 9, 8, 2, 12), (3, 11, 16, 1, 13)]]
    else:
        try:
            data = await request_service(request, "ride-service", "/api/rides", {"fromCityId": fromCityId,
                "toCityId": toCityId, "date": date.isoformat(), "maxPrice": maxPrice, "minSeats": minSeats}, authorization)
            # Pagination et noms des champs Ride restent a confirmer avec l'equipe.
            items = data if isinstance(data, list) else data["content"]
            rides = [Ride.model_validate(item) for item in items]
        except (Exception, ValidationError) as exc:
            raise HTTPException(503, "Ride Service indisponible ou reponse incompatible") from exc

    candidates = [r for r in rides if r.fromCityId == fromCityId and r.toCityId == toCityId
        and r.departureTime.date() == date and r.status == "PUBLISHED" and r.price <= maxPrice and r.availableSeats >= minSeats]

    async def reputation(driver_id):
        if DEMO:
            return {11: 4.8, 12: 3.5, 13: 5.0}.get(driver_id, NEUTRAL), False
        try:
            data = await request_service(request, "review-service", f"/api/reviews/reputation/{driver_id}", authorization=authorization)
            result = Reputation.model_validate(data)
            if result.userId != driver_id or result.reviewCount == 0:
                return NEUTRAL, True
            return result.averageRating, False
        except Exception:
            return NEUTRAL, True

    ratings = await asyncio.gather(*(reputation(driver) for driver in dict.fromkeys(r.driverId for r in candidates)))
    by_driver = dict(zip(dict.fromkeys(r.driverId for r in candidates), ratings))
    result = []
    for ride in candidates:
        rating, neutral = by_driver[ride.driverId]
        result.append({**ride.model_dump(mode="json"), "driverRating": rating, "ratingNeutral": neutral,
            "score": score(ride, preferredTime, maxPrice, rating),
            "explanation": f"Horaire {ride.departureTime:%H:%M}, prix {ride.price:.2f}, note " + ("neutre" if neutral else f"{rating:.1f}/5")})
    return sorted(result, key=lambda item: (-item["score"], item["id"]))[:10]
