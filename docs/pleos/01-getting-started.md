# Pleos Connect 앱 개발 시작 레퍼런스 (A: 시작하기 / 환경 / 에뮬레이터 / 튜토리얼 / 릴리즈 노트 / 용어)

> 원본: https://document.pleos.ai 공식 문서 (2026년 10월 기준 수집본). 각 섹션 하단 또는 상단에 출처 URL을 표기했습니다.
> 원문에 없는 내용은 추가하지 않았으며, 원문끼리 불일치하거나 불명확한 부분은 **[주의]**로 표시했습니다.

---

## 0. 한눈에 보기 (Quick Facts)

| 항목 | 값 |
| --- | --- |
| 플랫폼 | Pleos Connect = Android Automotive OS 기반 SDV 인포테인먼트(IVI) 플랫폼 |
| 개발자 포털 | Pleos Playground: `https://developers.pleos.ai` (v2.7.0부터 `pleos.ai/playground` → `developers.pleos.ai`로 변경) |
| Maven 저장소 | `https://nexus-playground.pleos.ai/repository/maven-releases/` |
| Vehicle SDK 좌표 | `ai.pleos.playground:Vehicle:<version>` (최신 릴리즈 2.3.2) |
| minSdk | **API 28 이상** (차량 사이버 보안 요건: 서명 키 로테이션·lineage 지원) |
| Emulator system image | API 34, `Pleos Connect v2.1.0` (Android 14) |
| Gradle | 8.0 이상 |
| 언어 | Java, Kotlin |
| AVD 필수 설정 | Boot = **Cold boot**, RAM = **4 GB** (Quick boot 시 VHAL 사용 불가) |
| Virtual Console | `adb forward tcp:8088 tcp:8088` → `http://127.0.0.1:8088/` (Emulator v2.1.0 이상) |
| 차량 권한 네임스페이스 | `pleos.car.permission.*` (예: `CAR_DOORS`, `CONTROL_CAR_DOORS`) |

---

## 1. 전체 개발 흐름

1. Pleos Playground 회원 가입 → 프로젝트 생성 → SDK 사용 신청 (2장)
2. Android Studio + Pleos Connect Emulator 설치·AVD 생성 (4장)
3. Android 프로젝트 생성, Maven 저장소/Vehicle SDK 의존성 추가, 권한 선언, 첫 API 호출 (3장)
4. Pleos Connect Virtual Console로 차량 상태를 조작하며 테스트 (5장)
5. Vehicle SDK Sample App으로 Capability/Get/Set/Listener 흐름 학습 (6장)
6. 이후 문서 흐름(원문 "다음 단계"):
   1. [앱 계획 및 설계 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro) – 화면, 주행 중 사용성, 성능, 개인정보 보호 기준
   2. [앱 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/intro)
   3. [SDK 및 API 문서](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/)
   4. [Vehicle SDK 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission), [Capability API](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability), [Get API](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/get-property)
   5. [Vehicle SDK Sample App 사용 가이드](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app)
   6. [Virtual Console 사용 가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide)
   7. [앱 등록 및 배포 가이드](https://document.pleos.ai/docs/pleos-only/distribute/app-submission-guide) – Pleos App Market 제출

출처: https://document.pleos.ai/docs/pleos-only/getting-started

---

## 2. Pleos Playground: 회원 가입, 프로젝트 생성, SDK 사용 신청

출처: https://document.pleos.ai/docs/pleos-only/create-my-project

### 2.1 회원 가입
이미 Pleos 계정(= 현대자동차 통합 계정, Pleos Account)이 있으면 생략.
1. [Pleos Playground](https://developers.pleos.ai) 접속
2. 우측 상단 **로그인** 선택
3. 로그인 화면 하단 **Pleos 계정 만들기**
4. 이용약관 동의 후 계정 정보 입력
5. 가입 완료 후 Playground 로그인

### 2.2 프로젝트 생성
프로젝트 생성 후 팀 멤버 추가 등 팀 관리와 SDK 사용 권한 신청이 가능해집니다.
1. Playground 로그인
2. 상단 GNB에서 **Workspace > My Project**
3. **프로젝트 생성** 클릭
4. 입력 항목
   - **프로젝트 이름**: 앱 이름 또는 과제 이름 (나중에 변경 가능)
   - **개발사명**: 소속 회사명
   - **카테고리**: 개발할 앱 카테고리 (앱 개발 과정에서 필수 정보이므로 처음에 정의 권장)
   - **애플리케이션 설명**: 간단한 설명
5. **생성** 클릭

### 2.3 프로젝트 정보 화면 (자격 증명/키)

| 항목 | 용도 |
| --- | --- |
| 프로젝트 이름 | 이 화면에서 수정 가능 |
| Application 상세 | 생성 시 입력 정보 |
| **키 정보 > 프로젝트 ID(Key)** | 앱 심사 요청 시 **Pleos App Market Console**에 입력 |
| **SDK > 클라이언트 ID** | 서버 연결이 필요한 SDK 사용 시 필요 (TextToSpeech, SpeechToText) |
| **SDK > 클라이언트 시크릿** | 서버 연결이 필요한 SDK 사용 시 필요 (TextToSpeech, SpeechToText) |
| 테스트 정보 > CRN | **Pleos Connect Emulator 2.1.0 이후부터는 CRN 입력 불필요** |

> [주의] 클라이언트 ID/시크릿을 코드에서 어떻게 주입하는지(예: Manifest meta-data, initialize 인자 등)는 이 폴더의 문서에 나와 있지 않습니다. TTS/STT SDK 개별 문서를 확인해야 합니다.

### 2.4 SDK 사용 신청
1. 프로젝트 정보 화면 좌측 사이드바 **SDK** 선택
2. 하위 메뉴에서 사용할 SDK 선택
3. SDK 상세 화면에서 상태 확인

| 상태 | 의미 | 해당 SDK(원문 기준) |
| --- | --- | --- |
| **준비 중** (`Comming Soon` 배지) | API 제공 준비 중, 신청 버튼 없음. "신청 필요"로 바뀐 후 진행 | FusedLocation, TextToSpeech, SpeechToText, Vehicle Control SDK |
| **사용 가능** | 신청 없이 사용 가능 또는 승인 완료 | Vehicle State, NaviHelper (신청 없이 바로 사용) |
| **신청 필요** | **신청** 클릭 → 정보 입력 후 제출 | – |
| **검토 중** | 신청 후 승인 전 | – |
| **반려됨** | 신청 반려 | – |
| **제휴 계약 필요** | 웹 신청 불가. 서비스 계약 담당자에게 별도 문의해 제휴 계약 후 사용 | – |

> [주의] 원문에서 Vehicle SDK가 "Vehicle State"/"Vehicle Control"로 나뉘어 표기되어 있습니다. 즉 상태 조회 계열은 바로 사용 가능, 제어 계열은 "준비 중"으로 기재되어 있으나, 이것이 Maven 아티팩트(`ai.pleos.playground:Vehicle`) 단위와 어떻게 대응하는지는 문서에 설명이 없습니다.

다음 단계: [개발 환경 구성](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)

---

## 3. 빠른 시작: Android 프로젝트 + Vehicle SDK 첫 API 호출

출처: https://document.pleos.ai/docs/pleos-only/getting-started

### 3.1 사전 준비
- Android Studio
- Android SDK API 34
- 프로젝트 `minSdk` API 28 이상
- Pleos Connect Emulator system image

### 3.2 Android 프로젝트 만들기
**File > New > New Project**
1. **Templates**에서 **Car** 또는 **Automotive** 카테고리 선택 (Android Studio 버전에 따라 이름 다름)
2. **Empty Activity** 선택
3. 프로젝트 이름/위치 지정
4. 언어 **Kotlin**, **Minimum SDK** API 28 이상
5. **Finish** → `app` 모듈이 정상 빌드되는지 먼저 확인

### 3.3 Emulator 설치(요약)
1. **Tools > SDK Manager > SDK Update Sites** → **Add**
2. Name: `Pleos Connect System Image`, URL: Pleos Connect Emulator 다운로드 URL (구체 URL은 4.2절)
3. **SDK Platforms** → **API 34**의 Pleos Connect system image 설치 → Android Studio 재시작
4. **Tools > Device Manager > Create device** → **Automotive** → Pleos Connect 디바이스
5. 고급 설정: **Boot option = Cold boot**, **RAM = 4 GB**, API 34 Pleos Connect image 선택
6. Device Manager에서 AVD 실행

### 3.4 Maven 저장소 등록 (`settings.gradle.kts`)

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
    }
}
```

### 3.5 SDK 의존성 추가 (`app/build.gradle.kts`)

```kotlin
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.1")
}
```

이후 **Sync Now**. 의존성을 찾지 못하면 Maven 저장소 접근 권한과 SDK 버전을 확인.

> [주의] 빠른 시작 문서는 `Vehicle:2.3.1`을 예시로 쓰지만, SDK 릴리즈 노트(2026-10-19)와 Sample App 기준 버전은 **Vehicle SDK 2.3.2**입니다. 최신을 쓰려면 `ai.pleos.playground:Vehicle:2.3.2`로 맞추는 것이 자연스러우나, 2.3.2의 Maven 좌표가 동일하다는 것은 원문에 명시되어 있지 않습니다.
> [주의] "Maven 저장소 접근 권한"이 언급되지만, 저장소 인증(username/password, token) 설정 방법은 이 폴더 문서에 없습니다. 위 스니펫에는 credentials 블록이 없습니다. 자세한 것은 [Importing Vehicle SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup) 참조.

### 3.6 차량 권한 선언 (`app/src/main/AndroidManifest.xml`)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.CAR_DOORS" />
    <application
        ...>
        ...
    </application>
</manifest>
```

