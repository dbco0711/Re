from pathlib import Path
import tempfile
import unittest
from backend.database.store import Store
from backend.ai.service import analyze_fixture, analyze_manual
from backend.matching.service import compare_routes
from backend.models import GPSPoint, TripCreate

class StarterTests(unittest.TestCase):
    def test_atomic_storage_and_retry(self):
        with tempfile.TemporaryDirectory() as temp:
            store = Store(Path(temp)/"test.db")
            payload = dict(client_trip_id="test",source="replay",started_at="2026-01-01T00:00:00Z")
            trip, created = store.create_trip(payload)
            self.assertTrue(created)
            self.assertEqual(store.create_trip(payload),(trip,False))
            with self.assertRaises(ValueError): store.create_trip(dict(payload,source="real_gps"))
            p = dict(seq=0,elapsed_ms=0,lat=37.0,lon=127.0,accuracy_m=5.0)
            store.add_points(trip["trip_id"],[p])
            self.assertEqual(store.add_points(trip["trip_id"],[p])["inserted_count"],0)
            with self.assertRaises(ValueError):
                store.add_points(trip["trip_id"],[dict(p,seq=1,elapsed_ms=10),dict(p,seq=3,elapsed_ms=20)])
            self.assertEqual(len(store.get_trip(trip["trip_id"])["points"]),1)

    def test_provenance_and_matching_guard(self):
        self.assertEqual(analyze_fixture("m")["processing_source"],"fixture")
        self.assertIsNone(analyze_manual("m","text")["summary"])
        with self.assertRaises(ValueError): analyze_manual("m","")
        self.assertFalse(compare_routes("a","a",[1,2],[1,2])["matched"])
        self.assertIsNone(compare_routes("a","b",[1,2],[1,2])["matched"])

    def test_validation(self):
        with self.assertRaises(ValueError): GPSPoint(seq=0,elapsed_ms=0,lat=91,lon=0)
        with self.assertRaises(ValueError): TripCreate(client_trip_id="x",source="replay",started_at="2026-01-01T00:00:00")
