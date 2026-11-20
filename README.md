# Re : load — 길 위의 기억

주행 중 남긴 이야기를 텍스트로 저장하고 당시 지나던 구간과 연결해, 같은 길에서 추억을 다시 만나는 Pleos Connect 차량용 앱입니다.

[개발 현황 및 향후 계획](docs/project-status.md) · [APK 다운로드](apk/Re-load-debug.apk) · [APK 실행 및 검증 범위](apk/README.md)

## 실행 방법

### 백엔드 서버

1. Windows PC에 Python 3.11.9와 Git을 설치하고 저장소를 내려받습니다.
2. 저장소 루트에서 `01-setup.cmd`를 실행해 가상환경과 의존성을 설치합니다.
3. `02-start-server.cmd`를 실행하고 서버 창을 유지합니다.
4. 브라우저에서 [서버 상태](http://127.0.0.1:8000/health)와 [API 시험 화면](http://127.0.0.1:8000/docs)을 확인합니다.

SQLite 기본 저장 위치는 `private/re.db`입니다. 같은 DB 파일을 유지하면 서버를 종료했다 다시 실행해도 저장된 기록을 조회할 수 있습니다. `RE_DB_PATH`를 설정하면 DB 위치를 변경할 수 있습니다.

### APK 설치 및 서버 연결

1. Android Studio의 Device Manager에서 Pleos Connect 에뮬레이터를 실행합니다.
2. [Re-load-debug.apk](apk/Re-load-debug.apk)를 다운로드합니다. APK는 Android에 설치하는 파일이며, Word나 Android Studio의 APK 분석 화면에서 열어 실행하는 파일이 아닙니다.
3. Android SDK의 `platform-tools`가 PATH에 등록된 터미널에서 다음 명령을 실행합니다. PATH에 없다면 해당 폴더에서 실행하거나 `adb.exe`의 전체 경로를 사용합니다.

```powershell
adb devices
adb install -r "다운로드한 APK의 전체 경로"
adb reverse tcp:8000 tcp:8000
```

PowerShell에서 현재 폴더의 실행 파일을 사용하는 경우 `adb` 대신 `./adb.exe`를 입력합니다. 기기가 여러 대 연결되어 있으면 `adb -s 기기번호`로 대상 에뮬레이터를 지정합니다.

4. 에뮬레이터 앱 목록에서 앱을 실행하고 마이크 권한을 허용합니다. PC의 백엔드 서버도 실행 중이어야 합니다.

앱의 서버 주소는 `http://127.0.0.1:8000`입니다. `adb reverse`가 에뮬레이터의 요청을 PC의 8000번 포트로 전달합니다. 에뮬레이터를 다시 연결한 후에는 포트 연결 명령을 다시 실행합니다.

### 소스에서 앱 실행

Android Studio에서 `android-app/practice-pleos` 폴더를 열고 Gradle 동기화를 완료한 뒤, Pleos Connect 에뮬레이터를 선택해 `app`을 실행합니다. Windows에서 한글 경로로 Gradle 오류가 발생하면 저장소를 영문 경로에 둡니다. SDK 경로는 각 PC의 Android SDK 설치 위치에 맞춥니다.

## 테스트 환경 및 필요한 설정

| 항목 | 환경 및 설정 |
|---|---|
| 백엔드 | Windows, Python 3.11.9, FastAPI, SQLite |
| Android 개발 도구 | Android Studio, Android SDK 및 Platform-Tools 필요 |
| 앱 빌드 설정 | compileSdk 37, targetSdk 37, minSdk 26 |
| Pleos SDK | FusedLocation 1.1.3, SpeechToText 3.3.1, NaviHelper 3.0.5 |
| 서버 연결 | PC 서버 실행 및 `adb reverse tcp:8000 tcp:8000` 필요 |
| 녹음 시험 | 마이크 권한 허용 및 에뮬레이터의 PC 마이크 입력 설정 필요 |
| 위치·내비게이션 시험 | 호환되는 Pleos 서비스와 위치 입력이 제공되는 환경 필요 |

### Pleos Connect 에뮬레이터 버전 및 화면 규격

개발 PC에 설치된 로컬 AVD 설정 파일을 확인한 기준입니다. 팀원의 별도 시연 환경과 동일한지는 추가 확인이 필요합니다.

| 항목 | 로컬 개발 환경 |
|---|---|
| AVD 이름 및 버전 표기 | Pleos Connect v2.1.0 |
| 시스템 이미지 | Android 14 / API 34, pleos_car_emulator, x86_64 |
| 화면 해상도 | 가로 2560 × 세로 1440 px |
| 화면 밀도 | 160 dpi |
| Android Emulator 도구 버전 | 37.2.13 (Pleos 이미지 버전과 별도) |

백엔드 저장·조회·분석과 합성 경로 기반 재방문 검색을 검증했습니다. APK는 팀원 시험에서 마이크 녹음 및 서버 연결을 확인했습니다. 실제 Pleos 위치 업데이트, 내비게이션 경로 표시 및 앱과 서버의 전체 흐름은 추가 검증이 필요합니다. 상세 범위는 [APK 안내](apk/README.md)에 기록했습니다. 저장된 요약문을 합성 음성으로 읽는 TextToSpeech 연동은 향후 개발 계획입니다.

## APK 파일 위치

저장소의 [`apk/Re-load-debug.apk`](apk/Re-load-debug.apk)에 디버그 APK가 있습니다. GitHub 파일 화면에서 다운로드 버튼 또는 **View raw**를 선택합니다. 설치·실행 방법과 확인하지 못한 기능은 [`apk/README.md`](apk/README.md)를 참고합니다.

## 기술 문서

- [API 계약](docs/api-contract.md)
- [데이터 계약](docs/data-contract.md)
- [B 백엔드 검증](docs/b-verification.md)
