"""B 담당: 로컬 SQLite trip·GPS 저장. 실제 음성은 저장하지 않음."""
from pathlib import Path
from contextlib import contextmanager
import sqlite3
import json
from uuid import uuid4

class Store:
    def __init__(self, path):
        self.path = Path(path)

    @contextmanager
    def transaction(self):
        self.path.parent.mkdir(parents=True, exist_ok=True)
        db = sqlite3.connect(self.path)
        try:
            db.execute("PRAGMA foreign_keys=ON")
            db.execute("CREATE TABLE IF NOT EXISTS trips(id TEXT PRIMARY KEY, client_id TEXT UNIQUE NOT NULL, payload TEXT NOT NULL)")
            db.execute("CREATE TABLE IF NOT EXISTS points(trip_id TEXT REFERENCES trips(id), seq INTEGER, payload TEXT NOT NULL, PRIMARY KEY(trip_id,seq))")
            yield db
            db.commit()
        except Exception:
            db.rollback()
            raise
        finally:
            db.close()

    def create_trip(self, payload):
        encoded = json.dumps(payload, sort_keys=True)
        with self.transaction() as db:
            row = db.execute("SELECT id,payload FROM trips WHERE client_id=?", (payload["client_trip_id"],)).fetchone()
            if row:
                if row[1] != encoded:
                    raise ValueError("client_trip_id_conflict")
                return dict(trip_id=row[0], **payload), False
            trip_id = str(uuid4())
            db.execute("INSERT INTO trips VALUES(?,?,?)", (trip_id,payload["client_trip_id"],encoded))
        return dict(trip_id=trip_id, **payload), True

    def get_trip(self, trip_id):
        with self.transaction() as db:
            row = db.execute("SELECT payload FROM trips WHERE id=?", (trip_id,)).fetchone()
            if not row:
                raise KeyError(trip_id)
            points = [json.loads(p[0]) for p in db.execute("SELECT payload FROM points WHERE trip_id=? ORDER BY seq", (trip_id,))]
        return dict(trip_id=trip_id, **json.loads(row[0]), points=points)

    def add_points(self, trip_id, points):
        with self.transaction() as db:
            if not db.execute("SELECT 1 FROM trips WHERE id=?", (trip_id,)).fetchone():
                raise KeyError(trip_id)
            saved = {seq:json.loads(payload) for seq,payload in db.execute("SELECT seq,payload FROM points WHERE trip_id=? ORDER BY seq", (trip_id,))}
            inserted = 0
            for point in points:
                seq = point["seq"]
                if seq in saved:
                    if saved[seq] != point:
                        raise ValueError("point_conflict")
                    continue
                if seq != len(saved) or (saved and point["elapsed_ms"] <= saved[seq-1]["elapsed_ms"]):
                    raise ValueError("point_order_invalid")
                db.execute("INSERT INTO points VALUES(?,?,?)", (trip_id,seq,json.dumps(point)))
                saved[seq] = point
                inserted += 1
        return dict(accepted_count=len(points), inserted_count=inserted)
