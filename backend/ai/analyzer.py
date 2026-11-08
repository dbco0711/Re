"""A 담당: 전사 텍스트 기반 규칙 처리 시제품 (실제 STT/LLM 아님)."""

import re
from typing import Any

_PURPOSE_PATTERNS = (
    ("학교", (r"학교", r"등교", r"수업", r"강의")),
    ("출근", (r"출근", r"회사", r"직장")),
    ("귀가", (r"집에\s*가", r"귀가", r"퇴근길")),
    ("친구 만남", (r"친구.*만나", r"친구.*보러")),
    ("산책", (r"산책",)),
    ("드라이브", (r"드라이브",)),
    ("쇼핑", (r"쇼핑", r"장\s*보러", r"마트")),
    ("병원", (r"병원", r"진료")),
)


def analyze_memo(memo_id: str, transcript: str) -> dict[str, Any]:
    """수동 입력한 전사문을 규칙으로 분류한다. 실제 AI 결과로 표시하지 않는다.

    Args:
        memo_id: 비어 있지 않은 메모 식별자.
        transcript: 사람이 입력한 전사 텍스트.
    Returns:
        JSON 직렬화 가능한 임시 분석 결과.
    Raises:
        TypeError: 문자열이 아닌 인수.
        ValueError: 메모 ID 또는 텍스트가 비어 있을 때.
    """
    if not isinstance(memo_id, str) or not isinstance(transcript, str):
        raise TypeError("memo_id and transcript must be strings")
    memo_id = memo_id.strip()
    transcript = " ".join(transcript.split())
    if not memo_id:
        raise ValueError("memo_id required")
    if not transcript:
        raise ValueError("transcript required")

    purpose = None
    for label, patterns in _PURPOSE_PATTERNS:
        if any(re.search(pattern, transcript, flags=re.IGNORECASE) for pattern in patterns):
            purpose = label
            break

    # 일반적인 시제품 예시: 목적이 확인되지 않으면 추측하지 않음.
    summary = transcript if len(transcript) <= 80 else transcript[:79].rstrip() + "…"
    keywords = [purpose] if purpose is not None else []
    return {
        "schema_version": "0.1",
        "memo_id": memo_id,
        "transcript": transcript,
        "summary": summary,
        "travel_purpose": purpose,
        "keywords": keywords,
        "processing_source": "manual",
        "provider": None,
        "model": None,
        "status": "rule_based_preview",
    }
