# 환경 검증

2026-11-06: Windows, Python 3.11.9.

- 기존 프로젝트 가상환경과 별개인 임시 venv 생성 완료.
- requirements.txt만으로 새 환경 패키지 설치 완료.
- pip check 통과.
- 새 환경의 Uvicorn으로 127.0.0.1:18764 서버 실행.
- GET /health: HTTP 200, status=ok 확인.
- GET /v1/demo-card: HTTP 200, source=fixture 확인.
- 시험 서버 종료 및 이 작업에서 만든 임시 환경 정리 완료.
- 기존 .venv와 8000번 서버는 변경하지 않음.

한 PC의 새 가상환경 재현 검증입니다. Python 미설치 PC의 설치, 팀원 PC, Android·Pleos·GPS·AI는 검증하지 않았습니다.
