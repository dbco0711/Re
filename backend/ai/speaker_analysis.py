"""A 오태경 담당 검토용: 명시적인 화자별 전사구간 분석.

기준 소스: Re-261111_yuchae_b.zip, Re-codex-261111_minsung_c-v2.zip.
작성 보조: ChatGPT (2026-11-11). B/C 코드 변경 없음.
주의: 음성에서 화자를 탐지하거나 실명/운전자 여부를 추정하지 않음.
"""
from __future__ import annotations

from typing import Any, Mapping, Sequence

from backend.ai.analyzer import analyze_memo


def analyze_speaker_transcript(
    memo_id: str,
    speaker_segments: Sequence[Mapping[str, Any]],
    *,
    speaker_labels: Mapping[str, str] | None = None,
    user_keywords: list[str] | str | None = None,
    situation: str | None = None,
) -> dict[str, Any]:
    """C가 *실제로 구분해 전달한* 구간만 분석. A의 기존 결과 키를 유지한다.

    segment: {speaker_id: str, text: str, start_elapsed_ms: int, end_elapsed_ms: int}
    speaker_id='unknown'은 화자가 식별되지 않았음을 뜻함.
    speaker_labels는 사용자가 명시적으로 지정한 표시 이름이며 신원 인증이 아님.
    """
    if isinstance(speaker_segments, (str, bytes)) or not isinstance(speaker_segments, (list, tuple)):
        raise TypeError("speaker_segments must be a list or tuple")
    if not speaker_segments:
        raise ValueError("speaker_segments required")
    if len(speaker_segments) > 100:
        raise ValueError("too many speaker segments")
    if speaker_labels is not None and not isinstance(speaker_labels, dict):
        raise TypeError("speaker_labels must be a dictionary")
    labels = speaker_labels or {}
    if any(not isinstance(k, str) or not k.strip() or not isinstance(v, str) or not v.strip() or len(v) > 100 for k, v in labels.items()):
        raise ValueError("invalid speaker label")
    cleaned = []
    ids = set()
    last_start = -1
    for segment in speaker_segments:
        if not isinstance(segment, Mapping):
            raise TypeError("each speaker segment must be a mapping")
        sid, value = segment.get("speaker_id"), segment.get("text")
        start, end = segment.get("start_elapsed_ms"), segment.get("end_elapsed_ms")
        if not isinstance(sid, str) or not sid.strip() or len(sid) > 100:
            raise ValueError("invalid speaker_id")
        if not isinstance(value, str) or not value.strip():
            raise ValueError("segment text required")
        if len(value) > 5000:
            raise ValueError("segment text too long")
        if type(start) is not int or type(end) is not int or start < 0 or end <= start:
            raise ValueError("invalid segment time")
        if start < last_start:
            raise ValueError("segments must be ordered by start time")
        sid = sid.strip()
        ids.add(sid)
        last_start = start
        cleaned.append({"speaker_id": sid, "text": value, "start_elapsed_ms": start, "end_elapsed_ms": end})
    if set(labels) - ids:
        raise ValueError("label refers to unknown speaker")
    transcript = " ".join(s["text"].strip() for s in cleaned)
    if len(transcript) > 5000:
        raise ValueError("transcript exceeds existing B limit")
    result = analyze_memo(memo_id, transcript, user_keywords=user_keywords, situation=situation)
    result["speaker_segments"] = cleaned
    result["speaker_count"] = len(ids - {"unknown"})
    result["speaker_labels"] = dict(labels)
    result["speaker_source"] = "provided_segments"
    return result
