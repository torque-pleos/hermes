# Pleos 개발 레퍼런스 (요약본)

출처: [Pleos Playground](https://developers.pleos.ai/playground) → 공식 문서 [document.pleos.ai](https://document.pleos.ai) (Docusaurus, 한국어판 기준)
수집일: 2026-11-01 · 각 섹션에 원문 URL 표기. 원문과 다를 경우 **원문이 우선**.

## 문서 목록

| 파일 | 내용 |
| --- | --- |
| [01-getting-started.md](01-getting-started.md) | Playground 가입·프로젝트 생성·SDK 사용 신청, Maven 설정, 에뮬레이터/AVD, Virtual Console, 샘플 앱, 릴리즈 노트(최신 버전), 용어 |
| [02-connect-sdk.md](02-connect-sdk.md) | Connect SDK: Vehicle / NaviHelper / STT / TTS / FusedLocation / LLM — 의존성, 권한, 생명주기, 코드 예제, 에러 코드 |
| [02a-sdk-method-index.md](02a-sdk-method-index.md) | SDK 클래스별 메서드 이름 인덱스 (상세 시그니처는 로그인 필요) |
| [03-app-design-dev-guide.md](03-app-design-dev-guide.md) | 앱 기획 가이드(카테고리·화면 모델·성능 기준), 주행 규제, 오디오 포커스, CCP, 미디어/내비 앱, Gleo AI 음성 연동, 디자인 시스템 |
| [04-test-and-publish.md](04-test-and-publish.md) | 테스트(폼팩터·Virtual Console), 보안/태그/업데이트/버전 정책, App Market 제출 절차, 심사 가이드라인, **제출 전 체크리스트** |
| [05-other-apis.md](05-other-apis.md) | Pleos Platform SDK(privileged), Vehicle Data API, Fleet API(REST/Webhook), SDVOS(비공개) |

## 핵심 요약

- **플랫폼**: Pleos Connect = Android Automotive OS(AAOS) 기반 IVI. 앱은 일반 Android 앱(Kotlin/Java) + Pleos Connect SDK.
- **저장소**: `https://nexus-playground.pleos.ai/repository/maven-releases/` · 예: `ai.pleos.playground:Vehicle:2.3.2`
- **최신 버전(2026-11 기준)**: Vehicle 2.3.2 · NaviHelper 3.0.5 · TTS/STT 3.3.1 · FusedLocation 1.1.3 · Emulator 2.1.0 · Playground 2.7.0
- **환경**: minSdk 28+, 에뮬레이터 API 34 `Pleos Connect v2.1.0` 이미지, AVD는 **Cold boot + RAM 4GB** (Quick boot 시 VHAL 불가)
- **테스트**: `adb forward tcp:8088 tcp:8088` → `http://127.0.0.1:8088/` Virtual Console로 차량 상태 조작
- **권한**: `pleos.car.permission.*` — App Market과 **사전 승인된 권한만** 사용 가능 (미승인 선언 시 심사 탈락)
- **패턴**: `initialize()` → `checkXxxCapabilities()` → get/set/register → `release()`
- **배포**: Pleos App Market Console에 **APK**(최대 600MB, 릴리즈 서명) 제출 → 심사. 버전 변경 필수, 자동 업데이트 없음.
- **주행 중 제한**: DO(Distraction Optimized) 미지원 앱은 주행 중 사용 불가. 미디어 앱은 주행 시작 시 영상 일시정지/숨김.

## Playground SDK 실제 상태 (2026-11-01, 로그인 후 프로젝트 SDK 메뉴에서 확인)

| SDK | 항목 | 상태 |
| --- | --- | --- |
| Vehicle State | 제원, 주행(브레이크·드라이브모드·크루즈·주행거리), 도어, EV/연료, 공조, 라이트(방향지시등 포함), 안전(경고등·타이어), 좌석, 사이드미러, 통행료, 창문(와이퍼 모드 포함) — 11개 | **사용 가능** |
| NaviHelper | 경로 정보 조회(경로·위치·TBT), 경로 탐색 설정 — 2개 | **사용 가능** |
| FusedLocation | 정밀 위치 정보 조회 | 준비 중 |
| TextToSpeech | 음성 합성 | 준비 중 |
| SpeechToText | 음성 인식 | 준비 중 |
| Vehicle Control | 도어, 공조, 주행 정보 설정, EV 충전, 창문/와이퍼, 좌석, 사이드미러, 라이트 — 8개 | **전부 준비 중** |
| LLM | — | Playground SDK 메뉴에 없음 |

- 프로젝트 정보 화면에 프로젝트 ID, 클라이언트 ID/시크릿, 에뮬레이터용 CRN 및 adb 입력 명령이 표시됨 (값은 저장소에 커밋하지 말 것).
- 로그인 상태에서도 SDK API Reference 상세 페이지 본문은 비어 있음(문서 사이트 오류로 추정).

## 공개 문서로 확인 불가한 부분

- SDK API Reference 상세(시그니처/설명)는 비로그인 상태에서 본문이 비어 있음 → 메서드명만 수집. Playground 로그인 후 확인 필요.
- LLM SDK는 가이드 페이지 없음(클래스/메서드명만 존재).
- SDVOS 문서는 접근 제한(`/404` 리다이렉트).
- 디자인 시스템의 개별 컴포넌트 페이지, 이미지로만 제공된 수치(해상도·그리드·터치 영역)는 미포함.
- AAB, versionCode 규칙, 난독화, 심사 소요 기간 등은 문서에 명시 없음.