API별 권한: [Vehicle SDK permissions](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)

### 3.7 첫 번째 API 호출

`Vehicle(context)` 생성 → `initialize()` → `getDoor()`로 도메인 객체 획득 → 도어 위치 조회.

```kotlin
val vehicle = Vehicle(context)
vehicle.initialize()

val door = vehicle.getDoor()
door.getCurrentDoorPosition(
    area = DoorArea(DoorValue.ROW_1_LEFT),
    onComplete = { pos: Int? ->
        // Use the current door position.
    },
    onFailed = { e: Exception ->
        // Handle the Vehicle SDK error.
    }
)

// Do not call release() here.
// Call release() when the app no longer needs Vehicle SDK (for example, in onDestroy()).
```

- `onComplete` / `onFailed`는 **비동기 콜백**.
- `vehicle.release()`는 콜백 처리 완료 후 또는 SDK를 더 이상 쓰지 않는 생명주기 시점(예: `onDestroy`)에서 호출.
- 예제는 Activity 생명주기/화면 코드는 포함하지 않음.

### 3.8 에뮬레이터에서 확인 및 점검 목록
1. Emulator 완전 부팅 확인 → 2. 실행 대상 기기로 선택 → 3. **Run** → 4. 도어 위치 조회 결과 처리 확인

값을 못 읽을 때:
- `minSdk` ≥ 28인지
- `pleos.car.permission.CAR_DOORS` 선언 여부
- SDK 의존성 동기화 여부
- 에뮬레이터 정상 실행 여부
- `onFailed`로 전달된 예외 메시지

---

## 4. 개발 환경 및 Pleos Connect Emulator (Connect SDK Emulator) 설치

출처: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator

### 4.1 소프트웨어 개발 환경

| 구분 | 버전 |
| --- | --- |
| Minimum Android SDK | **Android API 28 이상** |
| Gradle | 8.0 이상 |
| 개발 언어 | Java, Kotlin |
| Pleos Connect Emulator | 2.1.0 |
| Pleos Connect Virtual Console | 지원 최소 Emulator 버전 2.1.0 |

> minSdk API 28 이상은 차량 사이버 보안 요건(사이닝 키 로테이션·리니지 지원)에 따른 필수 요구사항입니다.

