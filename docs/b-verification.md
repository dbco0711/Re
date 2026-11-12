# B 백엔드 구현·검증 현황 (2026-11-07)

로컬 브랜치 codex/b-backend. GitHub 미업로드. 실제 음성·유료 AI·외부 배포 없음.

## 구현
- trip·GPS 저장·조회, 동일 요청 재전송, 순서·좌표·UTC 검증.
- 메모 동의·최대 30초·GPS 시간 구간 연결·저장·조회·수정·삭제.
- 철회 시 메모·GPS 삭제, 추가 수집 거부, 삭제·철회 후 늦은 AI 결과 거부.
- PUT /v1/memos/{memo_id}/analysis: A 결과 수신. fixture/manual/actual_api 구분, actual_api 제공자·모델 필수. 실제 제공자를 호출하거나 결과의 진위를 인증하는 기능은 아님.
- POST /v1/memories/search: current_trip_id, limit(기본5/최대20). 저장된 현재 GPS 최근200점, 최근 메모 후보50개 검색. 현재 trip·미래 메모·철회 제외, replay와 real_gps 혼합 제외.
- 회상 카드는 일반 안내·시각·출처·매칭 수치만 제공. 운전 중 원문·요약 자동 노출 없음. 상세 GET은 C가 정차 후 명시적 조작에서만 호출해야 함.

## 매칭 기준 (실제 주행 미보정)
WGS84 두 점 거리·방향 계산 → 약10m 간격 재표본 → 순서가 이어지는 같은 방향 구간 비교.
거리50m, 방향차35도, 중복100m 이상. 마지막 현재 이동 구간까지 일치해야 하며 이미 이탈한 옛 구간만으로 표시하지 않음.
5m 미만 이동·15초 초과 GPS 간격·seq 누락·30m 초과 또는 미제공 정확도·60m/s 초과 속도 구간은 연결하지 않음. 후보별 최대3000 표본 초과 시 409로 작은 경로 창 요청 필요.
메모 실제 시간 안의 측정점만 매칭하므로 짧거나 점이 적은 메모는 검색되지 않을 수 있음. 같은 근처의 평행도로·복잡한 교차로·다른 표본 간격·GPS 튐은 실제 자료로 추가 검증 필요. 도로망 Map Matching이나 정밀 위치 보간이 아님.

## 실행 검증
```powershell
.\.venv\Scripts\python.exe -m unittest discover -s backend/tests -v
.\.venv\Scripts\python.exe tools/verify_backend.py
```
합성 경로·임시 DB만 사용. 통합 검증은 별도 포트에 서버를 띄우고 완료 후 정리. 기존 DB와 서버는 보존.

## 남은 실제 통합
A의 실제 STT·LLM 실행과 실패·철회 연동, C의 GPS·녹음·정차 UI·Emulator 연결, 실제 주행 임계값 보정. 인증 없는 PC 내부 MVP이므로 인터넷 공개 불가. 원음·외부 AI 보관 삭제는 아직 데이터가 없어 미검증. 멀티 계정·운영 보안·대량 조회 최적화는 범위 밖.

서버 재시작: 기존 창 Ctrl+C → 02-start-server.cmd → /docs. 이번 시험에서 철회한 trip은 재사용하지 않고 새 client_trip_id로 주행 생성.
