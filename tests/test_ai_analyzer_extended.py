"""A 오태경 담당. ChatGPT 보조 수정 (2026-11-08)."""

import unittest
from backend.ai.analyzer import analyze_memo

class ExtendedMemoAnalysisTests(unittest.TestCase):
    def test_backward_compatible_call(self):
        r = analyze_memo("1", "산책하러 가는 길이에요")
        self.assertEqual((r["travel_purpose"],r["keywords"]), ("산책",["산책"]))
    def test_date_weather_traffic(self):
        r = analyze_memo("2", "데이트하러 가는데 차가 너무 막히고 비가 오네")
        self.assertEqual(r["travel_purpose"], "데이트")
        self.assertEqual(r["keywords"], ["데이트", "교통체증", "날씨", "비"])
    def test_trip(self):
        self.assertEqual(analyze_memo("3","여행 가는 중이야")["travel_purpose"], "여행")
    def test_unknown_purpose_weather(self):
        r=analyze_memo("4", "오늘 날씨가 좋네요")
        self.assertIsNone(r["travel_purpose"])
        self.assertEqual(r["keywords"], ["날씨"])
    def test_user_keywords_list(self):
        r=analyze_memo("5","산책하러 가요",user_keywords=["추억", "반려견", "추억"])
        self.assertEqual(r["keywords"], ["추억", "반려견", "산책"])
    def test_user_keywords_csv(self):
        r=analyze_memo("6","오늘은 날씨가 좋아요",user_keywords="기분 좋음, 노을, 노을")
        self.assertEqual(r["keywords"], ["기분 좋음","노을","날씨"])
    def test_user_situation_sets_purpose(self):
        r=analyze_memo("7","오늘 출발했어",situation="남자친구 만나러 데이트하러 가는 중")
        self.assertEqual(r["travel_purpose"], "데이트")
        self.assertEqual(r["transcript"], "오늘 출발했어")
    def test_situation_topic(self):
        r=analyze_memo("8","산책하러 가요",situation="도로가 정체되고 주차장도 복잡해")
        self.assertIn("주차",r["keywords"])
        self.assertIn("교통체증",r["keywords"])
    def test_not_infer_purpose_from_keyword_only(self):
        r=analyze_memo("9","오늘 날씨 좋네", user_keywords=["데이트"])
        self.assertIsNone(r["travel_purpose"])
        self.assertIn("데이트",r["keywords"])
    def test_conflicting_purposes_unknown(self):
        r=analyze_memo("10","산책하러 가고 여행도 가려고 해")
        self.assertIsNone(r["travel_purpose"])
    def test_empty_transcript_error_even_with_situation(self):
        with self.assertRaises(ValueError): analyze_memo("11","",situation="데이트")
    def test_wrong_keyword_type_error(self):
        with self.assertRaises(TypeError): analyze_memo("12","산책하러 가요",user_keywords=[1])
    def test_too_long_situation_error(self):
        with self.assertRaises(ValueError): analyze_memo("13","산책하러 가요",situation="가"*501)
    def test_api_contract_valid(self):
        import backend.models as models

        if not hasattr(models, "AIResult"):
            self.skipTest("AIResult는 B 백엔드에서 검증합니다.")
        AIResult = models.AIResult
        r=analyze_memo("14","산책하러 가요",user_keywords=["기억"],situation="비가 와")
        AIResult.model_validate(r)

    def test_negation_and_topic_only(self):
        for transcript in ("데이트는 취소했어", "산책하러 가지 않아요", "데이트 사진을 봐요"):
            with self.subTest(transcript=transcript):
                self.assertIsNone(analyze_memo("15", transcript)["travel_purpose"])
        result = analyze_memo("15", "산책하러 가지 않고, 카페에 가요")
        self.assertEqual(result["travel_purpose"], "카페")

    def test_raw_transcript(self):
        transcript = "  오늘은\n날씨가 좋네요.  "
        result = analyze_memo("16", transcript, situation="캠핑하러 가요")
        self.assertEqual(result["transcript"], transcript)
        self.assertEqual(result["summary"], "오늘은 날씨가 좋네요.")
        self.assertEqual(result["travel_purpose"], "캠핑")

    def test_specific_topics(self):
        result = analyze_memo("17", "눈이 내려요. 기념일 약속 전에 휴게소에서 충전해요.")
        self.assertEqual(result["keywords"], ["날씨", "눈", "기념일", "약속", "휴게소", "충전"])
        self.assertEqual(analyze_memo("17", "눈이 아파요")["keywords"], [])


if __name__ == "__main__": unittest.main()