Prerequisite:
1. Android Studio 최신 버전 설치 ([다운로드](https://developer.android.com/studio?hl=ko))
2. Android Emulator [시스템 요구 사항](https://developer.android.com/studio/run/emulator?hl=ko#requirements) 확인

### 4.2 Emulator 이미지를 Android Studio에 추가

> 이전 버전의 Pleos Connect Emulator가 설치되어 있으면 **먼저 삭제**한 후 진행.

1. **Menu > Tools > SDK Manager**
2. **SDK Update Sites** 탭
3. 좌측 상단 **+** 클릭
4. Pleos Connect SDK site 추가
   - Name: `Pleos Connect System Image`
   - URL for **Mac**: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
   - URL for **Windows/Linux**: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
5. **OK**
6. **SDK Platforms** 탭
7. **Android API Level 34**의 **Pleos Connect system Image** 체크
8. 하단 **Show Package Details** 체크박스 활성화
9. **OK** → 이미지 설치 진행
10. 완료 후 **Finish**, Android Studio 재시작

> [주의] Mac URL은 파일명이 `arm64`로, Apple Silicon 기준으로 보입니다. Intel Mac에 대한 별도 안내는 원문에 없습니다.

### 4.3 AVD 생성

1. Android Studio 시작(재시작 안 했다면 재시작)
2. **Menu > Tools > Device Manager** (구버전: **AVD Manager**)
3. **Create device** (구버전: **Create Virtual Device**)
4. 왼쪽 **Form Factor**: `Automotive`
5. `Pleos Connect v2.1.0` 선택 → **Next**
6. **Configure virtual device** → **Advanced Settings** 탭 (구버전: **Show Advanced Settings**)
7. **Startup > Default boot** = `Cold`
   (구버전: **Emulated Performance > Graphics** = `Hardware`, **Boot option** = `Cold boot`)
8. **Emulated Performance > RAM** = `4 GB`
9. **Device** 탭 → **Select system image > API** = `API 34` → 테이블에서 `Pleos Connect v2.1.0 APIs` 선택 → **Finish**

### 4.4 실행
**Menu > Tools > Device Manager**에서 생성한 AVD 실행.

### 4.5 Troubleshooting
- [Android Emulator 알려진 문제 해결](https://developer.android.com/studio/run/emulator-troubleshooting?hl=ko)
- [Android Studio / AGP 알려진 문제](https://developer.android.com/studio/known-issues?hl=ko)

---

## 5. Pleos Connect Emulator 설정

출처: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator

Emulator는 Pleos Connect SDK로 구현한 앱의 테스트 환경을 제공하며, 실제 차량처럼 테스트할 수 있도록 **Pleos Connect Virtual Console**을 제공합니다.

### 5.1 그래픽 카드 요구 사항 (GPU 필수)

| 구분 | 사양 |
| --- | --- |
| 권장 | NVIDIA RTX 4060/5060 Laptop 8GB 이상 / AMD Radeon RX 7700S·RX 7600M XT 8GB급 이상 외장 GPU |
| 최소 | NVIDIA GeForce RTX 3060 Laptop GPU 6GB 이상 / AMD Radeon RX 6600M 8GB 또는 RX 6700S 8GB 이상 |

> [주의] Apple Silicon Mac의 GPU 요건은 원문에 별도 언급이 없습니다.

### 5.2 언어 설정
기본 언어는 한국어. 영어 전환: **차량 설정 > 일반 설정 > 언어/Language** → `English`.

### 5.3 마이크 접근 설정 (STT 테스트용)
1. 에뮬레이터 패널에서 **Extended Controls** 열기
2. **Microphone** 메뉴에서 오디오 입력 수신 설정 (Android 공식 문서: [Extended controls](https://developer.android.com/studio/run/emulator-extended-controls?hl=ko#standalone-window))

### 5.4 Cold boot 필수
AVD 추가 시 `Boot options`를 반드시 `cold boot`로. **`quick boot`이면 VHAL을 이용할 수 없음.**

### 5.5 Bluetooth 동글
이 문서가 다루는 Emulator 버전에서는 AVD Bluetooth 동글 연동 **미지원** (Emulator v2.1.0 릴리즈에서 "블루투스 동글 연동 제공 종료").

---

## 6. Pleos Connect Virtual Console (Vehicle Emulator)

출처: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator

Pleos Connect Emulator 환경에서 차량 핵심 기능을 재현·시뮬레이션하는 개발·테스트 도구. 확인 가능한 것:
- 차량 속도, 도어, 조명 등 상태 변경
- 주행 및 차량 이벤트 시나리오 재현
- 앱 화면에 반영되는 실시간 차량 상태
- 차량 제어 명령 정상 동작
- 반복 테스트 및 데모 시나리오

> 지원 최소 Emulator 버전: **v2.1.0**

### 6.1 실행 방법

```bash
adb forward tcp:8088 tcp:8088
```

PC 브라우저에서:

```text
http://127.0.0.1:8088/
```

### 6.2 Emulation Controls (Domain별)

| Domain | 설명 |
| --- | --- |
| Vehicle | 차량 알림 표시/숨김 상태 시뮬레이션 |
| Motion | 기어, 브레이크, 스티어링 휠 각도, 주행 거리, 스티어링 휠 열선 단계 |
| ADAS | 크루즈 제어 준비/활성 상태, 목표 속도 |
| Convenience | 도어·창문·트렁크, 시트, 조명, 와이퍼, ETCS 시나리오, 글로브 박스 |
| Safety | 주차 브레이크, 안전벨트, ABS 등 경고등 꺼짐/켜짐/점멸 |
| Energy | 주행 가능 거리, 배터리 용량·충전 상태, 충전 전류·목표 충전량·충전 포트 |
| Climate | 공조 전원, 온도, 풍량, 송풍 방향, 내기순환, 제상, 좌석 열선·통풍, 공기 청정 |

전체 카드·설정 범위는 [Virtual Console 사용 가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide) 참조(이 폴더에는 미포함).

### 6.3 테마 및 시점
- 기어가 **P**일 때 왼쪽 상단 테마 버튼으로 **Highway** / **Forest** 테마 전환
- Highway 테마에서 오른쪽 상단 **First person / Third person** 시점 전환
- 테마 변경은 차량 테스트 기능에 영향 없음

### 6.4 접속 불가 시 점검 순서
1. Emulator 실행 중인지
2. Virtual Console 지원 Emulator 이미지(v2.1.0+)인지
3. `adb devices`로 ADB 연결 확인
4. `adb forward tcp:8088 tcp:8088` 재실행
5. PC 8088 포트를 다른 프로세스가 사용 중인지
6. `http://127.0.0.1:8088/` 재접속

---

## 7. 튜토리얼: Vehicle SDK Sample App

출처: https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app

차량 기능을 탐색하고 Vehicle SDK의 사용 흐름(Capability 확인 → 조회 → 변경 → 알림 구독/해제)을 익히는 Android 예제 앱. **Pleos Vehicle Service가 포함된 Pleos Connect 전용 AVD**에서 사용(일반 폰/기본 에뮬레이터 이미지 X).

> [주의] Sample App 소스/APK 다운로드 위치(저장소 URL)는 이 문서에 명시되어 있지 않습니다.

### 7.1 기준 버전

| 항목 | 기준 |
| --- | --- |
| Sample App | `1.0.0-sdk35` |
| Vehicle SDK | `2.3.2` |
| Vehicle Service | `2.3.22` |
| AVD | `Pleos_Connect_v2.1`, Android 14 |
| JDK | 17 |
| Gradle wrapper | `8.10.2` |
| Android Gradle Plugin | `8.8.2` |
| Kotlin | `2.0.0` |
| compileSdk / targetSdk | 35 |
| Sample App minSdk | 23 |

- Sample App 자체 `minSdk`는 23이지만, **Pleos Playground에 배포할 제품 앱은 API 28 이상**(signing key rotation/lineage) 적용 필요.
- 상단 버전 표시는 호환성 보장이 아님 → [API 호환성 및 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability) 함께 확인.

### 7.2 소스 빌드와 설치
Vehicle SDK 버전은 `gradle/libs.versions.toml`에서 관리. 프로젝트 루트에서:

```bash
./gradlew assembleDebug

./gradlew installDebug
```

- APK 경로: `app/build/outputs/apk/debug/`
- 설치 후 Launcher에서 **Vehicle SDK Sample** 실행
- SDK 버전을 바꾸면 `PublicApiPolicyTest`의 public API 수 기준을 해당 SDK 버전에 맞춰야 함(이전 기준이 남으면 테스트 실패. 빌드/Service 연결 성공 여부와는 별개).

### 7.3 화면 구성
- 진입 화면: 제목 `Vehicle Playground`, 상단에 SDK/Vehicle Service 버전·초기화 상태. 좌측 `BROWSE VEHICLE APIS` / `VEHICLE DOMAINS`(분류, 숫자=기능 수), `Search features`, 기능 카드(예: `Door Position`).
- `SDK initialized` = 초기화 호출이 예외 없이 반환됐다는 뜻일 뿐, Vehicle Service 연결 완료나 기능 가용성을 의미하지 않음.
- 상세 화면 영역:

| 레이블 | 역할 |
| --- | --- |
| `Vehicle support` | 지원 상태와 선택 영역의 값 범위. `Check support`로 재확인 |
| `Current value` | 마지막 조회/알림 값. `Area` 선택, `Refresh`로 재조회 |
| `Change value` | 변경값 지정. `Apply change`를 눌러야 요청 전송(확인창 없음). 변경 API 없는 기능은 미표시 |
| `Live updates` | `Start updates` 구독 / `Stop updates` 해제 |
| `API activity` | 호출 API와 최신 결과. `Expand`/`Collapse` |
| 왼쪽 화살표 | 목록 복귀 (구독 해제 아님) |

`Supported` = 기능 지원 상태, `Reference` = SDK 직접 호출 기준 예제.

### 7.4 용어

| 용어 | 의미 |
| --- | --- |
| Domain | Door, Window 등 차량 기능 분류 |
| Area | `ROW_1_LEFT`처럼 조회/제어 대상 영역 |
| Capability | 지원 여부, 읽기·쓰기 접근 방식, 영역별 값 범위 |
| Read / Write | 현재 상태 조회 / 상태 변경 요청 |
| Listener | 차량 상태 변경 알림을 받는 객체 |

### 7.5 Door Position 기준 예제 – 화면 ↔ API 매핑

| 작업 (화면) | typed API |
| --- | --- |
| 지원 범위 확인 (`Vehicle support`) | `checkDoorPositionCapabilities` |
| 현재 상태 조회 (`Current value`) | `getCurrentDoorPosition` |
| 변경 알림 구독 (`Live updates`) | `registerDoorPosition` |
| 상태 변경 요청 (`Change value`) | `setDoorPosition` |
| 변경 알림 해제 (`Live updates`) | `unregisterDoorPosition` |

흐름과 주의점:
- **지원 범위 확인**: 진입 시 자동 확인 후 성공하면 현재값 1회 자동 조회. 예시 범위 `0..1`이지만 다른 환경에서 같다고 가정 금지. 실패/범위 불명 시 변경 요청 금지. `Unsupported`에는 실제 미지원과 확인 요청 실패 둘 다 포함될 수 있음.
- **조회**: `null`/`unavailable` = 사용 가능한 현재값을 못 받음(권한 누락으로 단정하지 말 것).
- **API activity의 `Complete`**: 조회·변경은 결과 콜백 수신, Listener 등록·해제는 SDK 호출이 예외 없이 반환됨을 의미(Service 측 완료 응답 아님).
- **구독**: `Start updates` → `Receiving vehicle updates`. 실제 알림 수신 시 `registerDoorPosition` 응답에 `Door update: <value>` 표시.
- **변경**: `Closed (0)` / `Open (1)` 선택 → `Apply change`. `setDoorPosition` 결과의 값은 **요청한 값**이지 새로 조회한 상태가 아님. 실제 반영은 `Refresh` 재조회 또는 Listener 알림으로 확인.
- **해제**: 등록했던 영역을 선택한 상태에서 `Stop updates`. 화면 이동/Area 변경만으로는 해제되지 않음. 다른 영역으로 해제 시 오류 가능. 다른 영역으로 `Start updates` 시 기존 구독 해제 후 재등록.

### 7.6 제품 앱 적용 원칙
- 필요한 typed API를 직접 사용, **실제 사용 API의 최소 권한만 선언** (Sample App 권한 목록 복사 금지)
- Listener는 **등록 시 사용한 동일 객체와 area**로 해제
- SDK 사용 종료 시 `Vehicle.release()`

| 개발 단계 | 문서 |
| --- | --- |
| Maven 저장소·의존성 | [Vehicle SDK 연동 가이드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup) |
| 권한·Manifest | [권한 선언 가이드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission) |
| 초기화·도메인 객체·해제 | [도메인 객체 획득](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object) |
| Capability/Get/Set | [네 가지 API 타입](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object) |
| Listener 등록/해제 | [Callback 가이드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/callback) |
| API 명세 | [Vehicle SDK API 상세](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/entry) |

### 7.7 권한
- 위치: `app/src/main/AndroidManifest.xml`
- Door Position: 지원 확인·조회·Listener = `CAR_DOORS`, 변경 = `CONTROL_CAR_DOORS`
- 이 권한들은 일반 Android 위험 권한처럼 **런타임 승인 팝업 방식이 아님** — Manifest 선언 확인 + 접근 방식·영역 지원 여부 확인.

Sample App 전체 public permission (현 release Manifest 기준, `PRIVATE_*` 제외):

```xml
<!-- Read / state -->
<uses-permission android:name="pleos.car.permission.CAR_CLIMATE" />
<uses-permission android:name="pleos.car.permission.CAR_CONNECT_VENDOR_CONTROL" />
<uses-permission android:name="pleos.car.permission.CAR_DOORS" />
<uses-permission android:name="pleos.car.permission.CAR_DRIVE" />
<uses-permission android:name="pleos.car.permission.CAR_ENERGY" />
<uses-permission android:name="pleos.car.permission.CAR_INFO" />
<uses-permission android:name="pleos.car.permission.CAR_LIGHTS" />
<uses-permission android:name="pleos.car.permission.CAR_MILEAGE" />
<uses-permission android:name="pleos.car.permission.CAR_MIRRORS" />
<uses-permission android:name="pleos.car.permission.CAR_POWERTRAIN" />
<uses-permission android:name="pleos.car.permission.CAR_SAFETY" />
<uses-permission android:name="pleos.car.permission.CAR_SEATS" />
<uses-permission android:name="pleos.car.permission.CAR_SPEED" />
<uses-permission android:name="pleos.car.permission.CAR_TOLL" />
<uses-permission android:name="pleos.car.permission.CAR_TIRES" />
<uses-permission android:name="pleos.car.permission.CAR_WINDOWS" />
<!-- Control -->
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_CLIMATE" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_DOORS" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_DRIVE" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_ENERGY" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_INFO" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_LIGHTS" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_MIRRORS" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_SAFETY" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_SEATS" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_TOLL" />
<uses-permission android:name="pleos.car.permission.CONTROL_CAR_WINDOWS" />
<uses-permission android:name="pleos.car.permission.READ_CAR_DISPLAY_UNITS" />
<uses-permission android:name="pleos.car.permission.READ_CAR_STEERING" />
<uses-permission android:name="pleos.car.permission.READ_ADAS_STATES" />
<uses-permission android:name="pleos.car.permission.READ_WINDSHIELD_WIPERS" />
<uses-permission android:name="pleos.car.permission.CONTROL_WINDSHIELD_WIPERS" />
```

### 7.8 문제 해결

| 증상 | 확인/조치 |
| --- | --- |
| `Initialization failed` / Service 연결 오류 | 전용 AVD와 Vehicle Service 설치 상태 확인 |
| `Vehicle support` = `Unsupported` | 실제 미지원 또는 확인 실패. `API activity` 오류·권한·연결 확인 후 `Check support` |
| 현재값 `null`/`unavailable` | 지원 여부·선택 영역 확인 후 재조회 (권한 누락으로 단정 금지) |
| 조회 `Failed` | 오류 메시지, 읽기 권한, 영역, Service 상태 |
| 변경 거부/범위 오류 | 쓰기 권한, 쓰기 접근 가능 여부, 영역 값 범위 |
| 변경 완료인데 조회값 다름 | 요청 완료 ≠ 실제 상태. 재조회·알림 확인 |
| 구독했는데 알림 없음 | 등록 영역과 오류 메시지 확인 |
| 해제 시 등록 영역 오류 | 등록했던 영역으로 되돌린 뒤 `Stop updates` (코드에선 동일 Listener·area로 해제) |

- 기능은 `Supported`인데 특정 영역 조회 실패 예: Door Lock `ROW_1_LEFT` → `area ID: 0x1 not supported`. 기능 전체 지원과 영역별 가용성을 구분해야 함.

---

## 8. 릴리즈 노트

### 8.1 최신 버전 요약

| 대상 | 최신 버전 | 릴리즈일 |
| --- | --- | --- |
| Vehicle SDK | **v2.3.2** | 2026-10-19 |
| NaviHelper | **v3.0.5** | 2026-10-19 |
| TextToSpeech | **v3.3.1** | 2026-10-19 |
| SpeechToText | **v3.3.1** | 2026-10-19 |
| FusedLocation SDK | **v1.1.3** | 2026-10-19 |
| Pleos Connect Emulator | **v2.1.0** (정식 릴리즈) | 2026-10-19 |
| Pleos Playground | **v2.7.0** | 2026-10-29 |
| (참고) Vehicle Service (Sample App 기준) | 2.3.22 | – |
| (참고) Vehicle SDK Sample App | 1.0.0-sdk35 | – |

> [주의] 빠른 시작 예제의 의존성은 `Vehicle:2.3.1`입니다(3.5절 참조). Vehicle 외 SDK(NaviHelper, TTS, STT, FusedLocation)의 Maven 좌표는 이 폴더 문서에 없습니다. 패키지명으로 `ai.pleos.playground.tts.*`, `ai.pleos.playground.stt.*`가 언급됩니다.

### 8.2 Pleos Connect SDK 릴리즈 노트 (2026-10-19)
출처: https://document.pleos.ai/docs/pleos-only/release-note/sdk-release-note

#### Vehicle SDK v2.3.2

**Breaking change**

| 대상 | 변경 | 필요한 대응 |
| --- | --- | --- |
| `CruiseControl` | 기존 상수 `OFF`, `READY`, `ACTIVE` 제거. 최신 상태: `OTHER`, `ENABLED`, `ACTIVATED`, `USER_OVERRIDE`, `SUSPENDED`, `FORCED_DEACTIVATION_WARNING` | 상수 참조·상태 분기·저장된 enum 이름 수정. 단순 1:1 이름 변경으로 처리하지 말 것 |
| `checkHvacFanSpeedCapabilities` | 콜백 값 타입 `Map<SeatArea, List<Int>>` → `Map<SeatArea, AreaBounds<Int>>` | 목록 처리 → 최소·최대 범위 처리로 수정 |
| `FanDirectionType` | 3개 → `UNKNOWN`·복합 풍향 포함 8개로 확장, 순서 변경 | 모든 값 나열 `when`에 새 상태 처리. ordinal 저장 시 마이그레이션: `FACE 0→1`, `FLOOR 1→2`, `DEFROST 2→4` |
| `DoorArea`, `MirrorArea`, `SeatArea`, `TurnSignalArea`, `WheelArea`, `WindowArea` | 내용 기반 `equals`/`hashCode` 추가 | 객체별로 다른 키라고 가정한 Map·Set·비교 로직 점검 |

**New API**
- Capability 조회: `checkVINInfoCapabilities`, `checkSmartGlassLevelCapabilities` 등
- 상태 조회·구독: `getCurrentEvEfficiency`, `registerEvBatteryStateOfCharge`, `getCurrentSmartGlassLevel` 등 (EV/Fuel 효율, 배터리 충전율, 연료량, ETCS 음성 안내 볼륨, Smart Glass level)
- 차량 정보: `getVINInfo` (VIN 조회)
- 신규 모델: `CapabilityConfig`, `TripStatistics`, `TelltaleData`, `Telltales`

**Changed API**
- `checkDoorLockCapabilities`, `checkWindowPositionCapabilities` 등 기존 Capability API: `CapabilityConfig` 등을 함께 전달하는 새 콜백 오버로드 추가, 이전 콜백 형태 deprecated(WARNING 수준, 삭제 아님). 새 오버로드 권장
- `registerHvacTemperature`, `registerHvacTemperatureCurrent`(및 해제 함수): `HvacTemperatureValueListener`를 받는 오버로드 추가 (기존 `HvacTemperatureListener` 유지)
- `EvBatteryLevelListener`, `EvChargePortOpenListener`, `RangeRemainingListener`: 문서에서 숨김(타입·콜백은 유지)

**Deprecated API**
- `getCRNInfo` → `getVINInfo`, `checkCRNInfoCapabilities` → `checkVINInfoCapabilities`
- `checkWheelAreaCapabilities` → `CapabilityConfig`와 `List<WheelArea>`를 받는 `checkLowTirePressureCapabilities`

**Shutdown(삭제) API**
- `getHvacTemperatureValueSuggestion` 메서드 제거
- `VehicleSpeedListener` 및 `onSpeedUpdated` 제거

#### NaviHelper v3.0.5
- Breaking
  - `onNavigationStatus` 인자: `RouteDriving` → `NavigationStatus`
  - 좌표 인자 순서 `longitude, latitude` → `latitude, longitude` (`BookmarkInfo`, `CurrentLocationInfo`, `DestinationInfo`, `RecentDestinationInfo`, `RequestWaypointInfo`, `RouteInfo`, `WaypointInfo`)
  - `DestinationInfo`, `WaypointInfo`: `totalDistance/totalTime` → `remainDistance/remainTime`
- New: `requestTrip`, `requestRouteWithWaypoints`, `requestNavigationScreen`; `initialize(onConnected, onFailed)`; 리스너 `onCautionPointInfo`, `onRoutingCompleted`, `onTripInfo`
- Changed: 모델 필드 확장(POI provider, 좌표/거리, Trip/Traffic/Waypoint), Enum 추가·변경(`CameraType`, `CautionPointType`, `NavigationStatus`, `RoutePointType`)
- Deprecated: `onRouteEnded` → `onRoutingCompleted`
- Shutdown: `FindRouteResult`, `onUserDefinedRoute` 제거; `CameraType.SPEED_CAMERA_SECTION` 제거 → `SPEED_CAMERA_SECTION_START`, `SECTION_SPEED_ALERT`, `SPEED_CAMERA_SECTION_END`

#### TextToSpeech v3.3.1
- Breaking: 오류 콜백 인자 `String` → `ErrorType`
- New: `TextToSpeech.initialize(listener: OnInitializeListener)`, `OnInitializeListener`, `TextToSpeech.version`, `ErrorType` enum
- Changed: `EventListener.onError(errMsg: String)` → `onError(errorType: ErrorType)`; `OnServerConnectionListener.onFailed(msg: String)` → `onFailed(errorType: ErrorType)`
- Shutdown: `ai.pleos.playground.tts.constant.ServerType` 삭제; `ai.pleos.playground.tts.utils.Log`, `...utils.StringUtils` 공개 API 노출 제거

#### SpeechToText v3.3.1
- Breaking: 오류 콜백 인자 `String` → `ErrorType`
- New: `SpeechToText.initialize(listener: OnInitializeListener)`, `OnInitializeListener`, `SpeechToText.version`, `ErrorType`, `InferenceSource` (`ON_DEVICE`, `SERVER`)
- Changed: `ResultListener.onError(errMsg: String)` → `onError(errorType: ErrorType)`; `OnServerConnectionListener.onFailed(msg: String)` → `onFailed(errorType: ErrorType)`
- Shutdown: `ai.pleos.playground.stt.api.DevelopmentOnly` annotation 삭제; `ai.pleos.playground.stt.utils.Log`, `...utils.StringUtils` 공개 노출 제거

#### FusedLocation SDK v1.1.3
- Breaking: `FusedLocationListener.onChanged: LocationData` → `LocationData?`; `FusedLocationListener.onError(exception: Exception)` 필수 구현 추가
- New: `addProximityAlert`, `removeProximityAlert`; `getLastKnownLocation`, `isSupported`, `showLocationAccessBlockedPopup`; Proximity Alert Request/Event/Transition 모델 및 전용 Listener

### 8.3 Pleos Connect Emulator 릴리즈 노트 – v2.1.0 (2026-10-19, 정식)
출처: https://document.pleos.ai/docs/pleos-only/release-note/emulator-release-note

- New
  - **Pleos Connect Virtual Console** (도메인별 차량 상태/이벤트 설정 및 앱 반응 검증) – [가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide)
  - **폼팩터 테스트** 기능 (화면 크기/해상도/레이아웃 검증) – [가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide)
- Improvements: 탑재 SDK 버전 업데이트로 API 지원 범위 확대
- Removed: **LLM SDK** 제공 종료, **CRN** 제공 종료, **블루투스 동글 연동** 제공 종료

### 8.4 Pleos Playground 릴리즈 노트
출처: https://document.pleos.ai/docs/pleos-only/release-note/playground-release-note

**v2.7.0 (2026-10-29)**
- Playground 주소 변경: `pleos.ai/playground` → `developers.pleos.ai`
- Pleos Connect / Pleos App Market 메뉴를 신규 Pleos 브랜드 사이트로 연결, 푸터 개선
- 문서 검색 범위를 개발자 가이드 소제목까지 확대, 검색 결과에 문서 위치 표시

**v2.6.0 (2026-10-19)**
- New: 개발자 여정 가이드(**Discover > Developer Journey**), 상단 검색 아이콘 문서 검색
- Improvements: Pleos Connect 개발자 가이드를 개발 여정(시작 → 기획·설계 → 개발·연동 → 테스트 → 앱 등록)에 맞게 재구성. 주요 가이드: Virtual Console 셋업/사용, 폼팩터 테스트, Vehicle SDK Sample App
- Removed: **LLM SDK** 제공 종료 → Gleo AI 연동은 [Gleo Interface](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts) 이용

---

## 9. SDK 서비스 상태 페이지

출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-service-status

이 페이지는 실제 상태표가 아니라 안내문입니다: "요청하신 Pleos Connect SDK 문서는 현재 해당 경로에서 제공되지 않습니다." 최신 SDK 정보는 [Pleos Connect 공식 문서](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/) 참조. 제공 여부·향후 계획이 확정되지 않은 기능은 별도 안내 없음.

SDK별 신청 상태(준비 중/사용 가능 등)는 2.4절 참조.

---

## 10. InfoLink Emulator

### 10.1 설치 문서
출처: https://document.pleos.ai/docs/pleos-only/infolink-emulator/setup-infolink-emulator

> [주의] 수집 시점 기준 해당 URL은 **"페이지를 찾을 수 없습니다"(404)** 입니다. InfoLink Emulator의 설치 방법·이미지·URL은 이 폴더 문서에서 확인할 수 없습니다.

### 10.2 지역별 주행 규제 정책 (InfoLINK 추가 구현 항목)
출처: https://document.pleos.ai/docs/pleos-only/infolink-emulator/driving-restiction-policy

InfoLINK에서 동작하는 앱도 Pleos Connect 지역별 주행 규제 정책을 따라야 하며, 아래는 추가 구현 항목입니다.

**지역 구분**: 시스템 속성 `product.sw.target_region`
- 한국: `KOR` / 유럽: `EUR` / 북미: `USA`, `CAN`

주행 상황(Parking, Idling, Driving)과 지역에 따라 기능 사용 가능 여부가 달라짐(✅ 가능, ⛔ 제한, `음성한정`).

**QR 로그인 화면별 허용 범위** — `CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP_QR_SCAN` 해당(사유: 휴대전화 활용 유도). 아래는 주행 규제 적용 상태 기준.

| 모드 | 화면/시나리오 | 주행 중 잠금 처리 | 북미 | 유럽 | 한국 | 기타 |
| --- | --- | --- | --- | --- | --- | --- |
| 연결(블루투스) | BYOD > QR 코드 | QR 이미지를 다른 이미지로 대체 | ⛔ | ⛔ | ✅ | ⛔ |
| 연결(Wi-Fi) | Wi-Fi 연결 > URL QR 코드 | 콘텐츠 영역 차단 | ⛔ | ⛔ | 규격 미정 | ⛔ |
| S/W 업데이트 | What's New > QR 코드 | 콘텐츠 영역 차단 | ⛔ | ⛔ | ✅ | ⛔ |
| 매뉴얼 | QR 코드 | 콘텐츠 영역 차단 | ⛔ | ⛔ | ✅ | ⛔ |
| 고객센터 | 설정 > 인증 코드 > QR 코드 | QR 이미지 대체 | ⛔ | ⛔ | ✅ | ⛔ |
| 고객센터 | 활성화 > 인증 코드 > QR 코드 | QR 이미지 대체 | ⛔ | ⛔ | ✅ | ⛔ |
| 차량 관리 | 서비스 점검 > QR 코드 | 콘텐츠 영역 차단 | ⛔ | ⛔ | ✅ | ⛔ |
| 프로필 | QR 코드 | QR 이미지 대체 | ⛔ | ⛔ | ✅ | ⛔ |

**주행 규제 판단 기준** (속도·기어·브레이크 등 HW 신호 기반)

- 북미 (`USA`, `CAN`): Auto Hold를 정차로 인정하지 않음. 해제 조건:
  - 시동(IGN1) 또는 엔진(구동 준비) OFF
  - 속도 ≤ 5 km/h 이면서
    - 자동 변속기: 기어 **P**
    - 수동 변속기: **Parking Brake ON + 기어 N**
- 북미 외: Auto Hold를 정차로 인정. 해제 조건:
  - 시동(IGN1) 또는 엔진 OFF
  - 속도 ≤ 5 km/h 이면서 다음 중 하나: 기어 P / Parking Brake ON / Auto Hold 활성(장착 차종)
  - 모두 불충족 또는 5 km/h 초과 → 규제 ON

(원문에 판단 흐름도 이미지가 있으나 이미지 내용은 텍스트로 제공되지 않음)

---

## 11. (준비 중) 결제 시스템 및 외부 라이브러리 마이그레이션

출처: https://document.pleos.ai/docs/pleos-only/in-prep/_payment-system-migration (경로상 "in-prep" 문서)

- **인앱 결제**: Google Play Billing 등 외부 의존성 제거, 플랫폼 전용 **Payment SDK** 사용이 원칙. 정기결제·일회성 구매·쿠폰 로직은 플랫폼 정책에 맞게 재설계. **Payment SDK는 현재 준비 중**, 상세 사양은 오픈 시 안내 예정.
- **광고**: 운전자 주의 분산 규제로 정차 상태, 보조 화면, 승객 전용 디스플레이 등으로 노출 제한 가능. 모바일식 광고 시나리오는 전면 재디자인 필요.
- **서드파티 SDK**: 불필요한 SDK 제거(크래시·성능 저하 방지), 위치·마이크·카메라 접근 SDK는 차량용 보안 가이드라인 부합 여부 확인.

---

## 12. Pleos Connect 제품 개요

출처: https://document.pleos.ai/docs/connect/guide/connect

Pleos Connect는 Android Automotive 기반 SDV 인포테인먼트 플랫폼으로, 차량 데이터·상태를 API로 제공하는 Pleos Connect OS 위에 Gleo AI(42dot LLM 기반 앰비언트 AI), 차선 단위 내비게이션, system apps/pre-installed apps를 제공합니다. Devbox, Pleos Account 기반 프로필 동기화, 스마트폰 연동 등을 지원하며 앱은 Pleos App Market으로 유통됩니다.

| 제품 | 요약 | 문서 |
| --- | --- | --- |
| Pleos Connect OS | AAOS 기반 오픈 플랫폼, 멀티 윈도(클러스터+앱 동시), vehicle property 및 VHAL 제공 | https://document.pleos.ai/docs/connect/docs-ivios/intro |
| Gleo AI | LLM+음성 기반 개인형 AI 어시스턴트 | https://document.pleos.ai/docs/connect/docs-ambient/intro |
| Pleos App Market | 차량 특화 앱 거래 플랫폼 | https://document.pleos.ai/docs/connect/docs-appmarket/intro |
| Navigation | 온라인 기반 내비, 음성 목적지 설정, 실시간 교통, EV 충전소 자동 경유 | https://document.pleos.ai/docs/connect/docs-navigation/intro |

---

## 13. 용어집 (개발자 관련 핵심 발췌)

출처: https://document.pleos.ai/docs/connect/guide/glossary

### 13.1 플랫폼/계정/유통
| 용어 | 정의 |
| --- | --- |
| Pleos Connect | AAOS 기반 SDV IVI 플랫폼. 운영체제 포함 서비스 통칭 (구 IVI OS, 구 CRP – 둘 다 Deprecated 명칭) |
| Pleos Account | 현대자동차 통합 계정. App Market, App Market Console, cloud gateway 인증에 필요 |
| App Market / App Market Console | 앱 유통 서비스 / 앱 심사·출시 승인 개발자 서비스 |
| Infotainment Application | Pleos Connect에서 동작하는 차량용 인포테인먼트 앱 |
| Pre-Installed App / User-Installed App / System App | 출고 시 설치 / App Market에서 다운로드 / 플랫폼 필수 앱(삭제 불가) |
| Pleos Connect Devbox | 플랫폼 개발자용 reference hardware 세트 |
| Cloud Gateway | 42dot API 게이트웨이. 등록된 API 사용 시 authentication & authorization 필요 |
| EIC | cloud gateway에서 인증된 요청을 STT/TTS/RespGen으로 라우팅하는 백엔드 |
| BSM / AMI Server / NSS | 정보 연결 백엔드 / 개인화 AI 모듈 / 지도·교통 서빙 서버 |

### 13.2 SDK
| 용어 | 정의 |
| --- | --- |
| Vehicle SDK | 차량 정보 조회 및 제어 Vehicle API |
| ADAS SDK | 주행 보조 정보 ADAS API |
| NaviHelper SDK | 앱 → Pleos Connect Navigation 앱 연동 API |
| NaviProvider SDK | Navigation 앱 → 앱으로 이벤트·정보 전달 API |
| SpeechToText / TextToSpeech SDK | 42dot 음성인식 / 음성합성 플랫폼 연동 |
| Fused Location SDK | GPS·Wi-Fi·셀룰러·센서 결합 차량 위치 |
| LLM SDK | LLM 플랫폼 연동 (**v2.1.0/Playground v2.6.0에서 제공 종료**) |
| Car Accessory Protocol SDK | 스마트폰에서 차량 상태 조회·제어 API |
| Fleet API | Fleet REST API + Fleet API Webhook |
| Map API / Maps JS | 지도·경로·검색·교통 API / JS 라이브러리 |

### 13.3 차량 통합
| 용어 | 정의 |
| --- | --- |
| VHAL | Vehicle HAL, vehicle property 조회·설정 기반 |
| Vehicle property / Vendor vehicle property | VHAL 기반 표준 속성 / AAOS 표준에 없는 Pleos 확장 속성(ID·권한은 Vendor Vehicle Property API reference) |
| Vehicle emulator | vehicle property 값을 에뮬레이트하는 도구. Vehicle HAL Emulator가 SOME/IP로 Vehicle HAL service에 변경 전달 |
| Vehicle integration reference / SOME/IP based | 외부 ECU와 복잡한 데이터 교환용 Native/JNI/Kotlin 라이브러리·Franca IDL code generator |
| EVS | 외부 카메라 뷰 소프트웨어 스택 |

### 13.4 주행 규제/UX
| 용어 | 정의 |
| --- | --- |
| Driving state | CarDrivingStateService 판단: parked(기어 P), idling(비주차·속도 0), moving(비주차·속도≠0), unknown |
| CarUxRestrictions | 주행 상태별 UX 제한. idling·moving에서 `requiresDistractionOptimization`, `no_setup`, `no_video` 등 적용 |
| Distraction Optimized | 주행 중 조작 제한 정책. CarUxRestrictions + 앱의 `distractionOptimized` 선언 기반 |
| Driving restriction policy | 기어·속도 기반 앱 실행·조작·프로필 전환 제한 (CarDrivingStateService, CarUxRestrictionsManagerService) |
| Full Screen Mode / Entertainment mode | Parking 상태에서 앱 전체 화면 / game·image·video용 변형, timeout·상태 변경 시 해제 |

### 13.5 디스플레이 폼팩터
| 용어 | 정의 |
| --- | --- |
| Connect-S | Standard. 17인치, 14.5인치, 12.9인치. GNB/Status bar/PnP 포함 기준 폼팩터 |
| Connect-L | 구 명칭(12.9인치) → Connect-S로 재분류 |
| Connect-W / Connect-W-Extended | Wide(고급 차종) / 차량 상태에 따라 세로 확장된 형태 |
| Connect-WF | Waterfall, Wide 계열 소형 해상도 |

### 13.6 System UI / 화면 구성
GNB(하단 Global Navigation Bar), Status bar(상단), Global Search, Climate panel, Notification Center, Heads-Up Notification(HUN), Home screen(HOME 역할 system app, TaskView로 앱 배치), TaskView, Window policy / multi window(최대 2개 앱 TaskView), Driving View(AD + Driver information + Vehicle control), Mini app(AppWidget 기반 하단 소형 overlay), OSD, Welcome screen, App Library, Telltale 등. 자세한 정의는 원문 용어집 참조.

### 13.7 AI 관련
Gleo(wake word "글레오"), WWD(Wake Word Detection), PTT(Push To Talk), 42dot LLM, Ambient AI(On-device/Cloud Gleo AI), NL2API, NL2SEARCH, RAG, RespGen.

---

## 14. 불명확/불일치 사항 정리

1. Vehicle SDK 버전: 빠른 시작 예제 `2.3.1` vs 릴리즈 노트·Sample App `2.3.2`.
2. 빠른 시작 문서는 Emulator 저장소 URL을 "제공받은 URL"로만 기재, 설치 문서는 구체 URL(Mac arm64 / Windows·Linux)을 명시.
3. Maven 저장소 인증 방식(토큰/계정) 미기재 — "저장소 접근 권한 확인"만 언급.
4. TTS/STT 클라이언트 ID/시크릿의 코드 적용 방법 미기재.
5. Vehicle 외 SDK의 Maven 좌표 미기재.
6. Vehicle SDK Sample App 소스/APK 배포 위치 미기재.
7. InfoLink Emulator 설치 문서는 404.
8. SDK service status 페이지는 실제 상태 정보 없음.
9. Playground SDK 신청 화면에서 "Vehicle State"(사용 가능)/"Vehicle Control"(준비 중) 구분과 단일 `Vehicle` 아티팩트의 관계 미기재.
