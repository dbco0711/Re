# 공통 데이터 계약 v0.1 초안

shared/examples는 전부 fixture입니다. 서버 구현 전 합의용이며 실제 SDK 데이터가 아닙니다.

| 필드 | 규칙 |
|---|---|
| schema_version | 문자열 0.1, 변경 시 A·B·C 합의 |
| trip_id / memo_id | 비어 있지 않은 문자열, 실제 구현은 UUID 예정 |
| source | 경로는 replay 또는 real_gps, 샘플은 replay |
| processing_source | fixture, manual, actual_api; actual_api는 provider·model도 기록 |
| started_at / recorded_at | UTC ISO 8601, Z 표기 |
| elapsed_ms | trip 시작 후 단조 증가 시간, 0 이상 정수 |
| seq | trip 내 0부터 연속 증가, 중복 재전송은 동일 내용만 허용 |
| lat / lon | WGS84 십진도, 각각 -90~90 / -180~180 |
| accuracy_m | 0 이상 미터; 없으면 null, 0으로 추정하지 않음 |

trip.started_at과 GPS elapsed_ms·메모 start_elapsed_ms/end_elapsed_ms는 같은 기기의 같은 trip 기준입니다. 메모 길이는 0초 초과 30초 이하이며 trip 범위를 벗어나면 연결하지 않습니다. 서버 수신 시각으로 좌표를 추정하지 않습니다. heading이 없거나 정지한 경우 방향을 확정하지 않습니다. 끊긴 구간을 무조건 연결하지 않습니다.

AI 입력은 memo_id와 전사 또는 향후 음성 참조입니다. 결과는 transcript, summary, travel_purpose(불명확하면 null), keywords(배열), processing_source를 포함합니다. 실패를 빈 성공 응답으로 바꾸지 않습니다. manual 전사와 fixture는 actual_api가 아닙니다.

card.json은 UI 연결용입니다. 녹음·GPS·AI 결과 또는 재방문 판정의 증거가 아닙니다. 실제 API 연결 전 타입·필수 여부를 서버 모델과 맞추고 버전을 확정합니다.
