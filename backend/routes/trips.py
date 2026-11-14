from pathlib import Path
import os
from tempfile import NamedTemporaryFile
from typing import Literal
from uuid import UUID
from pydantic import BaseModel, Field
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


class RecordingSegment(BaseModel):
    consent: Literal[True]
    started_at_ms: int = Field(ge=0)
    duration_ms: int = Field(gt=0, le=10000)
    status: Literal["pending", "ok", "empty", "error"]
    transcript: str | None = Field(default=None, max_length=5000)
    error: str | None = Field(default=None, max_length=200)


@router.put("/{trip_id}/recordings/{session_id}/{segment}")
def save_recording_segment(trip_id: UUID, session_id: UUID, segment: int, body: RecordingSegment):
    if segment < 1:
        raise HTTPException(status_code=422, detail="invalid_segment")
    db = store()
    try:
        db.get_trip(str(trip_id))
    except KeyError as error:
        failure(error)
    folder = Path(db.path).parent / "recordings" / str(trip_id) / str(session_id)
    folder.mkdir(parents=True, exist_ok=True)
    target = folder / f"{segment:06d}.json"
    with NamedTemporaryFile(mode="w", encoding="utf-8", dir=folder, delete=False) as output:
        temporary = Path(output.name)
        output.write(body.model_dump_json(indent=2))
    try:
        temporary.replace(target)
    finally:
        temporary.unlink(missing_ok=True)
    return {"status": "saved"}
