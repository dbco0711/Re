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

## 기초 코드 추가 (2026-11-07)

POST /v1/trips (신규 201·동일 재요청 200), GET /v1/trips/{trip_id}, POST /v1/trips/{trip_id}/points를 구현했습니다. 기존 예정 표의 이 항목은 구현 상태로 갱신합니다. 충돌은 현재 409, 모델 검증은 422입니다. 기본 SQLite 경로는 private/re.db입니다. 메모·삭제·철회·실제 AI·경로 매칭은 미구현입니다.

Python 테스트 3개 및 새 서버의 실제 HTTP 저장·조회·재요청·오류 응답을 검증했습니다. Kotlin은 android-app/STARTER.md의 제한을 확인합니다.

## 메모 정보·동의 API 구현 (2026-11-07)

- POST /v1/trips/{trip_id}/memos: client_memo_id, consent=true, start_elapsed_ms, end_elapsed_ms, processing_source(fixture/manual), transcript(선택). manual은 전사 필수. 신규 201, 동일 요청 200, 충돌·경로 범위 밖·철회 후 409, 입력 검증 422.
- GET /v1/memos/{memo_id}: 메모와 linked_seqs 조회. 메모 시작 UTC는 trip 시작 시각+start_elapsed_ms로 계산. 서버 도착 시각 미사용.
- DELETE /v1/memos/{memo_id}: 메모 삭제, 반복 호출 204.
- DELETE /v1/trips/{trip_id}/consent: 해당 trip 메모·GPS 삭제, 철회 상태 유지, 이후 메모·GPS 수집 거부. 반복 호출 204. 재동의는 미구현이며 새 trip으로 진행.

메모 길이는 0초 초과 최대 30초이며 이미 수신한 GPS 시간 범위 안에서만 저장합니다. linked_seqs는 시작·끝을 감싸는 GPS 점 범위이지 위치 보간·연속성 검증 결과가 아닙니다. GPS 누락 품질 검증은 후속 작업입니다.
현재 음성 파일·AI 작업·외부 제공자 데이터는 생성하지 않으므로 그 삭제·취소는 아직 검증 범위가 아닙니다. trip의 최소 식별 정보는 철회 거부용으로 유지합니다. 인증 없는 PC 내부 개발용입니다.
테스트 5개 및 임시 DB의 실제 HTTP 저장·조회·삭제·철회·거부 응답 검증 완료. 기존 DB와 실행 서버는 변경하지 않았습니다.

## B 로컬 구현 갱신 (2026-11-07)
PATCH /v1/memos/{memo_id}: summary(1~300자) 수정.
PUT /v1/memos/{memo_id}/analysis: processing_source, transcript, summary, travel_purpose, keywords, provider, model. 출처 검증 실패422, 삭제 후404.
POST /v1/memories/search: current_trip_id, limit → cards 배열. 미존재404, 철회·잘못된 경로 순서·표본 한도 초과409. 원문·요약은 카드에 포함하지 않음.
현재 API는 저장된 GPS를 검색 입력으로 쓰며 예정 계약의 points 직접 입력은 지원하지 않습니다. 상세 구현·검증·한계는 b-verification.md가 최신 기준입니다.
