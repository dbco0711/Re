import logging

from backend.ai.analyzer import analyze_memo
from fastapi import APIRouter, HTTPException, Response
from backend.models import MemoCreate, MemoUpdate, AIResult, MemorySearch
from backend.routes.trips import store
router = APIRouter(tags=["memo-starter"])
logger = logging.getLogger(__name__)

def fail(error):
    raise HTTPException(404 if isinstance(error,KeyError) else 409,
                        "resource_not_found" if isinstance(error,KeyError) else str(error))

@router.post("/v1/trips/{trip_id}/memos",status_code=201)
def create_memo(trip_id:str,body:MemoCreate,response:Response):
    database = store()
    try:
        result, created = database.create_memo(trip_id, body.model_dump())
        response.status_code = 201 if created else 200
        if not created or body.processing_source != "manual":
            return result

        memo_id = result["memo_id"]
        try:
            analysis = AIResult.model_validate(analyze_memo(memo_id, body.transcript or ""))
            changes = analysis.model_dump(exclude={"transcript"})
            changes.update(status="processed", summary_source=analysis.processing_source)
        except Exception as error:
            # Exceptions can include input text. Log only the exception type.
            logger.warning("Automatic memo analysis failed (%s)", type(error).__name__)
            changes = {"status": "analysis_failed"}
        return database.update_memo(memo_id, changes, only_unprocessed=True)
    except (KeyError, ValueError) as error:
        fail(error)

@router.get("/v1/memos/{memo_id}")
def get_memo(memo_id:str):
    try:
        return store().get_memo(memo_id)
    except KeyError as error:
        fail(error)

@router.delete("/v1/memos/{memo_id}",status_code=204)
def delete_memo(memo_id:str):
    store().delete_memo(memo_id)
    return Response(status_code=204)

@router.delete("/v1/trips/{trip_id}/consent",status_code=204)
def revoke_consent(trip_id:str):
    try:
        store().revoke_consent(trip_id)
        return Response(status_code=204)
    except KeyError as error:
        fail(error)


@router.patch("/v1/memos/{memo_id}")
def update_memo(memo_id:str,body:MemoUpdate):
    try:
        return store().update_memo(memo_id,dict(summary=body.summary,summary_source="user_edit"))
    except (KeyError,ValueError) as error:
        fail(error)

@router.put("/v1/memos/{memo_id}/analysis")
def attach_analysis(memo_id:str,body:AIResult):
    try:
        result=body.model_dump()
        result["status"]="processed"
        result["summary_source"]=body.processing_source
        return store().update_memo(memo_id,result)
    except (KeyError,ValueError) as error:
        fail(error)

@router.post("/v1/memories/search")
def search_memories(body:MemorySearch):
    try:
        return store().search_memories(body.current_trip_id,body.limit)
    except (KeyError,ValueError) as error:
        fail(error)
