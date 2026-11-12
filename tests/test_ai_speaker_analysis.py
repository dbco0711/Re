"""A 오태경 담당 검토용 테스트. ChatGPT 보조 작성 (2026-11-11)."""
import unittest
from backend.ai.speaker_analysis import analyze_speaker_transcript


class SpeakerAnalysisTests(unittest.TestCase):
    def setUp(self):
        self.segments = [
            {"speaker_id": "speaker_1", "text": "이번 주말에 여행 갈까?", "start_elapsed_ms": 0, "end_elapsed_ms": 1000},
            {"speaker_id": "speaker_2", "text": "좋아 제주도 가자.", "start_elapsed_ms": 1200, "end_elapsed_ms": 2100},
        ]

    def test_two_speakers_preserved(self):
        result = analyze_speaker_transcript("memo1", self.segments)
        self.assertEqual(result["speaker_count"], 2)
        self.assertEqual(result["speaker_segments"], self.segments)
        self.assertEqual(result["speaker_labels"], {})
        self.assertEqual(result["processing_source"], "manual")

    def test_explicit_labels_only(self):
        result = analyze_speaker_transcript("memo1", self.segments, speaker_labels={"speaker_1": "운전자", "speaker_2": "동승자"})
        self.assertEqual(result["speaker_labels"]["speaker_2"], "동승자")

    def test_keywords_from_segments(self):
        result = analyze_speaker_transcript("memo1", self.segments, user_keywords=["친구"])
        self.assertIn("친구", result["keywords"])
        self.assertEqual(result["transcript"], "이번 주말에 여행 갈까? 좋아 제주도 가자.")

    def test_unknown_not_identified(self):
        result = analyze_speaker_transcript("memo1", [{"speaker_id":"unknown","text":"안녕하세요", "start_elapsed_ms":0,"end_elapsed_ms":100}])
        self.assertEqual(result["speaker_count"], 0)

    def test_reject_unordered(self):
        wrong = [dict(self.segments[1]), dict(self.segments[0])]
        with self.assertRaises(ValueError):
            analyze_speaker_transcript("memo1", wrong)

    def test_reject_bad_time(self):
        bad = [dict(self.segments[0], end_elapsed_ms=0)]
        with self.assertRaises(ValueError):
            analyze_speaker_transcript("memo1", bad)

    def test_reject_unknown_label(self):
        with self.assertRaises(ValueError):
            analyze_speaker_transcript("memo1", self.segments, speaker_labels={"speaker_3": "사용자"})

    def test_reject_empty_segments(self):
        with self.assertRaises(ValueError):
            analyze_speaker_transcript("memo1", [])


if __name__ == "__main__":
    unittest.main()
