# 팀원 시작 안내

상태: 로컬 서버 연결 확인 완료. 앱·AI·DB·매칭은 미구현. 아래 계약은 v0.1 초안이며 실제 서버 지원 목록과 구분합니다.

## 처음 읽는 순서
1. README.md: 목표와 현재 상태
2. docs/mvp-spec.md: 구현 범위
3. docs/data-contract.md, docs/api-contract.md: 연결 형식
4. docs/role-guide.md: 본인 첫 과제
5. docs/collaboration.md: 작업 전달 방법

## 저장소 받기
GitHub 초대 수락 후 Windows PowerShell에서 본인이 작업할 부모 폴더로 이동합니다. 기존 Re 폴더 위에 clone하지 않습니다.
```powershell
git clone https://github.com/dbco0711/Re.git
cd Re
git status
```
인증은 GitHub 로그인 화면에서 직접 합니다. 비밀번호·토큰을 채팅이나 파일에 넣지 않습니다.

## A·B 공통 실행 순서

공통 기준은 Python 3.11.9입니다. 설치 여부는 `python --version`으로 확인합니다. 다른 버전만 있으면 기존 Python을 지우지 말고 3.11.9 설치·경로 설정을 확인합니다.

| 순서 | 실행 파일 | 정상 결과 |
|---|---|---|
| 최초 준비 | Re 폴더에서 01-setup.cmd 더블클릭 | Setup complete |
| 서버 실행 | 02-start-server.cmd 더블클릭 | Uvicorn running |
| 환경 검사 | 03-check-env.cmd 더블클릭 | PASS |

서버 창을 켜 둔 채 PC 브라우저에서 http://127.0.0.1:8000/health 의 status=ok를 확인합니다. http://127.0.0.1:8000/v1/demo-card 는 fixture 카드입니다. 종료는 서버 창에서 Ctrl+C입니다. 각자의 127.0.0.1은 각자의 PC입니다.

기존 .venv는 검사 후 재사용하며 자동 삭제하지 않습니다. Python 버전 오류·패키지 오류는 화면을 B에게 전달합니다. Port 8000 is busy가 뜨면 기존 Re 서버가 켜져 있는지 확인하고 알 수 없는 프로그램을 강제 종료하지 않습니다.

## C·D
C는 Android 환경을 별도로 확인하고 shared/examples/card.json으로 화면부터 만듭니다. Android Emulator에서 localhost는 PC 서버가 아닙니다. 서버 접속 주소·네트워크 연결은 에뮬레이터 종류를 확인한 후 B와 설정하며 현재 연결 성공으로 간주하지 않습니다.
D는 Python·Android 설치 없이 문서를 읽고 Figma 화면과 동작표를 준비합니다.

## 문제 전달
역할, OS, 도구 버전, 실행 폴더, 명령, 실제 오류, 기대 결과를 공유합니다. 인증정보·실제 음성·좌표는 제거합니다. 각 팀원의 설치·실행은 본인 PC에서 별도로 검증해야 합니다.

## GitHub가 처음이라면

먼저 [설치·계정·clone 상세 안내](github-first-steps.md)를 읽습니다. 전체 문서는 [한글 자료 목차](../00-자료목차.md)에서 찾고, 제출할 때는 [브랜치·PR 실습](git-practice.md)을 따릅니다.

## 서버 화면이 안 열릴 때

서버 실행 명령을 입력한 PowerShell 창을 켜 둔 상태에서 브라우저를 엽니다. Uvicorn running 메시지가 나오기 전에는 서버가 준비되지 않은 것입니다. Ctrl+C 또는 창 닫기로 종료하면 두 주소 모두 열리지 않습니다. 각자의 127.0.0.1은 각자의 PC이며 팀장 서버로 자동 연결되지 않습니다. 접속 실패 시 서버 터미널의 오류를 먼저 전달하고 방화벽을 무작정 끄지 않습니다.
