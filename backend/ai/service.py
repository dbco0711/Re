"""A 담당: 실제 API 연결 전 fixture/manual 어댑터."""
import json
from pathlib import Path

def analyze_fixture(memo_id: str) -> dict:
    if not memo_id.strip():
        raise ValueError("memo_id required")
    result = json.loads((Path(__file__).resolve().parents[2] / "shared/examples/ai-result.json").read_text(encoding="utf-8"))
    result["memo_id"] = memo_id
    return result

def analyze_manual(memo_id: str, transcript: str) -> dict:
    if not memo_id.strip() or not transcript.strip():
        raise ValueError("memo_id and transcript required")
    return dict(memo_id=memo_id, transcript=transcript, processing_source="manual",
                summary=None, travel_purpose=None, keywords=[], provider=None,
                model=None, status="not_analyzed")
