from pathlib import Path
import os
from fastapi import APIRouter, HTTPException, Response
from backend.models import TripCreate, PointsBatch
from backend.database.store import Store

router = APIRouter(prefix="/v1/trips", tags=["trip-starter"])

def store():
    return Store(os.environ.get("RE_DB_PATH", str(Path(__file__).resolve().parents[2] / "private/re.db")))

def failure(error):
    raise HTTPException(status_code=404 if isinstance(error, KeyError) else 409,
                        detail="trip_not_found" if isinstance(error, KeyError) else str(error))

@router.post("", status_code=201)
def create_trip(body: TripCreate, response: Response):
    try:
        result, created = store().create_trip(body.model_dump(mode="json"))
        response.status_code = 201 if created else 200
        return result
    except ValueError as error:
        failure(error)

@router.get("/{trip_id}")
def get_trip(trip_id: str):
    try:
        return store().get_trip(trip_id)
    except KeyError as error:
        failure(error)

@router.post("/{trip_id}/points")
def add_points(trip_id: str, body: PointsBatch):
    try:
        return store().add_points(trip_id,[p.model_dump() for p in body.points])
    except (ValueError, KeyError) as error:
        failure(error)
