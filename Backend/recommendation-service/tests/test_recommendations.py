from datetime import datetime, time
from fastapi.testclient import TestClient
from app import app, Ride, score


def test_score_boundaries():
    ride = Ride(id=1, fromCityId=1, toCityId=2, departureTime=datetime(2026, 12, 1, 8), price=0,
                availableSeats=1, driverId=1, status="PUBLISHED")
    assert score(ride, time(8), 20, 5) == 100
    assert score(ride.model_copy(update={"price": 20}), time(12), 20, 0) == 0


def test_demo_filters_and_orders(monkeypatch):
    monkeypatch.setattr("app.DEMO", True)
    client = TestClient(app)
    response = client.get("/api/recommendations", params={"fromCityId":1,"toCityId":2,"date":"2026-12-01",
        "preferredTime":"08:00","maxPrice":15,"minSeats":2})
    assert response.status_code == 200
    data = response.json()
    assert {x["id"] for x in data} == {1, 2}
    assert data[0]["score"] >= data[1]["score"]


def test_auth_and_invalid_inputs(monkeypatch):
    monkeypatch.setattr("app.DEMO", False)
    client = TestClient(app)
    params = {"fromCityId":1,"toCityId":2,"date":"2026-12-01","preferredTime":"08:00","maxPrice":15,"minSeats":2}
    assert client.get("/api/recommendations", params=params).status_code == 401
    monkeypatch.setattr("app.DEMO", True)
    params["minSeats"] = 0
    response = client.get("/api/recommendations", params=params)
    assert response.status_code == 400
    assert set(response.json()) == {"status", "error", "message", "timestamp"}
