# 협업 규칙

main은 통합 기준입니다. 작업 전 git status로 미저장 변경을 확인하고, 깨끗한 main에서 git pull --ff-only 후 역할별 브랜치를 만듭니다. 충돌 시 강제 덮어쓰기·reset을 하지 말고 B와 확인합니다.

예: git switch -c a/fixture-result, b/trip-storage, c/home-screen, d/design-notes. Codex가 만드는 브랜치는 codex/ 접두사를 사용합니다.

변경 파일을 git add 경로로 선택하고 git diff --cached로 확인 후 commit·push합니다. GitHub에서 Pull request를 만들고 B가 검토·통합합니다. PR에는 변경 목적, 실제 실행 결과, 미검증 항목을 씁니다. main 강제 push는 하지 않습니다.

담당: A backend/ai, B backend의 나머지·데이터 계약, C android-app, D design. shared/examples와 공통 사양 변경은 A·B·C가 먼저 합의합니다. 담당은 협업 경계이며 GitHub 접근 권한 제한을 설정했다는 뜻은 아닙니다.

실제 키·음성·동선·DB·빌드·서명 키는 올리지 않습니다. JSON이라고 자동으로 안전한 것은 아닙니다. 합성 자료만 shared/examples와 test-data/synthetic에 넣습니다. 팀원 메시지·초대·배포는 별도 사용자 작업입니다.
