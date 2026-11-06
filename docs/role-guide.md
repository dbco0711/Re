# 역할별 시작과 인수 기준

## A 오태경
1. START-HERE의 Python 환경 확인.
2. shared/examples/ai-result.json을 읽고 fixture 결과 생성 함수 준비.
3. 다음 담당 폴더 backend/ai에서 작업. 함수 계약 초안은 analyze_memo 입력 memo_id·transcript, 출력 AI 결과 dict. 실제 음성 입력 방식은 B와 합의.
4. 정상·빈 입력·불명확한 목적·처리 실패 예시를 전달. 이후 제공자 선정 후 실제 STT·LLM 연결.
완료 증거: 실행 명령, 실제 출력, 출처, 실패 예시. API 키는 공유하지 않음.

## B 김유채
현재 서버·가상환경은 로컬 검증 완료. 다음은 데이터 모델·SQLite 저장·API 입력 검증과 매칭 시험 설계입니다. backend/database, matching, routes, services는 시작 시 생성합니다. A·C와 v0.1 필드를 합의하고 변경 기록을 남깁니다.
완료 증거: API별 실행 결과, 합성 데이터 시험, 실제·fixture 구분.

## C 정민성
기존 Android 연습 프로젝트를 보존합니다. Android Studio·SDK·JDK·Gradle·AGP·Kotlin 버전과 Pleos 계정 접근 상태를 기록합니다. 공식 문서에서 호환 조합을 확인하고 새 android-app 프로젝트를 생성합니다. 이 저장소에는 아직 Android 빌드 파일이 없습니다.
첫 결과는 빈 앱의 실제 Emulator 실행, 다음은 card.json 기반 화면입니다. 테스트 경로 재생과 실제 GPS 수신을 구분합니다. 서버 연결·권한 거부·네트워크 실패는 따로 검증합니다.
완료 증거: 실행 화면, 버전 목록, 실제 실행 환경·오류 기록.

## D 명정은
코딩·Python 설치는 요구하지 않습니다. design/handoff.md를 기준으로 Figma 홈·녹음·회상 화면과 동작표를 작성합니다. 디자인의 mock 데이터를 실제 처리처럼 표현하지 않습니다.
완료 증거: Figma 링크 또는 내보내기, 상태별 문구·버튼 동작·화면 크기, C에게 전달할 명세. 영상은 구현 완료 기능과 데모를 구분합니다.

## 후속 작업

[역할별 후속 작업표](roadmap.md)에서 다음 단계·제출물·통합 순서를 확인합니다.
