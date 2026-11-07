from datetime import datetime
from typing import Literal
from pydantic import BaseModel, Field, field_validator, model_validator

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


class MemoCreate(BaseModel):
    client_memo_id: str = Field(min_length=1, max_length=100)
    consent: bool = Field(strict=True)
    start_elapsed_ms: int = Field(ge=0)
    end_elapsed_ms: int = Field(ge=0)
    processing_source: Literal["fixture", "manual"] = "fixture"
    transcript: str | None = Field(default=None, max_length=5000)

    @model_validator(mode="after")
    def validate_memo(self):
        if not self.consent:
            raise ValueError("explicit_consent_required")
        if not 0 < self.end_elapsed_ms-self.start_elapsed_ms <= 30000:
            raise ValueError("duration_must_be_1_to_30000_ms")
        if self.processing_source == "manual" and not (self.transcript or "").strip():
            raise ValueError("manual_transcript_required")
        return self


class MemorySearch(BaseModel):
    current_trip_id: str = Field(min_length=1)
    limit: int = Field(default=5, ge=1, le=20)

class MemoUpdate(BaseModel):
    summary: str = Field(min_length=1, max_length=300)

class AIResult(BaseModel):
    processing_source: Literal["fixture", "manual", "actual_api"]
    transcript: str = Field(max_length=5000)
    summary: str = Field(min_length=1, max_length=300)
    travel_purpose: str | None = Field(default=None, max_length=100)
    keywords: list[str] = Field(default_factory=list, max_length=20)
    provider: str | None = Field(default=None, max_length=100)
    model: str | None = Field(default=None, max_length=100)

    @model_validator(mode="after")
    def provenance(self):
        if self.processing_source == "actual_api" and not ((self.provider or "").strip() and (self.model or "").strip()):
            raise ValueError("actual_api_provider_model_required")
        if any(not k.strip() or len(k)>100 for k in self.keywords):
            raise ValueError("invalid_keyword")
        return self
