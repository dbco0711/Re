"""Re 메모 규칙 기반 분석 시제품.

담당: A 오태경 (AI·음성)
개선안 작성: ChatGPT, A 오태경 검토·적용용 (2026-11-08)
변경: 목적 및 상황 규칙 확장, 직접 입력 지원, 원문 보존.
연동 담당: B 김유채 (메모 API에서 기존 두 인자 사용).
주의: 실제 STT/LLM이 아니며 목적 분류는 추정치이다.
"""

import re
from typing import Any

# 키워드와 목적은 별도 개념: 키워드는 원문에 나타난 주제, 목적은 이동 이유.
# 카테고리별 '행동/이동 의도' 표현만 목적 판별에 사용한다.
_RULES: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("데이트", (r"데이트(?:하러|를\s*하러|\s*가는|\s*중)", r"연인(?:과|이랑|하고)\s*.*(?:만나|보러|놀러)", r"애인(?:과|이랑|하고)\s*.*(?:만나|보러|놀러)", r"남자친구(?:를|랑|와|하고)?\s*(?:만나러|보러)", r"여자친구(?:를|랑|와|하고)?\s*(?:만나러|보러)")),
    ("여행", (r"여행(?:(?:을|도)?\s*(?:가|떠나)|하러|\s*중)", r"관광(?:하러|을\s*가|하러\s*가)", r"휴가(?:를|로)?\s*(?:가|떠나|출발)", r"호캉스(?:하러|를\s*가)")),
    ("학교", (r"등교", r"학교(?:에|로|를)\s*(?:가|가는|가려고|가야|도착)", r"수업(?:을\s*들으러|에\s*가|들으러)", r"강의(?:를\s*들으러|에\s*가)")),
    ("출근", (r"출근", r"회사(?:에|로)\s*(?:가|가는|가려고|도착)", r"직장(?:에|로)\s*(?:가|가는|가려고)")),
    ("귀가", (r"귀가", r"집(?:에|으로|으론|으로는)\s*(?:가|가는|가려고|돌아)", r"퇴근길", r"집으로\s*복귀")),
    ("친구 만남", (r"(?<!남자)(?<!여자)친구(?:를|랑|와|하고|들이랑)?\s*(?:만나러|보러|보기\s*위해)", r"동창(?:을|들(?:을)?)?\s*(?:만나러|보러)")),
    ("가족 방문", (r"부모님(?:을|\s*댁에)?\s*.*(?:뵈러|보러|방문)", r"가족(?:을|들)?\s*.*(?:만나|보러|방문)", r"본가(?:에|로)\s*(?:가|방문)")),
    ("외식", (r"밥(?:을)?\s*먹으러", r"식사(?:하러|를\s*하러)", r"외식(?:하러|을\s*하러)", r"맛집(?:에|을)\s*(?:가|방문|찾)")),
    ("쇼핑", (r"쇼핑(?:하러|을\s*하러|을\s*가)", r"장(?:을)?\s*보러", r"마트(?:에|로)\s*(?:가|가는|들르)", r"물건\s*사러")),
    ("병원", (r"병원(?:에|으로)\s*(?:가|가는|도착)", r"진료(?:를)?\s*받으러", r"검진(?:을)?\s*받으러")),
    ("운동", (r"헬스장(?:에|으로)\s*(?:가|가는)", r"운동(?:하러|을\s*하러)", r"수영(?:하러|을\s*하러)", r"축구(?:하러|를\s*하러)")),
    ("공항 이동", (r"공항(?:에|으로)\s*(?:가|가는|데려다|마중)", r"비행기\s*타러", r"공항\s*픽업")),
    ("픽업·배웅", (r"(?:데리러|태우러|마중\s*나가|배웅하러|데려다\s*주러)",)),
    ("출장", (r"출장(?:을)?\s*(?:가|가는|중)", r"거래처(?:에|로)\s*(?:가|방문)", r"업무\s*보러")),
    ("카페", (r"카페(?:에|로)\s*(?:가|들르|갈)", r"커피(?:를)?\s*마시러")),
    ("캠핑", (r"캠핑(?:하러|(?:을)?\s*가|\s*중)",)),
    ("산책", (r"산책(?:하러|을\s*하러|가러)",)),
    ("드라이브", (r"드라이브(?:하러|를\s*하러|\s*중|\s*가|\s*갈)",)),
)

