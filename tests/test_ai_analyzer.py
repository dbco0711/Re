import unittest

from backend.ai.analyzer import analyze_memo


class AnalyzeMemoTests(unittest.TestCase):
    def test_school(self):
        result = analyze_memo("memo-001", "오늘은 학교에 가는 길입니다.")
        self.assertEqual(result["travel_purpose"], "학교")
        self.assertEqual(result["keywords"], ["학교"])
        self.assertEqual(result["processing_source"], "manual")
        self.assertEqual(result["status"], "rule_based_preview")

    def test_friend(self):
        self.assertEqual(analyze_memo("m2", "친구를 만나러 갑니다.")["travel_purpose"], "친구 만남")

    def test_unknown_purpose(self):
        result = analyze_memo("m3", "오늘 날씨가 참 좋네요.")
        self.assertIsNone(result["travel_purpose"])
        self.assertEqual(result["keywords"], [])

    def test_empty_text(self):
        with self.assertRaises(ValueError):
            analyze_memo("m4", "  ")

    def test_empty_memo_id(self):
        with self.assertRaises(ValueError):
            analyze_memo(" ", "산책하러 가요")

    def test_invalid_type(self):
        with self.assertRaises(TypeError):
            analyze_memo("m5", None)

    def test_json_serializable(self):
        import json
        json.dumps(analyze_memo("m6", "드라이브 중이에요"), ensure_ascii=False)


if __name__ == "__main__":
    unittest.main()
