# 첫 작업 제출 실습 (A·B·C, Git 명령 사용)

commit은 PC 안의 저장 지점, push는 GitHub 업로드, pull은 팀 변경 내려받기, branch는 독립 작업 줄기, PR은 main에 합쳐 달라는 검토 요청입니다. 파일을 저장하는 것만으로 GitHub에 올라가지 않습니다.

## 1. 시작 전
PowerShell에서 clone한 Re 폴더로 들어가 실행합니다.
```powershell
git status
```
변경이 있으면 다음 명령을 진행하기 전에 B에게 확인합니다. 깨끗하면:
```powershell
git switch main
git pull --ff-only
git switch -c a/first-fixture
```
마지막 이름은 A 예시입니다. C는 c/first-screen처럼 자신의 작업 이름을 사용합니다. 기존 브랜치가 있다고 나오면 -c로 재생성하지 말고 B에게 확인합니다.

## 2. 작성자 확인 (첫 commit 전)
```powershell
git config --get user.name
git config --get user.email
```
값이 없거나 본인이 아니면 아래의 예시를 본인 값으로 바꿔 실행합니다. --global을 쓰지 않으므로 이 프로젝트에만 적용됩니다. 공개하고 싶지 않은 이메일은 GitHub Settings → Emails에서 제공하는 본인의 noreply 주소를 사용합니다.
```powershell
git config user.name "본인 이름"
git config user.email "본인의 GitHub용 이메일"
```
예시 문자열 그대로 저장하지 않습니다.

## 3. 파일 저장 후 확인·commit
아래 경로는 A가 실제로 해당 파일을 만들었을 때의 예시입니다. 파일이 없으면 그대로 실행하지 말고 본인이 수정한 파일 경로로 바꿉니다.
```powershell
git status
git diff
git add backend/ai/example.py
git diff --cached
git commit -m "Add fixture AI result example"
```
새 파일 내용은 git diff에 안 보일 수 있어 git diff --cached에서 반드시 확인합니다. 키·음성·실제 위치·.env가 보이면 commit하지 말고 B에게 확인합니다. nothing to commit은 저장하거나 선택한 변경이 없는 상태입니다.

## 4. GitHub에 작업 브랜치 올리기
```powershell
git push -u origin a/first-fixture
```
본인의 브랜치 이름으로 바꿉니다. GitHub 인증은 본인 계정으로 진행합니다. main에 직접 push하거나 --force를 붙이지 않습니다.

## 5. PR 만들기
GitHub Re 저장소 → Pull requests → New pull request → base: main, compare: 본인 브랜치 → 변경 파일 확인 → Create pull request. 제목은 변경 목적, 본문은 바뀐 내용·실행한 명령과 결과·미검증 항목을 씁니다. B 검토 전 임의로 Merge하지 않습니다.

## 6. 충돌·거절 시
push rejected, merge conflict, pull 실패가 나면 강제 명령을 사용하지 않습니다. 실행한 명령·오류를 인증정보 없이 B에게 전달합니다. PR이 합쳐진 뒤 새 작업은 깨끗한 main에서 pull하고 새 브랜치를 만듭니다. 다른 작업 중 변경을 잃을 수 있으니 reset·clean을 임의로 실행하지 않습니다.

D는 이 실습이 필수가 아닙니다. Figma 링크·동작표를 B·C에게 전달하는 방식으로 시작합니다.
