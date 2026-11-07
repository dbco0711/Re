from datetime import datetime
from typing import Literal
from pydantic import BaseModel, Field, field_validator

class TripCreate(BaseModel):
    client_trip_id: str = Field(min_length=1, max_length=100)
    source: Literal["replay", "real_gps"]
    started_at: datetime

    @field_validator("started_at")
    @classmethod
    def require_utc(cls, value):
        if value.utcoffset() is None or value.utcoffset().total_seconds() != 0:
            raise ValueError("UTC required")
        return value

class GPSPoint(BaseModel):
    seq: int = Field(ge=0)
    elapsed_ms: int = Field(ge=0)
    lat: float = Field(ge=-90, le=90, allow_inf_nan=False)
    lon: float = Field(ge=-180, le=180, allow_inf_nan=False)
    accuracy_m: float | None = Field(default=None, ge=0, allow_inf_nan=False)

class PointsBatch(BaseModel):
    points: list[GPSPoint] = Field(min_length=1, max_length=1000)
