"""Synthetic HTTP integration check; isolated DB and loopback server."""
import json, os, socket, subprocess, sys, tempfile, time
from pathlib import Path
import urllib.request, urllib.error
ROOT=Path(__file__).resolve().parents[1]

def verify():
    with tempfile.TemporaryDirectory(prefix="re-backend-") as tmp:
        env=os.environ.copy();env["RE_DB_PATH"]=str(Path(tmp)/"test.db")
        with socket.socket() as sock:
            sock.bind(("127.0.0.1",0));port=sock.getsockname()[1]
        server=subprocess.Popen([sys.executable,"-m","uvicorn","backend.main:app","--host","127.0.0.1","--port",str(port)],cwd=ROOT,env=env,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
        def call(path,method="GET",body=None):
            request=urllib.request.Request(f"http://127.0.0.1:{port}"+path,method=method,data=json.dumps(body).encode() if body is not None else None,headers={"Content-Type":"application/json"})
            try:
                with urllib.request.urlopen(request,timeout=10) as response:
                    raw=response.read();return response.status,json.loads(raw) if raw else None
            except urllib.error.HTTPError as error:
                return error.code,json.loads(error.read())
        try:
            for _ in range(60):
                if server.poll() is not None:raise RuntimeError(server.stderr.read().decode(errors="replace"))
                try:
                    if call("/health")[0]==200:break
                except OSError:time.sleep(.1)
            else:raise RuntimeError("readiness timeout")
            def trip(name,date,reverse=False,count=7):
                code,data=call("/v1/trips","POST",dict(client_trip_id=name,source="replay",started_at=date));assert code==201
                tid=data["trip_id"]
                coords=list(range(count));coords=coords[::-1] if reverse else coords
                points=[dict(seq=i,elapsed_ms=i*10000,lat=37,lon=127+j*.001,accuracy_m=5) for i,j in enumerate(coords)]
                assert call(f"/v1/trips/{tid}/points","POST",dict(points=points))[0]==200
                return tid
            past=trip("past","2026-11-01T00:00:00Z")
            now=trip("now","2026-11-07T00:00:00Z",count=4)
            departed=trip("departed","2026-11-07T00:00:00Z")
            body=dict(client_memo_id="memo",consent=True,start_elapsed_ms=0,end_elapsed_ms=30000)
            code,memo=call(f"/v1/trips/{past}/memos","POST",body);assert code==201
            uri="/v1/memos/"+memo["memo_id"]
            assert call(f"/v1/trips/{past}/memos","POST",body)[0]==200
            assert call(f"/v1/trips/{past}/memos","POST",dict(body,consent=False))[0]==422
            assert call(uri,"PATCH",dict(summary="edited fixture"))[1]["summary_source"]=="user_edit"
            ai=dict(processing_source="fixture",transcript="fixture text",summary="fixture summary")
            assert call(uri+"/analysis","PUT",ai)[0]==200
            assert call(uri+"/analysis","PUT",dict(ai,processing_source="actual_api"))[0]==422
            code,result=call("/v1/memories/search","POST",dict(current_trip_id=now));assert code==200 and len(result["cards"])==1
            assert call("/v1/memories/search","POST",dict(current_trip_id=departed))[1]["cards"]==[]
            assert "transcript" not in result["cards"][0] and "summary" not in result["cards"][0]
            assert call("/v1/memories/search","POST",dict(current_trip_id=past))[1]["cards"]==[]
            reverse=trip("reverse","2026-11-07T00:00:00Z",True)
            assert call("/v1/memories/search","POST",dict(current_trip_id=reverse))[1]["cards"]==[]
            assert call(f"/v1/trips/{past}/consent","DELETE")[0]==204
            assert call(uri)[0]==404
            assert call(uri+"/analysis","PUT",ai)[0]==404
            assert call(f"/v1/trips/{past}/memos","POST",body)[0]==409
            assert call("/v1/memories/search","POST",dict(current_trip_id=now))[1]["cards"]==[]
            assert call("/v1/demo-card")[1]["source"]=="fixture"
            print("PASS: synthetic HTTP trip/memo/edit/analysis/search/reverse/revoke/late-result flow")
        finally:
            server.terminate();server.wait(timeout=10)
            server.stderr.close()

if __name__=="__main__":verify()
