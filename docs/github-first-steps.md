# GitHub·Git 처음 사용하는 팀원 안내 (Windows)

## 1. 무엇을 설치하나요?
GitHub는 브라우저로 사용하는 공동 저장소 서비스입니다. Git은 PC에서 변경 이력을 관리하는 프로그램입니다. 이 안내는 Git 명령을 사용하며 GitHub Desktop은 필수 설치가 아닙니다. D는 문서 열람과 Figma 작업만 한다면 Git·Python을 설치하지 않아도 됩니다.

## 2. 계정·초대 확인
브라우저에서 https://github.com 접속 → 계정이 없으면 Sign up, 있으면 Sign in → 본인 계정으로 로그인합니다. 팀장이 보낸 저장소 초대를 수락한 뒤 https://github.com/dbco0711/Re 를 엽니다. 404가 뜨면 계정·초대 수락부터 확인합니다. 비밀번호·인증 코드·토큰은 공유하지 않습니다.

## 3. Git 설치 확인과 설치
Windows 시작 버튼에서 PowerShell을 검색해 실행합니다. 다음 한 줄을 붙여넣고 Enter를 누릅니다. 코드 블록의 ```나 PS로 시작하는 안내 문구는 입력하지 않습니다.
```powershell
git --version
```
버전이 나오면 설치를 건너뜁니다. 명령을 찾을 수 없다고 나오면 https://git-scm.com/install/windows 에서 PC 아키텍처에 맞는 Setup 설치 파일을 받습니다(설정 → 시스템 → 정보 → 시스템 종류로 x64/ARM64 확인). 다운로드한 설치 파일을 실행하고 설치 마법사의 안내를 따릅니다. 모르는 선택지가 있으면 기본값을 유지하되 PATH 항목은 명령 프롬프트와 다른 프로그램에서 Git을 사용할 수 있는 선택을 유지합니다. 설치 후 PowerShell을 닫았다가 다시 열어 git --version을 재확인합니다.

## 4. 프로젝트를 내 PC에 받기 (clone)
탐색기에서 본인이 작업할 부모 폴더를 새로 만들거나 선택합니다. 팀장의 PC 경로를 따라 만들 필요는 없습니다. 그 폴더 주소 표시줄을 클릭해 powershell을 입력하고 Enter를 누릅니다.
```powershell
Get-Location
git clone https://github.com/dbco0711/Re.git
cd Re
git status
```
한 줄씩 실행합니다. Get-Location은 현재 위치를 확인합니다. clone은 그 안에 Re 폴더를 만들고 내려받습니다. cd Re는 받은 폴더로 들어갑니다. status는 현재 상태를 보여줍니다. 인증 창이 나타나면 본인 GitHub 계정으로 브라우저 인증합니다. 비밀번호를 명령에 붙이지 않습니다.
Re 폴더가 이미 있다는 오류가 나면 삭제하거나 다시 clone하지 말고 기존 폴더의 출처를 B에게 확인합니다. GitHub Code → Download ZIP은 읽기용으로 가능하지만 Git 이력이 없으므로 협업용 clone 대신 사용하지 않습니다.

## 5. 파일 읽기
Re에서 00-자료목차.md를 먼저 엽니다. GitHub에서는 Code 탭에서 파일을 클릭하면 문서가 표시됩니다. PC에서는 메모장 또는 이미 설치된 코드 편집기로 엽니다. D는 GitHub에서 바로 읽어도 됩니다.

## 6. Python은 A·B만 우선 준비
PowerShell의 Re 폴더에서 python --version을 실행합니다. 현재 B 검증 기준은 3.11.9이며 최신 버전과 같다는 뜻은 아닙니다. 이미 Python이 있으면 재설치하지 말고 버전을 B에게 알려줍니다. 설치가 없으면 https://www.python.org/downloads/windows/ 공식 배포를 사용하되 팀이 사용할 버전을 B와 먼저 맞춥니다. 3.11.9용 기존 설치 파일을 선택한다면 Windows installer를 사용하며 embeddable package는 선택하지 않습니다. 설치 창에서 Add python.exe to PATH가 있으면 선택하고 설치합니다. PowerShell을 다시 열어 버전을 확인합니다. 설치 관리자 화면이 다르거나 Microsoft Store만 열리면 멈추고 B에게 화면·오류를 전달합니다.
실행은 START-HERE.md의 Python 명령을 한 줄씩 따릅니다. 팀원 PC의 설치·실행 성공은 아직 확인하지 않았습니다.

공식 참고: [Git Windows 설치](https://git-scm.com/install/windows), [GitHub clone 안내](https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository), [Python Windows 배포](https://www.python.org/downloads/windows/).
