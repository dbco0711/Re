import unittest
import tempfile
from pathlib import Path
from backend.database.store import Store
from backend.models import AIResult
from backend.tests.test_matching import route

class MemoryTests(unittest.TestCase):
    def test_search_scope_and_revoke(self):
        with tempfile.TemporaryDirectory() as temp:
            db=Store(Path(temp)/"test.db")
            def trip(name,date,source="replay",points=None):
                t=db.create_trip(dict(client_trip_id=name,source=source,started_at=date))[0]["trip_id"]
                db.add_points(t,points or route())
                return t
            past=trip("past","2026-11-01T00:00:00Z")
            memo,_=db.create_memo(past,dict(client_memo_id="m",consent=True,start_elapsed_ms=0,end_elapsed_ms=30000))
            current=trip("now","2026-11-07T00:00:00Z",points=route()[:4])
            departed=trip("departed","2026-11-07T00:00:00Z")
            self.assertEqual(db.search_memories(departed)["cards"],[])
            cards=db.search_memories(current)["cards"]
            self.assertEqual(len(cards),1)
            self.assertNotIn("transcript",cards[0])
            self.assertNotIn("summary",cards[0])
            self.assertEqual(db.search_memories(past)["cards"],[])
            backwards=trip("reverse","2026-11-07T00:00:00Z",points=route(True))
            self.assertEqual(db.search_memories(backwards)["cards"],[])
            real=trip("real","2026-11-07T00:00:00Z",source="real_gps")
            self.assertEqual(db.search_memories(real)["cards"],[])
            updated=db.update_memo(memo["memo_id"],dict(summary="test",summary_source="user_edit"))
            self.assertEqual(updated["summary"],"test")
            db.revoke_consent(past)
            self.assertEqual(db.search_memories(current)["cards"],[])
            with self.assertRaises(KeyError):db.update_memo(memo["memo_id"],dict(summary="late result"))

    def test_analysis_provenance(self):
        with self.assertRaises(ValueError):AIResult(processing_source="actual_api",transcript="t",summary="s")
        result=AIResult(processing_source="fixture",transcript="t",summary="s")
        self.assertEqual(result.processing_source,"fixture")
