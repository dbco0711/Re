# API 계약 v0.1 초안

## 현재 실제 구현
| 메서드 | 경로 | 응답 |
|---|---|---|
| GET | /health | 200, status=ok, service=re-backend |
| GET | /v1/demo-card | 200, fixture 카드. backend/main.py가 현재 응답의 기준 |

## 향후 구현 예정 — 현재 호출 불가
| 메서드 | 경로 초안 | 입력·동작 | 정상 응답 초안 |
|---|---|---|---|
| POST | /v1/trips | source, started_at, client_trip_id | 201, trip_id |
| POST | /v1/trips/{trip_id}/points | points 배열, seq 기반 재전송 | 200, accepted_count |
| POST | /v1/trips/{trip_id}/memos | client_memo_id, 동의, 메모 구간 | 201, memo_id, status |
| GET | /v1/memos/{memo_id} | 처리 상태·결과 조회 | 200, pending/processing/completed/failed |
| POST | /v1/memories/search | current_trip_id, 최근 points | 200, cards 배열(없으면 []) |
| PATCH | /v1/memos/{memo_id} | 정차 후 summary 등 수정 | 200, 수정 결과 |
| DELETE | /v1/memos/{memo_id} | 연관 데이터 삭제 | 204 |
| DELETE | /v1/trips/{trip_id}/consent | 철회·작업 중단·연관 데이터 제거 | 204 |

음성 업로드 방식·파일 제한·인증은 미정입니다. 처리 전 서버에서 동의를 재확인합니다. 삭제·철회 도중 완료된 AI 결과가 다시 저장되지 않도록 취소 상태를 확인해야 합니다. 현재 서버는 인증 없는 PC 내부 연결 확인용이므로 외부 노출하지 않습니다.

예정 오류: 400 순서·시간 충돌, 404 미존재, 409 동일 client ID에 다른 내용, 422 입력 검증, 503 처리 제공자 장애. error.json은 목표 형식이며 현재 FastAPI 기본 오류를 변경한 것은 아닙니다.

A↔B는 Python 모듈 단위로 우선 연결합니다. 별도 AI HTTP 서버는 요구하지 않습니다. 구현 전 B가 요청·응답 모델과 오류 형식을 확정하고 C에게 전달합니다.
