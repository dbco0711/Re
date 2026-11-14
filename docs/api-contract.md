# Re : load API

서버를 실행하면 /docs에서 현재 요청과 응답 형식을 확인할 수 있습니다.

| 메서드 | 경로 | 동작 |
|---|---|---|
| GET | /health | 서버 상태 |
| GET | /v1/demo-card | 가상 회상 카드 |
| POST | /v1/trips | 주행 생성 |
| GET | /v1/trips/{trip_id} | 주행과 위치 기록 조회 |
| POST | /v1/trips/{trip_id}/points | GPS 점 저장 |
| POST | /v1/trips/{trip_id}/memos | 메모 저장 및 전사문 분석 |
| GET | /v1/memos | 저장된 메모 목록 |
| GET | /v1/memos/nearby | 위도·경도 기준 주변 메모 또는 녹음 단위 조회 |
| GET | /v1/memos/{memo_id} | 전사문과 분석 결과 조회 |
| PATCH | /v1/memos/{memo_id} | 요약 수정 |
| PUT | /v1/memos/{memo_id}/analysis | 분석 결과 저장 |
| DELETE | /v1/memos/{memo_id} | 메모 삭제 |
| DELETE | /v1/trips/{trip_id}/consent | 동의 철회 및 저장된 메모·위치 점 삭제 |
| POST | /v1/memories/search | 현재 주행 ID로 이전 경로의 추억 검색 |

신규 주행과 메모 등록은 201, 동일 요청의 재전송은 200을 반환합니다. 요청 자료의 형식 오류는 422, 기존 기록 또는 주행 상태와의 충돌은 409입니다. 전사문 자동 분석에 실패하면 원문을 보관하고 상태를 analysis_failed로 기록합니다.

[구현 현황과 향후 계획](project-status.md) · [데이터 계약](data-contract.md) · [백엔드 검증](b-verification.md)
