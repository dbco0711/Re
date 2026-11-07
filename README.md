# Re — 길 위의 기억

사용자가 동의하여 남긴 짧은 음성 메모를 주행 경로·시각과 저장하고, 같은 도로 구간을 다시 지날 때 기억을 확인하는 차량용 앱 프로젝트.

## 현재 상태

초기 공동 세팅과 로컬 서버 연결 확인 단계입니다. Android 앱, Pleos SDK, GPS 매칭, DB, STT·LLM은 아직 구현하거나 연결하지 않았습니다. `/v1/demo-card`는 fixture입니다.

## 로컬 서버 (Windows PowerShell, Re 폴더에서)

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn backend.main:app --host 127.0.0.1 --port 8000
```

이미 `.venv`가 있으면 생성 명령을 생략합니다. 가상환경 활성화 없이 전용 Python을 직접 사용하므로 실행 정책 변경이 필요 없습니다. 브라우저에서 http://127.0.0.1:8000/health 및 http://127.0.0.1:8000/docs 로 확인하고, 서버 터미널에서 Ctrl+C로 종료합니다. 이 주소는 PC 내부 전용이며 Emulator·차량 연결은 별도 단계입니다.

## 팀

| 담당 | 이름 | 범위 |
|---|---|---|
| A | 오태경 | Python, STT·LLM, 구조화 JSON, 오류·품질 검증 |
| B·팀장 | 김유채 | GPS 경로 매칭, FastAPI·SQLite, API 계약·통합 시험 |
| C | 정민성 | Kotlin·Pleos 앱, GPS·녹음·서버 통신, APK |
| D | 명정은 | UI·UX, Figma, 상태별 동작표, 시연 영상·자료; 현재 코딩 제외 |

## 공통 규칙

- A·B는 독립 모듈로 작업하고 C는 샘플 JSON으로 화면을 시작합니다.
- GPS·메모는 같은 trip과 기기 시계로 연결합니다. WGS84, UTC, seq, elapsed_ms를 사용하며 서버 도착 시각으로 위치를 붙이지 않습니다.
- 실제·재생 경로는 source로 구분하고 AI 처리 출처는 fixture/manual/실제 제공자 등으로 명시합니다.
- 현재 trip을 과거 기억으로 매칭하지 않습니다. 거리뿐 아니라 방향·연속 경로·중복 길이를 검증합니다.
- 명시적 시작·중지, 최대 30초 녹음, 수정·삭제·동의 철회를 반영합니다. 상시 녹음은 제외합니다.
- 운전 중 민감한 기억이나 긴 원문을 자동 노출하지 않습니다.
- 실제 음성·동선·DB·키·.env·가상환경·빌드·서명 키는 Git에 넣지 않습니다. 실데이터는 private/ 또는 test-data/real/에만 보관합니다.

## 다음 문서

- [팀 환경과 GitHub 연결](docs/team-setup.md)
- [역할별 첫 과제와 완료 기준](docs/tasks.md)

공식 사이트: https://appdevchallenge.auto-hmg.io/ · Pleos 문서: https://document.pleos.ai/
공모전 일정·제출 조건·SDK 지원 및 권한은 개발·제출 전에 공식 자료로 재확인합니다. 과거 확인 내용은 현재 검증 결과가 아닙니다.

## 팀원 전달 문서

[처음 시작하기](docs/START-HERE.md)에서 MVP·계약·역할별 안내·협업 규칙을 순서대로 확인합니다. 계약 v0.1은 초안이며 예정 API는 아직 구현되지 않았습니다.

## 자료를 못 찾겠다면

[00-자료목차.md](00-자료목차.md)를 먼저 열어 주세요. GitHub 처음 사용자를 위한 설치·받기·PR 실습도 연결되어 있습니다.

## 기초 코드 추가 (2026-11-07)

POST /v1/trips (신규 201·동일 재요청 200), GET /v1/trips/{trip_id}, POST /v1/trips/{trip_id}/points를 구현했습니다. 기존 예정 표의 이 항목은 구현 상태로 갱신합니다. 충돌은 현재 409, 모델 검증은 422입니다. 기본 SQLite 경로는 private/re.db입니다. 메모·삭제·철회·실제 AI·경로 매칭은 미구현입니다.

Python 테스트 3개 및 새 서버의 실제 HTTP 저장·조회·재요청·오류 응답을 검증했습니다. Kotlin은 android-app/STARTER.md의 제한을 확인합니다.

## HTML 안내자료

HTML 파일은 안내자료/ 폴더에 모았습니다. [자료 목차](안내자료/00-자료목차.html), [팀원 안내](안내자료/Re-팀원안내.html), [프로젝트 소개](안내자료/README.html). 팀원 안내 HTML은 자체 목차와 본문이 포함된 독립 파일입니다. 목차·프로젝트 소개 HTML을 전달할 때는 안내자료 폴더의 HTML 3개를 함께 전달합니다.