_TOPIC_RULES: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("교통체증", (r"교통\s*체증|(?:도로|교통|차량)(?:가|이)?\s*정체|(?:차가|길이|도로가)\s*(?:\S+\s+){0,2}막히|차가\s*많",)),
    ("날씨", (r"날씨|맑(?:아|고|음|다)|흐리|폭염|(?<!\w)(?:비|눈)(?:가|이)?\s*(?:오|와|내리|내려)",)),
    ("비", (r"(?<!\w)(?:비|빗방울)(?:가|를)?\s*(?:오|와|내리|내려|맞|그치|멈추)",)),
    ("눈", (r"(?<!\w)눈(?:이|을)?\s*(?:오|와|내리|내려|맞|그치|쌓)",)),
    ("기념일", (r"기념일|\d+\s*주년",)),
    ("약속", (r"약속",)),
    ("주차", (r"주차|주차장",)),
    ("길 찾기", (r"길을\s*잃|길\s*찾|내비|네비|경로",)),
    ("음악", (r"음악|노래|플레이리스트",)),
    ("휴게소", (r"휴게소",)),
    ("충전", (r"충전",)),
    ("주유", (r"주유소|기름\s*넣|주유",)),
)

# 앞 목적을 제외하는 표현은 앞 절에 남겨 뒤 목적에 영향을 주지 않는다.
_SPLIT = re.compile(r"[.!?。,\n]+|(?<=말고)\s*|\s+(?:하지만|그런데|근데|대신)\s+")
_NEGATION = re.compile(
    r"안\s*(?:가|하|만나)|못\s*(?:가|하|만나)|"
    r"(?:가지|하지|만나지)\s*(?:않|못)|아니|취소|포기|말고"
)


def analyze_memo(
    memo_id: str,
    transcript: str,
    user_keywords: list[str] | str | None = None,
    situation: str | None = None,
) -> dict[str, Any]:
    """전사문 + 선택적인 사용자의 키워드·상황으로 규칙 기반 결과 생성.

    이전 2-인수 호출 그대로 호환됨. user_keywords는 리스트 또는 쉼표 구분 문자열.
    situation은 사용자 직접 입력 상황 설명(전사 원문은 변경하지 않음).
    실제 STT/LLM 결과가 아니라 규칙 기반 미리보기이다.
    """
    if not isinstance(memo_id, str) or not isinstance(transcript, str):
        raise TypeError("memo_id and transcript must be strings")
    memo_id = memo_id.strip()
    normalized = " ".join(transcript.split())
    if not memo_id:
        raise ValueError("memo_id required")
    if not normalized:
        raise ValueError("transcript required")
    if situation is not None and not isinstance(situation, str):
        raise TypeError("situation must be a string or None")
    situation = " ".join((situation or "").split())
    if len(situation) > 500:
        raise ValueError("situation too long (max 500 characters)")
    if user_keywords is None:
        user_keywords = []
    elif isinstance(user_keywords, str):
        user_keywords = user_keywords.split(",")
    elif not isinstance(user_keywords, list) or any(not isinstance(k, str) for k in user_keywords):
        raise TypeError("user_keywords must be a list of strings or comma-separated string")
    cleaned_keywords = []
    for item in user_keywords:
        item = item.strip()
        if not item:
            continue
        if len(item) > 100:
            raise ValueError("user keyword too long (max 100 characters)")
        if item not in cleaned_keywords:
            cleaned_keywords.append(item)
    if len(cleaned_keywords) > 20:
        raise ValueError("too many user keywords (max 20)")

    # 사용자가 입력한 상황도 분석 근거로 사용하되, 전사문 자체는 유지한다.
    analysis_text = transcript + (". " + situation if situation else "")
    found_purposes: list[str] = []
    for sentence in _SPLIT.split(analysis_text):
        if _NEGATION.search(sentence):
            continue
        for label, expressions in _RULES:
            for expression in expressions:
                if re.search(expression, sentence, re.IGNORECASE):
                    if label not in found_purposes:
                        found_purposes.append(label)
                    break

    # 서로 다른 목적을 여러 개 찾으면 하나를 임의 선택하지 않는다.
    purpose = found_purposes[0] if len(found_purposes) == 1 else None
    # 키워드는 목적과 동일시하지 않고 발견된 이동 주제 + 주변 상황을 중복 없이 기록한다.
    keywords = list(found_purposes)
    for label, expressions in _TOPIC_RULES:
        if any(re.search(expr, analysis_text, re.IGNORECASE) for expr in expressions):
            if label not in keywords:
                keywords.append(label)
    # 사용자가 명시한 키워드를 먼저 보존하며, 추출된 키워드는 뒤에 추가한다.
    keywords = cleaned_keywords + [k for k in keywords if k not in cleaned_keywords]
    keywords = keywords[:20]  # B의 AIResult 모델과 동일한 최대 20개
    summary = normalized if len(normalized) <= 80 else normalized[:79].rstrip() + "…"
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
