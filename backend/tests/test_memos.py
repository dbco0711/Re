import unittest
import tempfile
from pathlib import Path
from backend.models import MemoCreate
from backend.database.store import Store

class MemoTests(unittest.TestCase):
    def test_memo_lifecycle(self):
        with tempfile.TemporaryDirectory() as temp:
            db=Store(Path(temp)/"test.db")
            trip=db.create_trip(dict(client_trip_id="t",source="replay",started_at="2026-11-07T00:00:00Z"))[0]["trip_id"]
            points=[dict(seq=i,elapsed_ms=i*10000,lat=37,lon=127) for i in range(4)]
            db.add_points(trip,points)
            body=dict(client_memo_id="m",consent=True,start_elapsed_ms=5000,end_elapsed_ms=15000)
            memo,created=db.create_memo(trip,body)
            self.assertTrue(created)
            self.assertEqual(memo["recorded_at"],"2026-11-07T00:00:05Z")
            self.assertEqual(memo["linked_seqs"],[0,1,2])
            self.assertEqual(db.create_memo(trip,body),(memo,False))
            with self.assertRaises(ValueError):db.create_memo(trip,dict(body,end_elapsed_ms=20000))
            self.assertEqual(db.get_memo(memo["memo_id"]),memo)
            db.delete_memo(memo["memo_id"])
            db.delete_memo(memo["memo_id"])
            with self.assertRaises(KeyError):db.get_memo(memo["memo_id"])
            memo,_=db.create_memo(trip,body)
            with self.assertRaises(ValueError):db.create_memo(trip,dict(body,client_memo_id="outside",end_elapsed_ms=31000))
            db.revoke_consent(trip)
            db.revoke_consent(trip)
            with self.assertRaises(KeyError):db.get_memo(memo["memo_id"])
            with self.assertRaises(ValueError):db.create_memo(trip,body)
            with self.assertRaises(ValueError):db.add_points(trip,points)
            self.assertEqual(db.get_trip(trip)["points"],[])

    def test_consent_and_duration(self):
        body=dict(client_memo_id="m",consent=True,start_elapsed_ms=0,end_elapsed_ms=10000)
        for change in [dict(consent=False),dict(consent="true"),dict(end_elapsed_ms=0),dict(end_elapsed_ms=30001),dict(processing_source="manual")]:
            with self.assertRaises(ValueError):MemoCreate(**dict(body,**change))
