# Re : load — 길 위의 기억

Pleos Connect 차량 앱에서 남긴 음성 메모를 주행 구간에 연결합니다. 현재 위치 주변의 추억을 찾아보고, 같은 방향으로 다시 지난 구간의 추억을 검색할 수 있습니다.

[개발 현황 및 향후 계획](docs/project-status.md)

## 로컬 실행

Windows에서 Python 3.11.9를 준비한 뒤 저장소 폴더의 01-setup.cmd를 실행합니다. 설치가 끝나면 02-start-server.cmd를 실행하고 [API 시험 화면](http://127.0.0.1:8000/docs)을 엽니다. 기본 DB는 private/re.db에 저장됩니다.

Android Studio에서는 android-app/practice-pleos 프로젝트를 열고 Pleos Connect 에뮬레이터를 선택합니다. Windows에서 한글 경로 때문에 Gradle 오류가 나면 영문 경로의 작업 폴더를 사용합니다. 앱에서 사용하는 서버 주소와 에뮬레이터의 포트 연결을 확인합니다.

## 기술 문서

- [API 계약](docs/api-contract.md)
- [데이터 계약](docs/data-contract.md)
- [B 백엔드 검증](docs/b-verification.md)
