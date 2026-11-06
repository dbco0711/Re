"""Local connection check only; no GPS, audio, database or AI processing."""
from fastapi import FastAPI

app = FastAPI(title="Re — 길 위의 기억", version="0.1.0")


@app.get("/health")
def health():
    return {"status": "ok", "service": "re-backend"}


@app.get("/v1/demo-card")
def demo_card():
    return {
        "source": "fixture",
        "processing_source": "fixture",
        "is_demo": True,
        "title": "길 위의 기억 — 연결 확인",
        "summary": "화면 연결을 확인하기 위한 가상 카드입니다.",
        "recorded_at": "2026-01-01T00:00:00Z",
        "trip_id": "fixture-trip-001",
        "notice": "실제 녹음·GPS·AI 처리 결과가 아닙니다.",
    }
