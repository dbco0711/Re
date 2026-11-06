# 팀 공동 환경 준비

## 확인된 로컬 환경 (2026-11-06)

- Windows PC, OS 빌드 26200. CIM 조회는 접근 제한으로 실패했고 .NET·Python으로 Windows를 확인했습니다.
- Git 2.55.0.windows.4, Python 3.11.9. `py`는 설치 Python을 인식하지 못하므로 `python` 또는 `.venv\Scripts\python.exe`를 사용합니다.
- 상위 고구마 폴더에 커밋·remote가 없는 빈 Git 저장소가 있습니다. Re 전용 Git 초기화·GitHub 업로드는 아직 하지 않았습니다.
- Android Studio·SDK는 기본 위치에서 발견되지 않았습니다. 전체 PC 미설치 판정은 아닙니다.

## GitHub 계정과 비공개 저장소 (사용자가 직접 진행)

1. 브라우저에서 https://github.com 에 접속합니다. 계정이 있으면 로그인하고 없으면 Sign up을 선택합니다. 비밀번호·인증 코드·토큰은 채팅에 입력하지 않습니다.
2. 우측 상단 + → New repository → Repository name `Re` → Private를 선택합니다. README, .gitignore, License 자동 추가는 선택하지 않고 Create repository를 누릅니다.
3. 생성된 HTTPS 저장소 주소만 확인합니다. 아직 파일을 올리지 않아도 됩니다. 계정·저장소 준비 후 주소를 알려주면 로컬 연결 상태를 먼저 확인합니다.
4. 로컬 Git은 Re 폴더 자체를 저장소로 만들 예정입니다. 상위 빈 저장소는 삭제하지 않습니다. 초기화 전에 경계를 다시 확인합니다. Git 사용자 이름·이메일도 확인 후 설정하며 임의로 추정하지 않습니다.
5. 첫 업로드 전 `git status`, `git diff --cached`로 파일을 검토하고 실제 키·음성·위치·DB가 없는지 확인합니다. push는 사용자 요청 후 진행합니다.
6. 팀 초대는 GitHub 저장소 Settings → Collaborators에서 진행할 수 있습니다. 현재 초대·메시지 발송은 수행하지 않았습니다.

## 구조와 설치 범위

현재 backend/, docs/와 루트 설정 파일만 만듭니다. android-app/, backend/ai·matching·database·routes·services·tests, shared/examples/, test-data/synthetic/, design/, tools/, apk/는 필요한 작업을 시작할 때 추가합니다.

Python은 서버·AI용이고 Kotlin은 차량 앱용입니다. B의 현재 작업에는 Android 도구 설치가 필요 없습니다. SQLite는 향후 Python 기본 sqlite3로 시작할 수 있으며 현재 DB 파일은 만들지 않습니다.

OneDrive 아래 가상환경은 다른 PC와 공유·복사하지 말고 각 PC에서 재생성합니다. 동기화 충돌이 생기면 프로젝트 위치 이동을 별도로 검토합니다.

## 현재 Git 연결 상태 (2026-11-06)

실제 작업 위치는 바탕 화면의 공모전 작업 폴더/Re입니다. 이 Re 폴더 자체에 Git을 초기화했고 기본 브랜치는 main입니다.
origin: https://github.com/dbco0711/Re.git
원격 주소만 등록했습니다. 원격 인증·접근 권한·Private 여부는 아직 검증하지 않았습니다.
커밋, push, 팀 초대는 수행하지 않았습니다. 기존 전역 Git 작성자 설정을 확인했으며 변경하지 않았습니다.
