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
            db.execute("CREATE TABLE IF NOT EXISTS consents(trip_id TEXT PRIMARY KEY REFERENCES trips(id), revoked INTEGER NOT NULL)")
            db.execute("CREATE TABLE IF NOT EXISTS memos(id TEXT PRIMARY KEY, trip_id TEXT REFERENCES trips(id), client_id TEXT NOT NULL, request TEXT NOT NULL, payload TEXT NOT NULL, UNIQUE(trip_id,client_id))")
            db.execute("BEGIN IMMEDIATE")
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
            state = db.execute("SELECT revoked FROM consents WHERE trip_id=?", (trip_id,)).fetchone()
            if state and state[0]:
                raise ValueError("consent_revoked")
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


    def create_memo(self, trip_id, payload):
        from backend.models import MemoCreate
        from datetime import datetime, timedelta
        payload = MemoCreate(**payload).model_dump()
        request = json.dumps(payload,sort_keys=True)
        with self.transaction() as db:
            trip = db.execute("SELECT payload FROM trips WHERE id=?",(trip_id,)).fetchone()
            if not trip:
                raise KeyError(trip_id)
            state = db.execute("SELECT revoked FROM consents WHERE trip_id=?",(trip_id,)).fetchone()
            if state and state[0]:
                raise ValueError("consent_revoked")
            row = db.execute("SELECT request,payload FROM memos WHERE trip_id=? AND client_id=?",(trip_id,payload["client_memo_id"])).fetchone()
            if row:
                if row[0] != request:
                    raise ValueError("client_memo_id_conflict")
                return json.loads(row[1]),False
            points = [json.loads(row[0]) for row in db.execute("SELECT payload FROM points WHERE trip_id=? ORDER BY seq",(trip_id,))]
            start,end = payload["start_elapsed_ms"],payload["end_elapsed_ms"]
            if not points or start < points[0]["elapsed_ms"] or end > points[-1]["elapsed_ms"]:
                raise ValueError("memo_outside_recorded_route")
            first = max(p["seq"] for p in points if p["elapsed_ms"] <= start)
            last = min(p["seq"] for p in points if p["elapsed_ms"] >= end)
            timestamp = datetime.fromisoformat(json.loads(trip[0])["started_at"].replace("Z","+00:00"))+timedelta(milliseconds=start)
            result = dict(payload,memo_id=str(uuid4()),trip_id=trip_id,
                          recorded_at=timestamp.isoformat().replace("+00:00","Z"),
                          linked_seqs=list(range(first,last+1)),status="stored_not_processed")
            db.execute("INSERT INTO consents VALUES(?,0) ON CONFLICT(trip_id) DO NOTHING",(trip_id,))
            db.execute("INSERT INTO memos VALUES(?,?,?,?,?)",(result["memo_id"],trip_id,payload["client_memo_id"],request,json.dumps(result)))
        return result,True

    def get_memo(self,memo_id):
        with self.transaction() as db:
            row = db.execute("SELECT payload FROM memos WHERE id=?",(memo_id,)).fetchone()
            if not row:
                raise KeyError(memo_id)
            return json.loads(row[0])

    def delete_memo(self,memo_id):
        with self.transaction() as db:
            db.execute("DELETE FROM memos WHERE id=?",(memo_id,))

    def revoke_consent(self,trip_id):
        with self.transaction() as db:
            if not db.execute("SELECT 1 FROM trips WHERE id=?",(trip_id,)).fetchone():
                raise KeyError(trip_id)
            db.execute("INSERT INTO consents VALUES(?,1) ON CONFLICT(trip_id) DO UPDATE SET revoked=1",(trip_id,))
            db.execute("DELETE FROM memos WHERE trip_id=?",(trip_id,))
            db.execute("DELETE FROM points WHERE trip_id=?",(trip_id,))


    def update_memo(self,memo_id,changes):
        with self.transaction() as db:
            row = db.execute("SELECT trip_id,payload FROM memos WHERE id=?",(memo_id,)).fetchone()
            if not row:
                raise KeyError(memo_id)
            state = db.execute("SELECT revoked FROM consents WHERE trip_id=?",(row[0],)).fetchone()
            if not state or state[0]:
                raise ValueError("consent_revoked")
            memo=json.loads(row[1])
            memo.update(changes)
            db.execute("UPDATE memos SET payload=? WHERE id=?",(json.dumps(memo),memo_id))
            return memo

    def search_memories(self,current_trip_id,limit=5):
        from backend.matching.service import compare_routes
        from datetime import datetime
        with self.transaction() as db:
            current=db.execute("SELECT payload FROM trips WHERE id=?",(current_trip_id,)).fetchone()
            if not current:
                raise KeyError(current_trip_id)
            current=json.loads(current[0])
            state=db.execute("SELECT revoked FROM consents WHERE trip_id=?",(current_trip_id,)).fetchone()
            if state and state[0]:
                raise ValueError("consent_revoked")
            points=[json.loads(row[0]) for row in db.execute("SELECT payload FROM points WHERE trip_id=? ORDER BY seq DESC LIMIT 200",(current_trip_id,))][::-1]
            rows=db.execute("SELECT m.payload,t.payload FROM memos m JOIN trips t ON t.id=m.trip_id JOIN consents c ON c.trip_id=m.trip_id WHERE m.trip_id!=? AND c.revoked=0 ORDER BY m.rowid DESC LIMIT 50",(current_trip_id,)).fetchall()
            cards=[]
            for memo_data,trip_data in rows:
                memo,trip=json.loads(memo_data),json.loads(trip_data)
                # Synthetic and actual GPS are never silently mixed.
                if trip["source"]!=current["source"]:
                    continue
                if datetime.fromisoformat(memo["recorded_at"].replace("Z","+00:00")) >= datetime.fromisoformat(current["started_at"].replace("Z","+00:00")):
                    continue
                linked=memo["linked_seqs"]
                history=[json.loads(row[0]) for row in db.execute("SELECT payload FROM points WHERE trip_id=? AND seq>=? AND seq<=? ORDER BY seq",(memo["trip_id"],linked[0],linked[-1]))]
                # Count only measured points within the memo interval, not bracketing time outside it.
                history=[p for p in history if memo["start_elapsed_ms"]<=p["elapsed_ms"]<=memo["end_elapsed_ms"]]
                match=compare_routes(current_trip_id,memo["trip_id"],points,history)
                if match["matched"]:
                    cards.append(dict(memo_id=memo["memo_id"],trip_id=memo["trip_id"],
                                      title="이 구간에 남긴 기억이 있습니다",recorded_at=memo["recorded_at"],
                                      source=trip["source"],processing_source=memo["processing_source"],
                                      is_demo=trip["source"]=="replay" or memo["processing_source"]=="fixture",
                                      display_mode="driving_brief",match=match))
            cards.sort(key=lambda c:(c["match"]["overlap_m"],c["recorded_at"]),reverse=True)
            return dict(cards=cards[:limit],candidate_limit=50,current_point_limit=200)
