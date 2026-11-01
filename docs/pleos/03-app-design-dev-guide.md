# Pleos Connect 차량 앱 설계·개발·Gleo AI 연동·디자인 시스템 레퍼런스

> 출처: https://document.pleos.ai 공식 문서(markdown 변환본) 요약. 각 섹션 하단/상단에 원문 URL을 표기했습니다.
> 원문의 이미지(SVG/PNG)에만 있는 수치(예: 버튼 픽셀 크기, 그리드 수치, 해상도 px)는 텍스트로 제공되지 않아 이 문서에 포함하지 않았습니다. 해당 수치가 필요하면 원문 이미지 또는 Figma 리소스를 확인하세요.
> 입력 폴더에는 디자인 시스템 개별 컴포넌트 페이지(`components/...`)가 포함되어 있지 않았습니다(목록은 [9.7](#97-컴포넌트) 참고).

---

## 목차

1. 앱 설계 가이드(Planning Guide)
2. 지역별 주행 규제 정책 / 주행 규제 개발(`CarUxRestrictions`)
3. 오디오 포커스 / Suspend 모드
4. CCP(Rotary Controller)
5. Music/Audio 앱 개발 가이드
6. Maps/Navigation 앱 개발 가이드
7. 기타 개발 가이드(TTS 선행 묵음, IME resize, FAQ, ADB)
8. Gleo AI 앱 연동 가이드(Gleo Interface)
9. 디자인 시스템(Intro, Layout, Window, Token, Theme, Experiences)

---

## 1. 앱 설계 가이드 (Vehicle App Planning Guide)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro

모바일 앱을 Pleos Connect 앱으로 전환하거나 처음부터 설계할 때 필수 고려 사항을 다룹니다. 하위 문서: 주행 안전 설계, 지역별 주행 규제 정책, 앱 카테고리, 앱 구조 및 동작 모델, 하드웨어 접근 정책, SDK API 정책, API 호환성 및 가용성, 보안·개인정보, 성능·리소스.

### 1.1 앱 카테고리

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-category

앱 설계 시점부터 카테고리를 명확히 정의해야 합니다.

| 카테고리 | 핵심 목적 |
| --- | --- |
| Maps/Navigation | 위치 기반 길 안내 |
| Mobility Services | 이동 수단/이동 서비스 제공 |
| Lifestyle | 일상생활 관리 및 향상 |
| Entertainment | 오락 및 여가 제공 |
| Business | 업무 및 생산성 지원 |
| Music/Audio | 오디오 콘텐츠 중심 |
| Kids | 아동 대상 콘텐츠 |
| Social/Communication | 사람 간 소통 및 네트워크 |

카테고리와 주행 상태에 따라 주행 중 사용 가능한 기능 범위가 달라집니다. 주행 중 기능이 **제한적으로 허용될 수 있는** 카테고리 예시(실제 적용은 주행 상태·지역 정책에 따름):

- **Music/Audio**: 앨범 아트 등 단순 시각 정보 표시, 재생/일시정지/곡 넘기기 등 단순 조작, 운전 중 음악 스트리밍 청취, 라디오 채널 변경, 오디오북 재생
- **Maps/Navigation**: 주행 경로 안내 및 실시간 교통 정보, 주의를 분산시키지 않는 간결한 지도, TBT(Turn By Turn) 음성 안내, 목적지 경로 탐색, 실시간 빠른 길 안내, 주변 교통 상황 안내
- **Mobility Services**: 주행과 밀접한 차량 관리 서비스, 현재 위치 기반 즉각 정보, 전기차 충전소 찾기 및 경유지 설정, 가까운 주유소 탐색, 주행 중 접근 가능한 주차장 위치 확인

### 1.2 앱 구조 및 동작 모델 (화면 모델)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model

- 화면 = **Driving view**(운전자 쪽 고정 영역, 앱이 가리거나 종료할 수 없음) + **앱 영역**(차량/디스플레이 구성에 따라 window 1개 또는 2개). 앱 영역에 최대 2개 앱 배치.
- `portrait`: 앱 영역 중 하나의 window 사용. 두 앱 동시 표시 시 각 앱은 일반적으로 portrait.
- `landscape`: 하나의 앱이 두 개의 앱 영역을 확장해 사용.
- Portrait는 핵심 기능·주요 정보, landscape는 확장 영역을 활용한 부가 정보·확장 기능으로 구분 설계. 두 화면을 각각 설계해야 합니다.

**앱 실행 및 window 전환**
- 새 앱은 현재 표시 중인 앱의 방향을 따릅니다. landscape 앱 1개 표시 중 → 새 앱도 landscape. portrait 앱 2개 표시 중 → 새 앱은 primary window에 portrait.
- 화면에서 사라진 앱은 종료되지 않고 백그라운드에 남을 수 있으며, 표시 중 앱이 종료되면 최근 사용 순으로 다시 표시됨 → foreground 재진입을 고려.
- 사용자는 window bar 또는 제스처로 위치·크기 변경 가능 → 크기 변경 시에도 핵심 기능 유지.

**화면 방향/크기**
- `screenOrientation` 미지정 시 Activity는 기본 **portrait**로 실행되며 window 1개 사용.
- `screenOrientation="landscape"` 지정 시 window 2개 영역 사용.
- 반응형 레이아웃 필수. 방향은 `AndroidManifest.xml`의 `screenOrientation` 기준으로 동작.

**포그라운드/백그라운드**
- 포그라운드 진입 시 상태 복원 + 크기/방향에 맞게 UI 재구성.
- 백그라운드 전환 시 화면 갱신·애니메이션·불필요한 센서 수집 등 즉시 중단.
- 오디오 재생·통신·다운로드 등 지속 작업은 실행 조건과 중단·재개 정책 명확히 정의.
- 백그라운드 상주·타이머·폴링·네트워크 요청 최소화.
- 프로세스 재생성 시에도 사용자 데이터·진행 상태 복원 가능한 상태 저장 전략 마련.

### 1.3 주행 안전을 고려한 설계 (Driver Distraction)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline

- [AAOS 운전자 주의 분산 가이드라인](https://source.android.com/docs/automotive/driver_distraction/guidelines?hl=ko) 준수.
- 앱은 `CarUxRestrictions` 기준으로 제한 상태를 확인하고 제한 대상 기능의 UI·동작을 축소/비활성화해야 합니다.
- **주행 규제 상태에서 영상 재생을 시작하거나 재개하지 않도록** 처리. 오디오 재생은 오디오 포커스 정책 준수.

UX 원칙:
- **시선 분산 최소화**: 운전 정보는 짧은 시간 내 이해 가능하도록 간결·명확하게. HUD·클러스터·OSD 영역 사용 시 해당 영역 정책 준수. 주행 중 동영상·애니메이션·움직이는 이미지 최소화/제한. 장식적·반복적으로 움직이는 시각 요소 지양.
- **정보 구조·인터랙션**: 주행 중 필요한 정보·기능 우선 배치, 최소 조작으로 도달, 자주 쓰는 기능은 첫 화면·상단에, 드롭다운·다단 메뉴 남용 금지, 자동 애니메이션·반복 화면 전환 최소화, 중요한 오디오 안내 시 오디오 포커스 정책에 따라 미디어 일시정지/볼륨 조정.
- **터치 조작 최적화**: UI Design Guide에 따라 large touch target, high contrast, 텍스트 최소화 패턴 적용. 터치 최소화, 음성·물리 버튼 연계. 게임·인터넷 서핑·피트니스 등 주의 분산 활동 유도 금지. 버튼 크기·터치 영역 간 간격은 UI Design Guide 기준(구체 수치는 원문 이미지/버튼 컴포넌트 페이지 `/docs/connect/guide/docs-design/components/actions/basic-button` 참조).
- **텍스트 입력 최소화**: 검색·로그인도 키보드보다 프리셋 선택, 최근 항목, 음성 입력 우선. 음악 앱은 프리셋 플레이리스트·최근 재생·즐겨찾기를 첫 화면에 배치.

### 1.4 하드웨어 접근 정책 (Platform API Architecture)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture

- **직접 접근 금지**: 차량 물리 제어·센서에 직접 접근 불가.
- **플랫폼 API 우선**: 차량 상태 확인·제어는 Pleos Connect SDK 표준 플랫폼 API 사용.
- 모바일 전용 API는 제공되지 않거나 제한될 수 있음: Telephony(전화), SMS, 일부 센서(자이로, 근접 등), 캘린더, 주소록 등. → 불필요 기능 제거 또는 지원 여부 확인 후 동작하는 Optional 구조로 설계.
- **주행 중 네트워크 대응**(권장): 최근 콘텐츠 로컬 캐싱, 재시도(Retry) 로직과 적절한 타임아웃, 오프라인 모드 안내 UI.

### 1.5 SDK API 정책 (AAOS Alignment)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api

- **표준 우선**: AAOS 표준에 동일/유사 API가 있으면 표준 API 사용 권장, SDK는 중복 기능 미제공.
- **확장 기능**: AAOS로 접근 불가하거나 제조사 고유 보안·특화 제어 영역만 SDK 전용 API 제공.
- **하위 호환성**: SDK API가 AAOS 표준으로 편입되더라도 기존 SDK API는 **최소 2개 메이저 버전**까지 유지.

| 구분 | 접근 경로 | 예시 |
| --- | --- | --- |
| 표준 데이터 | AAOS Car API | 속도, 기어 상태, 외기 온도, 연료 레벨 등 |
| 특화 기능 | Pleos Connect SDK | 제조사 전용 시트 제어, 특화 라이팅, 고유 보안 인증 등 |

### 1.6 API 호환성 및 가용성

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability

| SDK | 차량 모델별 호환성 |
| --- | --- |
| Vehicle SDK | 모델에 따라 다름 — 도메인별 `checkXXXCapabilities` API로 확인 |
| NaviHelper SDK | 내비게이션 탑재 차량에 한해 전체 호환 |
| TextToSpeech SDK, SpeechToText SDK | 전체 호환 |
| FusedLocation SDK | 모델에 따라 다름 — `isSupported` API로 확인 |

- AAOS Vehicle Property: 정의/ID는 [`VehiclePropertyIds`](https://developer.android.com/reference/android/car/VehiclePropertyIds), 실제 지원·접근 가능 여부는 [`CarPropertyManager`](https://developer.android.com/reference/android/car/hardware/property/CarPropertyManager)로 확인.
- **모델명 기준 분기 금지**. SDK가 제공하지 않는 특수 프로퍼티가 필요하면 구현 전 기술 지원팀 검토 요청.

### 1.7 보안 및 개인정보

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/security-privacy

- 차량은 공용 기기 → 소유자 변경·운전자 교체 시 쉽게 로그아웃/계정 전환 가능해야 함.
- 소셜 로그인·OTT 토큰 등은 Keystore 등 안전한 스토리지에 저장.
- 위치·음성·계정 정보 요구 시 "왜 필요한지" 명확히, 동의 플로우는 간단하게.
- 불필요한 PII 저장 금지, 로그의 개인정보는 마스킹.
- Telemetry/분석: 차량 ID·사용자 ID 익명화/가명화 정책 수립, 운전 중 행동 데이터를 마케팅 목적으로 사용하지 않도록 구분.
- 상세 등록 보안 기준: `/docs/pleos-only/app-registration-guide/app-security-guide`

### 1.8 성능 및 리소스

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt

- Cold start 시 스플래시에 오래 머물지 않도록 최소 초기화, 나머지는 lazy loading.
- **CPU 사용률 기준**
  - 내비게이션 앱(주행 중): **15% 이하**
  - 내비게이션이 아닌 앱(주행 중): **5% 미만**
  - 모든 앱(정차 중): **25% 이하**
- **RAM 사용량 400 MB 이하**.
- 백그라운드 긴 작업·불필요한 타이머/폴링 최소화, RecyclerView·캐시 전략으로 메모리 최적화.
- 오디오·미디어 앱은 Pleos Connect 오디오 포커스 정책 준수, 포커스 획득·상실 대응 로직 반드시 구현.

---

## 2. 주행 규제

### 2.1 지역별 주행 규제 정책

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy

- 주행 규제는 속도·기어·브레이크 등 하드웨어 신호 + 지역별 안전 규정에 따라 적용. 플랫폼 버전·양산 차량에 따라 다를 수 있음.
- **Distraction Optimization(DO) 미선언 앱**: Parking(주차) = 사용 가능 / Drive(주행) = **사용 불가**.

주행 규제 적용 시 기능별 허용(✅ 사용 가능, ⛔ 제한, `음성한정` 음성으로만). Parking에서는 모든 지역·모든 항목 ✅.

| 기능 (관련 플래그) | 북미 Driving | 유럽 Driving | 그외 지역 Driving |
| --- | --- | --- | --- |
| 키보드 (`UX_RESTRICTIONS_NO_KEYBOARD`(8)) | ⛔ | ✅ | ✅ |
| 다이얼패드 (`UX_RESTRICTIONS_NO_DIALPAD`(1)) | ⛔ | ✅ | ✅ |
| 검색 (`NO_KEYBOARD`(8), `NO_FILTERING`(2)) | `음성한정` | ✅ | ✅ |
| 미디어 앱 로그인·설정·검색 (`NO_SETUP`(64), `NO_KEYBOARD`(8), `NO_FILTERING`(2)) | ⛔ | ⛔ | ✅ |
| 디지털키·도어·시트·차량 설정 (`NO_SETUP`(64)) | ⛔ | ⛔ | ⛔ |
| 동영상 시청 (`UX_RESTRICTIONS_NO_VIDEO`(16)) | ⛔ | ⛔ | ⛔ |
| 내비게이션 검색 (`NO_KEYBOARD`(8), `NO_FILTERING`(2)) | ⛔ | ✅ | ✅ |
| QR 로그인 (`NO_SETUP`(64)) | ⛔ | ⛔ | ⛔ |
| 매뉴얼(대용량 텍스트) (`UX_RESTRICTIONS_LIMIT_CONTENT`(32), `UX_RESTRICTIONS_LIMIT_STRING_LENGTH`(4)) | ⛔ | ⛔ | ⛔ |

- 「검색」은 웹 검색이 아니라 인포테인먼트 내 **통합 검색**.
- InfoLINK용 앱은 지역별 키보드 정책 분기 처리가 필요할 수 있음 → partnership@pleos.ai 문의.

### 2.2 주행 규제 개발 가이드 (`CarUxRestrictions`)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide

- [`CarUxRestrictionsManager`](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager)에 `registerListener()` 등록 → `onUxRestrictionsChanged()` 콜백으로 [`CarUxRestrictions`](https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictions) 수신.
- DO를 선언한 앱도 규제 활성화 시 `CarUxRestrictions`와 `ActiveRestrictions`를 확인해 기능별 UX 제한을 직접 적용해야 합니다.
- 플랫폼이 지역별 사양에 맞는 `ActiveRestrictions`를 내려주므로, 앱은 notify로 수신한 값만 반영하면 됩니다(앱이 지역 판단 불필요).
- 예: 동영상(DMB)은 `UX_RESTRICTIONS_NO_VIDEO`로 판단. UX 규제 Figma 사양서는 Pleos Playground에 비공개.

샘플 코드 (동영상 재생 제한):

```java
if (mCurrentCarUxRestrictions.isRequiresDistractionOptimization()
        && (mCurrentCarUxRestrictions.getActiveRestrictions()
        & CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO) != 0) {
    stopPlayingVideo();
}
```

테스트용 ADB(모든 차종 공통):

```sh
adb shell cmd hmg_car_service --service drivingstate driving_regulation on
adb shell cmd hmg_car_service --service drivingstate driving_regulation off
```

---

## 3. 오디오 포커스

### 3.1 오디오 포커스 개발 가이드

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development

[AAOS 오디오 포커스](https://source.android.com/docs/automotive/audio/audio-focus?hl=ko)를 바탕으로 하되 **Pleos Connect 오디오 포커스 정책(Interaction Matrix)** 을 반드시 따릅니다.

**AAOS 기본 개념**
- 새 요청의 CarAudioContext와 현재 보유자 CarAudioContext 간 사전 정의된 Interaction으로 처리.
- R(Reject): 거부, 포커스 못 얻음 / E(Exclusive): 새 요청이 획득, 기존 보유자 상실 / C(Concurrent): 조건 충족 시 동시 유지(일반적으로 duck 기반).
- 포커스는 오디오 제어의 유일 수단이 아니라 충돌 방지/간접 제어 수단.
- C를 의도하면 들어오는 요청은 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` 사용(대표). 기존 보유자 설정(duck 시 pause 등)에 따라 C가 깨질 수 있음.
- 보유자가 여럿일 때 가장 보수적인 결과 적용: **R > E > C**.
- (Optional) 통화 중 내비를 C → R로 바꾸는 사용자 설정 플래그. (Optional) non-transient 요청이 R일 경우 지연 포커스(delayed) 지원 가능.

**Pleos Connect Interaction Matrix v1.0 (2026-01-28)** — 원문 이미지로 제공.
- 행: 현재 포커스 보유자 컨텍스트 / 열: 새 요청 CarAudioContext / 셀: E/C/R
- 컨텍스트: MUSIC, NAVIGATION, VOICE_COMMAND, CALL_RING, CALL, ALARM, NOTIFICATION, SYSTEM_SOUND, EMERGENCY, SAFETY, VEHICLE_STATUS, ANNOUNCEMENT

**개발사 구현 규칙**
1. 각 스트림은 재생 전 `AudioAttributes` + `AudioFocusRequest`로 포커스 요청.
2. C 기대 컨텍스트(예: NAVIGATION이 MUSIC과 동시)는 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` 사용.
3. E 필요 컨텍스트(CALL, EMERGENCY 등)는 목적에 맞는 gain 사용, 포커스로 모든 것을 강제 제어하지 않음.

**포커스 변경 이벤트 처리(필수, AAOS 필수는 아니나 Pleos 정책상 필수)** — 권장 매핑:
- `AUDIOFOCUS_GAIN` → 정상 재생
- `AUDIOFOCUS_LOSS` → 즉시 중단/정지
- `AUDIOFOCUS_LOSS_TRANSIENT` → 일시정지 후 복귀 가능
- `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` → duck(볼륨 감쇄)

**Duck 규칙**: `LOSS_TRANSIENT_CAN_DUCK` 수신 시 볼륨 감쇄(예: -X dB), 해제 조건(다른 스트림 종료/포커스 반환)에서 원복. duck 중 pause하도록 구성하면 C가 깨질 수 있음.

**다중 홀더 처리**(개념 의사 코드):

```text
cell = MATRIX[currentHolder][incoming]

result = min_by_severity(result, cell) (R가 가장 강함)
```

result = C로 시작 → 모든 currentHolder에 매트릭스 적용 → 결과에 따라 grant / steal / fail.

**컨텍스트별 가이드**
- **EMERGENCY**: current일 때 대부분 incoming은 R. incoming EMERGENCY는 대부분 current를 E로 선점. EMERGENCY-EMERGENCY는 C. 테스트에서 긴급 중 음악/알림 요청 실패 확인 필수.
- **SAFETY**: MUSIC/NAVIGATION/알림과는 대체로 C/E. EMERGENCY가 current면 SAFETY incoming은 R (Safety는 Emergency 아래).
- **CALL / CALL_RING**: CALL 중 MUSIC incoming = R(통화 중 음악 금지), NAVIGATION incoming = C(통화 중 내비 허용). 거부 옵션 요구 시 플래그로 C→R.
- **NAVIGATION ↔ MUSIC**: 기본 C 공존. NAVIGATION은 MAY_DUCK 요청 사용 필수.
- **VOICE_COMMAND(Gleo)**: 일반 오디오보다 우선하되 EMERGENCY, CALL 등 Reject 컨텍스트에는 우선하지 않음. Gleo 호출로 `AUDIOFOCUS_LOSS_TRANSIENT` 수신 시 즉시 재생 중단, 필요 시 abandon.

**설정/로그 권장**
- 매트릭스 하드코딩 금지 권장 → `interaction_matrix.json` 또는 csv 탑재 후 로딩. 키 `current_context`, `incoming_context` → 값 E|C|R.
- 로그 필수: `incoming context`, `gain type`, `client id`, current holders 목록, holder별 결과, 최종 결과 및 처리(grant/steal/reject), duck 적용/해제(대상 스트림, 감쇄 값).

**검증 체크리스트(필수)**
1. MUSIC 재생 중 NAVIGATION 요청 → C (뮤직 duck + 내비 재생)
2. CALL 중 MUSIC 요청 → R (실패)
3. MUSIC 중 CALL 요청 → E (통화가 뮤직 선점)
4. EMERGENCY 재생 중 어떤 요청(MUSIC/NOTIFICATION 등) → R
5. MUSIC+NAVIGATION 동시 홀더 상태에서 VOICE_COMMAND 요청 → 보수적 결과(R/E 우선) 적용 확인

**CarAudioContext ↔ AttributeUsages**

| CarAudioContext | AttributeUsages |
| --- | --- |
| MUSIC | `MUSIC`, `UNKNOWN`, `GAME`, `MEDIA` |
| NAVIGATION | `NAVIGATION`, `ASSISTANCE_NAVIGATION_GUIDANCE` |
| VOICE_COMMAND | `VOICE_COMMAND`, `ASSISTANT` |
| CALL_RING | `CALL_RING`, `NOTIFICATION_RINGTONE` |
| CALL | `CALL`, `VOICE_COMMUNICATION`, `VOICE_COMMUNICATION_SIGNALING` |
| ALARM | `ALARM` |
| NOTIFICATION | `NOTIFICATION`, `NOTIFICATION_*` |
| SYSTEM_SOUND | `SYSTEM_SOUND`, `ASSISTANCE_SONIFICATION`, `ASSISTANCE_ACCESSIBILITY` |
| EMERGENCY | `EMERGENCY` |
| SAFETY | `SAFETY` |
| VEHICLE_STATUS | `VEHICLE_STATUS` |
| ANNOUNCEMENT | `ANNOUNCEMENT` |

시스템 컨텍스트는 Android 11 이상에서만 사용 가능.

### 3.2 Suspend 모드 오디오 포커스

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/suspend-mode-audio-focus

- Suspend = IGN OFF 후 완전 종료가 아닌 저전력 상태. UI/디스플레이 비활성, 오디오·미디어 중단, Garage mode 등 일부 시스템 작업만 제한적 수행. 앱은 "언제든 중단되고 다시 시작될 수 있는 상태"로 간주.
- **원칙**: Suspend 진입 전 오디오 출력 중단, 포커스 명시적 abandon, Resume 후 자동 포커스 복원 없음, 상태 저장 후 필요 시 재요청.
- **진입 시 순서**: ① 재생 즉시 중단 ② 포커스 abandon ③ 플레이백 상태 저장(재생 여부, 위치 등) ④ 오디오 리소스 해제(player, track 등). 이 과정에서 새 재생/포커스 요청 불가.
- **예외**(최소 범위): 전화 통화·긴급 통신, 안전 경고음, 시스템 필수 음성 안내.
- **Resume**: 이전 상태 그대로 복원하지 않음. 앱이 재평가 후 필요 시 재요청. 자동 재생 여부는 OEM/사용자 정책.
- **준수**: Suspend 시 포커스 유지 금지, 백그라운드 재생 금지, Resume을 위한 포커스 선점/유지 금지.

예제(Kotlin, 원문 그대로):

```kotlin
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager

    // 예시용 플레이어 상태
    private var isPlaying: Boolean = false
    private var lastPlaybackPositionMs: Long = 0L
    private var hasAudioFocus: Boolean = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausePlayback()
                abandonAudioFocus()
            }
        }
    }

    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun startPlayback() {
        if (!requestAudioFocus()) return
        // TODO: 실제 플레이어 start 호출
        isPlaying = true
    }

    fun onSuspendEntering() {
        // 1) 새 작업 시작 금지
        // 2) 진행 중 작업 중단
        pausePlayback()
        // 3) 상태 저장
        savePlaybackState()
        // 4) 오디오 포커스 반납
        abandonAudioFocus()
        // 5) 필요 시 네트워크/타이머/센서 리소스 정리
        releaseTransientResources()
    }

    fun onResumeFromSuspend() {
        // Resume 후 자동 재생을 바로 하지 않고,
        // 현재 시스템 상태/사용자 조건을 다시 확인한 다음 재개 여부 판단
        restorePlaybackState()
        val shouldAutoResume = false // OEM/앱 정책에 따라 결정
        if (shouldAutoResume) {
            startPlayback()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        hasAudioFocus = false
    }

    private fun pausePlayback() {
        if (!isPlaying) return
        // TODO: 실제 플레이어 현재 위치 조회
        lastPlaybackPositionMs = getCurrentPlaybackPosition()
        // TODO: 실제 플레이어 pause/stop 호출
        isPlaying = false
    }

    private fun savePlaybackState() {
        // TODO: SharedPreferences/DB/ViewModel 등에 저장
        // 예: 재생 위치, 현재 콘텐츠 ID, 재생 여부
    }

    private fun restorePlaybackState() {
        // TODO: 저장된 상태 복원
    }

    private fun releaseTransientResources() {
        // TODO: 타이머 취소, 센서/리스너 해제, 임시 wake lock 정리 등
    }

    private fun getCurrentPlaybackPosition(): Long {
        // TODO: 실제 플레이어 position 반환
        return lastPlaybackPositionMs
    }
}
```

---

## 4. CCP (Center Control Panel / Rotary Controller)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ccp-rotary-controller-development

CCP = 물리 버튼·하드키(Rotary Controller, 다이얼, 물리 버튼 등)로 시스템 기능을 제어하는 입력 장치. CCP Back 하드키는 이전 단계 이동 또는 앱/시스템 Back 수행.

**앱 분류별 정책**
- **User-installed 앱**(Pleos App Market 배포): CCP 지원 선택. 미지원 앱도 등록·배포 제한 없음. 단, 필수 조작을 **터치만으로 완결**할 수 있어야 함.
- **Pre-installed 앱**: CCP **필수**. 계약서에 의무 명시, 초기 릴리즈부터 회전/클릭/방향 이동 지원, 이후 업데이트에서 누락·비활성화 불허, 미지원/불완전 시 승인 불가.
  - 예외(계약서 명시 필요): ① 정적 콘텐츠 중심(포커스 조작 불필요, 단순 정보 표시형) ② 브라우저/특정 미디어 앱처럼 기술적 제한(WebView 포커스 제약 등) ③ 게임처럼 UX·안전상 부적합.
- **System 앱**: 플랫폼/OEM 시스템 이미지 포함 앱.
- **Pre-installed 브라우저 앱** 요건: 회전 → 스크롤(상/하), 클릭 → 링크/버튼 선택, 좌/우 → 포커스 이동(주소창, 북마크/탭/홈, 뒤로/앞으로, 설정, 기본 메뉴). CCP 지원은 브라우저 UI 영역에 한정(웹페이지 내부 포커스 전면 보장 불요).

**CCP 미지원 선언** (`AndroidManifest.xml`):

```xml
<application
    ...>
    <meta-data
        android:name="com.hmg.rotary.ROTARY_SUPPORTED"
        android:value="false" />
</application>
```

**CCP Back 하드키 → 앱 숨김(필수)**
- 앱 최상단 포커스(더 이상 이전 화면 없음)에서 Back 입력 시 **종료하지 않고 숨김(Hide)**. 미디어 사운드 등 백그라운드 기능은 계속 동작.
- 하위 화면/팝업이 있으면 기존 Back 정책(이전 화면 이동/팝업 종료).
- CCP Back이 Android 표준 Back 이벤트로 오는지 확인 → `OnBackInvokedDispatcher` 또는 기존 Back API 사용.
- 최상단에서 `moveTaskToBack(true)` 호출. `finish()`/`finishAffinity()` 사용 금지. 프로세스가 종료될 수 있으므로 상태 저장·복원 필요.
- 최상단 Back에서 앱 종료되거나 `moveTaskToBack(true)` 숨김 미구현 시 **심사 반려 가능**.

**CCP 심사(Pre-installed)**: 모든 주요 UI 포커스 이동, 회전 시 리스트/메뉴 스크롤, 클릭 선택/확인, 역방향(뒤로/취소), 터치 전용 영역 부재, 운전 중 차단 요소 등 안전 동작 준수. 합격 시 등록, 불합격 시 수정 후 재심사.

- CCP UX 가이드라인 다운로드: `/img/policy/Pleos-Connect-CCP-UX-guideline-20260220.fig`
- 정책은 AAOS 버전·플랫폼·차종별로 업데이트될 수 있으며, HMI/안전 기준 미준수 시 배포 제한 또는 기본 탑재 제외 가능.

---

## 5. Music/Audio 앱 개발 가이드

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/music-audio-app-developement-guide

(이 가이드는 Pleos App Market 등록 심사 기준으로 적용됩니다.)

**카테고리**
- 포함: 음악 스트리밍/재생, 팟캐스트·오디오북·라디오, 차량 내 미디어 재생 목적 오디오 플랫폼. **영상이 주 기능인 앱은 제외**.
- 등록 요건: 오디오 재생이 주 기능, 주행 환경 적합 UI/UX, 백그라운드 재생 및 차량 미디어 컨트롤 연동.
- 부가 기능(커뮤니티, 쇼핑 등)이 있으면 오디오 재생이 핵심인지로 판단. 음악을 단순 보조 기능으로 제공하는 앱은 즉시 반려.

**미디어 앱 라이브러리 하위 카테고리(필수 정보 전달)**

| 하위 카테고리 | 구분 원칙 |
| --- | --- |
| 음악(Music) | 아티스트·앨범·트랙 기반 (스트리밍, 로컬 플레이어) |
| 팟캐스트(Podcast) | 에피소드 단위, 구독·최신 회차·에피소드 목록 구조 |
| 오디오북(Audiobook) | 도서 기반 장시간, 챕터 탐색·재생 위치 저장 |
| 라디오(Radio) | 실시간 스트리밍/방송, 채널·방송국 중심 탐색 |

- 콘텐츠 유형별 명확한 브라우징 구조 제공, 하위 카테고리 정보 전달 필수, 단일 리스트 혼합 노출은 제한될 수 있음. 하위 카테고리 불명확 시 반려 가능.
- 미디어 센터 사용 앱은 미디어 앱 라이브러리에 자동 등록 가능, 미사용 앱은 라이브러리 미표시.

**매니페스트**

```xml
<application
    ...
    android:appCategory="audio" />
```

```xml
<application
    ...>
    <meta-data
        android:name="androidx.car.app.launchable"
        android:value="true" />
</application>
```

**필수 요구사항**
- Android `MediaBrowserService`(MBS) 구현 필수(차량 시스템이 MBS로 탐색·상태 확인·제어). `MediaSession`으로 재생 상태·제어 제공. MBS 미구현 오디오 앱은 탑재·배포 제외 가능.
- **미디어 센터**(MBS 기반 개발 템플릿) 사용 권장.

| 기능 | 미디어 센터 사용 | 미사용 |
| --- | --- | --- |
| 스티어링 휠 키 | 기본 제공 | 별도 구현 필요(미구현 시 반려 가능) |
| 미디어 앱 라이브러리 등록 | 지원 | 미지원(미노출) |
| Tune Knob(다음/이전 곡) | 지원 | 미지원 |
| 차량 Media 버튼 연동 | 지원 | 미지원 |
| 차량 Mode 버튼 연동 | 미지원 | 미지원 |
| 차량 미디어 생태계 통합성 | 높음 | 제한적 |
| 추가 개발 부담 | 적음 | 기능별 별도 개발 |

**스티어링 휠 키 지원(필수)**: 미디어 센터 연동(재생/일시정지/정지/이전/다음) 또는 MBS 기반 구현.
- MBS 구현 시 `MediaSessionCompat` 등록 및 `MediaSessionCompat.Callback` 구현. 최소 지원: 재생, 일시정지, 정지, 미디어 ID 기반 재생, 검색 기반 재생. 가능하면 이전/다음 트랙.
- 앱 연결 시 **자동 재생 금지**(사용자 명시 동작 또는 시스템 재개 요청 시에만). 프로필 변경 시에도 자동 재생 금지.
- 미지원/Google 가이드 미준수 미디어 세션 → 탑재·배포 제외 가능.

**오디오 포커스 상실·세션 정리**
- 게임 앱 실행 등으로 `AUDIOFOCUS_LOSS` 수신 시 `MediaSession`이 active로 남지 않도록 stop 또는 session 비활성화.

```kotlin
private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focus ->
    when (focus) {
        AudioManager.AUDIOFOCUS_LOSS -> {
            stopPlayback()
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> pausePlayback()
        AudioManager.AUDIOFOCUS_GAIN -> resumePlayback()
    }
}
```

- `bind()`/`unbind()`만 쓰면 `onTaskRemoved()` 미수신 가능 → `onCreate()`에서 `startService()` 호출해 Started 상태로.
- `onStartCommand()` 및 재생 시점에 `startForeground()` 호출.
- `onTaskRemoved()`: `stopMedia()` → `stopSelf()` → `super.onTaskRemoved()` 순서(내부 정지 로직을 super 호출 전에).
- 쓰리핑거 동작 시 `MediaSessionCompat.Callback.onStop()` 호출될 수 있음 → `ExoPlayer.stop()` 수행.

**Gleo AI 연동**
- 곡 검색·재생 음성 기능(예: "멜론 앱에서 아이유 노래 틀어줘") + 핵심 기능 약 3~5개 추가 음성 기능 선택 제공 가능. 심사 통과·출시 후 설치 즉시 사용.
- 방법 ① **Gleo Interface 구현**(function 구현 + schema 등록, 8장 참조) ② **Android 표준 미디어 API 기반**(Gleo AI가 Media3 기반 MediaAgent로 제어; Schema 없이 음성 기능 사용 가능하도록 향후 업데이트 준비 중).
- Media3 최소 조건: `MediaSession`/`MediaLibraryService` 연동, 재생/일시정지/다음/이전 등 기본 명령, (`MediaLibraryService` 시) 검색 명령. `MediaBrowserService`도 호환되나 Media3 권장.
- 곡 검색 음성 기능은 **Media3 `MediaLibraryService`의 검색 기능 필수**. `MediaSession`만 있으면 기본 재생 제어만 가능.

**심사/위반 조치**: 카테고리·하위 카테고리 명확성, 라이브러리 연동 구조, 사용성·안전성 평가. 카테고리 불일치 → 수정 요청/반려, 하위 카테고리 미정의 → 보완 요청, 반복 위반 → 노출 제한/등록 중단.

---

## 6. Maps/Navigation 앱 개발 가이드

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/navigation-app-development-guide

기본 정책: 운전자 안전 최우선, 단일 경로·단일 내비게이션, 주행 중 조작 최소화, 카테고리 기반 기능 제한, Pleos Connect 플랫폼 API·AAOS 표준 API 사용 및 UX 가이드 준수.

**카테고리 선언**(내비컨테이너 = 내비앱 고정 표시영역 사용을 위해 필수):

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.APP_MAPS" />
</intent-filter>
```

- Pleos App Market 등록 시에도 동일 카테고리 반영. 선언 카테고리와 실제 기능 불일치 시 즉시 반려, 심사 중 기능 변경 시 재심사 요청.

**단일 경로 정책 (Route Exclusivity)** — AAOS에서는 하나의 Navigation 앱만 Active Route 가능.

| 상태 | 설명 | 허용 범위 |
| --- | --- | --- |
| Active Route | 경로 안내 | 음성·TBT 허용 |
| Inactive Route | 지도 탐색 상태 | 안내 금지 |
| Background | 백그라운드 | 모든 안내 금지 |

- Inactive/Background에서 안내 제공 시 정책 위반(OEM 내비는 안전규제 대응 정보 예외). Navigation 앱 간 및 Phone Projection과의 관계에도 적용.
- 사용 API: `CarAppFocusManager` 직접 핸들링 — [`requestAppFocus`](https://developer.android.com/reference/android/car/CarAppFocusManager#requestAppFocus(int,%20android.car.CarAppFocusManager.OnAppFocusOwnershipCallback)), [`CarAppFocusManager.OnAppFocusChangedListener`](https://developer.android.com/reference/android/car/CarAppFocusManager.OnAppFocusChangedListener).
- 경로 전환 허용: 사용자의 명시적 선택, 기존 내비 앱 오류/종료. 전환 시 사용자 인지 안내 제공, 동의 없는 자동 전환 금지.

**주행 중 조작**
- 금지: 복잡한 메뉴 탐색(**3 depth 초과**), 긴 리스트 스크롤, 영상·애니메이션·광고 노출
- 허용: 음성 입력, 단일 터치 기반 주요 기능, 즐겨찾기 목적지 선택

**UX 심사**: Pleos Connect·AAOS UX 가이드 준수, 터치 영역 최소 크기 충족, 주행 핵심 정보 상시 노출, **주행 중 전체 화면 팝업 금지**.
**시스템/권한**: 공식 API 권장, 비공식 센서/API 금지. 위치 권한 필수, 마이크는 음성 입력 시에만, 불필요 권한 요청 시 반려.
**성능**: 실행·전환 지연 최소화, 장시간 주행 안정성, Crash/freeze 시 반려.
**보안**: 위치 데이터 암호화, HTTPS, 개인정보 처리방침 제공, 불필요한 위치 저장 금지.
**즉시 반려(Immediate Reject)**: 주행 중 광고 노출, 영상·게임 요소, 복수 내비 동시 경로 안내, 카테고리 위반 기능, OEM/AAOS 정책 위반.

---

## 7. 기타 개발 가이드

### 7.1 TTS / 음성 안내 선행 묵음(pre-roll silence)

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide

- 적용 범위: TTS 음성 안내, 녹음 음원 안내 멘트, 내비·알림·상태 안내 등 모든 음성 출력 경로.
- 모든 음성 안내 시작 전 **150 ms ~ 200 ms** 선행 묵음 권장(최소 150 ms; 150 ms 미만은 첫 음절 손실 가능; 200 ms 초과는 체감 지연, 예외적으로만).
- 묵음 없이 즉시 재생 금지, 일관된 기준 적용.
- 적용 위치 우선순위: ① PCM/WAV 데이터 앞단 silence padding ② TTS 결과 후처리 ③ 플레이어 재생 지연(스케줄링 편차가 커서 비권장).
- 연속 발화 단위에는 추가 묵음 불필요, 스트림이 끊기거나 새 세션이면 재적용.
- 함께 확인: 출력 경로 사전 초기화, 오디오 포커스/세션 확보 완료, 디코더/플레이어/출력 디바이스 재생 가능 상태. 선행 묵음은 출력 경로 초기화 문제 자체를 대체하지 않음.

### 7.2 IME 동작에 따른 window resize

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/ime-window-resize-handling-guide

IME 표시 시 view를 IME insets만큼 조정하는 것은 AOSP 기본 동작입니다.

| 구분 | 사양 |
| --- | --- |
| EditText/TextField가 포함된 window | IME 표시 시 view가 위로 밀려 올라가는 것이 사양 |
| EditText/TextField가 없는 window | IME 표시 시 view가 앵커링되지 않아야 함 → 앱 수정 필요 |

insets 적용 대상을 `statusBars`·`navigationBars`로 한정하면 IME insets 영향 제외.

System Dialog:

```kotlin
val params = WindowManager.LayoutParams(...)
val typesToFit = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
params.setFitInsetsTypes(typesToFit)
windowManager.addView(overlay, params)
```

App Dialog:

```kotlin
val dialog = Dialog(this)
val window = dialog.window
window?.let { win ->
    val params = win.attributes
    val typesToFit = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
    params.setFitInsetsTypes(typesToFit)
    window.attributes = params
}
dialog.setContentView(R.layout...)
dialog.show()
```

- IME 외 SystemBars insets에 따른 view 업데이트는 [`View.OnApplyWindowInsetsListener`](https://developer.android.com/reference/android/view/View.OnApplyWindowInsetsListener)에서 개별 처리.
- Activity 유형은 Google [Handle input method visibility](https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility#Respond) 기준 별도 검토.

### 7.3 FAQ — 실행 환경 및 플랫폼 리소스

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq

- **플랫폼 확인**: `android.os.Build.DEVICE`(`ro.product.device`). 실차에서만 유효(에뮬레이터 구분 불가). 값: Connect-S `CONNECT_S`, Connect-L `CONNECT_L`, Connect-W `CONNECT_W`.
- **지역 코드**: `product.sw.target_region` → 한국 `KOR`, 유럽 `EUR`, 북미 `USA`, `CAN`. 지역별 UX 정책 적용에 사용.
- **내비게이션 탑재 여부 확인**: 퍼블릭 미공개 → partnership@pleos.ai.
- **클러스터와 동일한 속도**: 클러스터는 `PERF_VEHICLE_SPEED` 사용 → 권한 선언 후 `CarPropertyManager`로 `PERF_VEHICLE_SPEED` 조회/구독(GPS 계산 속도는 어긋날 수 있음).
- **배터리 충전 상태**: Vehicle SDK `getEvBatteryStateOfCharge` API.
- **미디어 코덱**(에뮬레이터 결과를 실차에 그대로 적용 금지):
  - Connect-S/L 오디오: FLAC, AC3+, ALAC, APE, PCM, TTA, AIFF, WMA, OGG, DSD, DFF/DSF, DTS, WAV, MP2, MP3, BSAC, ADPCM, HE-AAC, HE-AAC+, AAC, EAC3, EAC3+JOC, OPUS, AMR 등 / 비디오: MPEG-1, MPEG-2, VC-1, WMV, H.263, MP42, MP43, MPEG4 Part2, Xvid, VP6, VP7, VP8, VP9, H.264, HEVC(H.265) – main 10 profile(기타 포함), x265, AV1 등
  - Connect-W 오디오: PCM, AAC, MPEG, AMR, WMA, Opus, Vorbis, FLAC, ALAC, APE, Dolby, DTS, ADPCM, AIFF, TTA, BSAC / 비디오: MPEG-1, MPEG-2, MPEG-4 Part 2, H.263, H.264 (AVC), H.265 (HEVC), AV1, VP8, VP9
  - Emulator: 오디오 디코더 `c2.android.{amrnb,amrwb,flac,g711.alaw,g711.mlaw,aac,mp3,opus,raw,vorbis}.decoder`, 비디오 디코더 `c2.android.{h263,av1,avc,hevc,mpeg4,vp8,vp9}.decoder`, 오디오 인코더 `c2.android.{amrnb,amrwb,flac,aac,opus}.encoder`, 비디오 인코더 `c2.android.{h263,av1,avc,hevc,mpeg4,vp8,vp9}.encoder`

### 7.4 ADB Command 레퍼런스

출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide

설치:
```bash
# Mac
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install android-platform-tools
adb version
```
```powershell
# Windows
choco install adb
winget install Google.AndroidSDKPlatformTools
adb version
```
```bash
# Ubuntu / Debian
sudo apt update
sudo apt install android-tools-adb android-tools-fastboot
adb version
```

| 용도 | 명령 |
| --- | --- |
| 기기 확인 | `adb devices` |
| CCS 연결(CCS 차량 전용, Devbox/Emulator 미지원) | `adb shell settings put secure hmg_is_ccs_account_linked 1`<br>`adb shell settings put global hmg_provisioning_status 4` |
| 화면 캡처 | `adb exec-out screencap -p > ~/Desktop/screen_$(date +"%Y%m%d_%H%M%S").png` |
| 화면 녹화 시작(Ctrl+C로 종료) | `adb exec-out screenrecord --output-format=h264 - > ~/Desktop/screen.h264` |
| mp4 변환(`brew install ffmpeg`) | `ffmpeg -framerate 60 -i ~/Desktop/screen.h264 -c copy ~/Desktop/screen_$(date +"%Y%m%d_%H%M%S").mp4` |
| 앱 설치(-d 다운그레이드 허용, -r 원문 표기 "로컬 정보 제거") | `adb install -d -r [apk파일주소]` |
| 설치 대안 | `adb push [apk파일주소] /data/local/tmp/`<br>`adb shell pm install -d -r /data/local/tmp/[apk파일이름]` |
| 패키지 찾기 | `adb shell pm list packages \| grep [패키지명 일부]` |
| 런처 실행(GNB 실행과 유사) | `adb shell monkey -p [패키지이름] -c android.intent.category.LAUNCHER 1` |
| Activity 직접 실행 | `adb shell am start -n [패키지이름]/[Activity 경로]` |
| 삭제 / 패키지 비우기 | `adb uninstall [패키지이름]` / `adb shell pm clear [패키지이름]` |
| 강제 종료 | `adb shell am force-stop [패키지이름]` |
| 텍스트 입력 | `adb shell input text "[텍스트]"` |
| 주행 규제 ON/OFF | `adb shell cmd hmg_car_service --service drivingstate driving_regulation on` / `off` |
| 기어 변경 | Pleos Connect Virtual Console 사용(`/docs/pleos-only/app-test-guide/virtual-console-guide`) |

유틸리티 모드 변경(먼저 차속 0 / Parking Brake On / Gear P 수행):

```sh
adb shell echo -e -n "\x83\x50\x10\x00\x00\x02\xAA\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x02\x00\x00\x00" > /dev/vcs_simulator_rx
```

---

## 8. Gleo AI 앱 연동 가이드 (Gleo Interface)

### 8.1 Core Concepts

출처: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts

- Gleo Interface: Gleo AI와 타사 앱이 차량 내 음성 명령 처리를 위해 상호작용하는 방식. **function calling** 기반(OpenAI function calling과 유사, Android 지원을 위한 스키마 확장).
- 앱은 호출 가능 function과 호출 방식을 정의한 JSON 파일 **App Schema**를 등록.
- Gleo AI → **Gleo Action**(Intent)으로 앱 호출 → 앱 → **Callback**(Broadcast)으로 응답 → Gleo AI가 최종 응답 생성.

### 8.2 App Schema

출처: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability

필드:
- `packageName`(string): Android 패키지명
- `appDescription`(string): 앱 주요 목적의 간결한 설명
- `version`(string): Pleos 앱 마켓 등록 버전
- `actionCalls`(array, **최대 3개**)
  - `type`: `"function"` 고정
  - `function`(object)
    - `component`(기본 `"activity"`): `"activity"` → `context.startActivity()` (포어그라운드 UI/사용자 상호작용), `"service"` → `context.sendBroadcast()` (UI 없는 백그라운드)
    - `name`: 고유 이름, **모두 대문자 + 단어 구분 밑줄(_)**
    - `description`: 목적·역할 설명 문장
    - `parameters`: `type`=`"object"` 고정, `properties`(각 parameter의 `type`, `description`, 선택 `enum`), `required`(array<string>)
- (예시에는 `invokeWord` 필드도 등장: `"invokeWord" : "리마인더"`)

작성 가이드라인:
- 음성 호출은 최대 3개이며 그 외 기능은 화면 상호작용으로 사용.
- function 이름 고유, 단독으로 사용자 명령을 수행하도록 설계, 동사만 쓰지 말고 2~3 단어로 대상 포함.
- 모든 description은 **영어**, Gleo 동작 조작/타 앱 억제 문구 금지, **구체적 예시 포함 금지**, 한 문장(최대 두 문장).
- parameter `type`: string, number, boolean, array 중 **하나만**(`["string","null"]` 불가). `enum`은 문자열만, `null` 미지원.
- parameter 이름은 LLM이 이해하기 쉽게.
- `required`에는 필수 parameter만(선택적 제외). 채워짐 보장은 아니나 성공률 상승.

예시(원문):

```json
{
  "packageName": "ai.dot42.pleos.reminderapp",
  "appDescription": "A reminder app that lets you schedule tasks and receive notifications at the right time.",
  "invokeWord" : "리마인더",
  "version" : "1.0.0",
  "actionCalls": [
    {
      "type": "function",
      "function": {
        "name": "ADD_REMINDER",
        "description": "Add a reminder that will notify the user at the given date and time.",
        "parameters": {
          "type": "object",
          "properties": {
            "title": {
              "type": "string",
              "description": "Title of the reminder."
            },
            "dateTime": {
              "type": "string",
              "description": "Date and time to trigger the reminder notification, in ISO 8601 date and time format."
            }
          },
          "required": ["title", "dateTime"]
        },
        "component": "activity"
      }
    },
    {
      "type": "function",
      "function": {
        "name": "SEARCH_REMINDER",
        "description": "Search for a reminder that matches or contains the search term.",
        "parameters": {
          "type": "object",
          "properties": {
            "searchTerm": {
              "type": "string",
              "description": "The keyword for searching a reminder."
            }
          },
          "required": ["searchTerm"]
        },
        "component": "activity"
      }
    },
    {
      "type": "function",
      "function": {
        "name": "DELETE_REMINDER",
        "description": "Delete a reminder from the list of reminders.",
        "parameters": {
          "type": "object",
          "properties": {
            "searchTerm": {
              "type": "string",
              "description": "The title or keyword for deleting a reminder."
            }
          },
          "required": ["searchTerm"]
        },
        "component": "activity"
      }
    }
  ]
}
```

### 8.3 Schema 전달 (Deliver Schema)

출처: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema

- 파일명 반드시 `schema.json`, 위치 `assets/schema.json`.
- 전달 후 가이드라인 준수 검토 → 승인 시 통합 자동 활성화.
- 각 function마다 `AndroidManifest.xml`에 action filter 정의:

```xml
<activity android:name=".YourActivity">
    <intent-filter>
        <action android:name="ADD_REMINDER" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

- Gleo AI 앱 부팅 시 `AssetManager`로 모든 시스템/타사 앱의 `assets/schema.json`을 스캔해 DB 저장(**부팅 시 1회**).
- (AAOS) 앱 설치(ADB install 또는 마켓)/삭제 시 `schema.json`을 읽어 DB 갱신.

### 8.4 Gleo Action (Intent 변환)

출처: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples

"오늘 오후 3시에 미팅 리마인더를 추가해줘" → Gleo AI 생성 JSON:

```json
{
  "packageName": "ai.dot42.pleos.reminderapp",
  "action": "ADD_REMINDER",
  "actionParameters": {
    "title": "미팅",
    "dateTime": "2025-02-20T06:00:00Z"
  }
}
```

변환 규칙: `action` → Intent action, `packageName` → 대상 앱 지정, `actionParameters`는 파싱되어 Intent에 추가(putExtra). 예시 코드에서는 `"ACTION_PARAM"` extra에 JSON 문자열로 전달됩니다.

`component="activity"` — 송신(Gleo AI):

```kotlin
val intent = Intent()
intent.action = "ADD_REMINDER"
intent.putExtra(
    "ACTION_PARAM",
    "\"{\"title\":\"미팅\", \"dateTime\":\"2025-02-20T06:00:00Z\"}\""
)
intent.setPackage("ai.dot42.pleos.reminderapp")
context.startActivity(intent)
```

수신(3rd-party App):

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ...
        val bundle = intent?.extras
        bundle?.let { data ->
            runCatching {
                val actionParam = data.getString("ACTION_PARAM")
                // ...
            }.onFailure {
                Log.e(logTag, "Failed to get intent extras: $it")
            }
        }
    }
}
```

`component="service"` — 송신은 동일하되 `context.sendBroadcast(intent)`. 수신:

```kotlin
class CustomReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action == "ADD_REMINDER") {
            val rawJson = intent.getStringExtra("ACTION_PARAM") ?: "{}"
            try {
                // JSON parsing
                val json = JSONObject(rawJson)
                val title = json.optString("title", "No Title")
                val dateTime = json.optString("dateTime", "No DateTime")
                // To Do..
                Log.d("ReminderReceiver", "Title: $title, DateTime: $dateTime")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ReminderReceiver", "Failed to parse actionParameters")
            }
        }
    }
}
```

### 8.5 Callback Schema

출처: https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback

필드:
- `functionName`(string, 필수): Callback을 발생시킨 action 이름(App Schema의 actionCalls → function → name과 일치)
- `status`(enum, 필수): `"success"` | `"asking"` | `"fail"`
- `message`(string, 필수): 완전한 **영어** 문장(검색어·장소·항목명 등은 타 언어 포함 가능)
- `mode`(enum, 선택, 기본 `"voice"`): `"voice"` 음성 출력 / `"beep"` 효과음만 / `"silent"` 무음(앱이 자체 TTS·음악 재생 시)
- `data`(string, 선택이나 **강력 권장**): JSON 객체 배열 문자열. 응답 생성에 필요한 정보만 포함. silent/beep에서도 맥락 유지를 위해 포함 권장.

status 의미:
- `success`: 정상 처리(결과 없음도 포함). 예: "The search was completed, but no matching results were found."
- `asking`: 필수 parameter 누락, 선택 필요, 사용자 확인 필요 등 후속 상호작용 필요.
- `fail`: 비즈니스 로직 실패 + 시스템 에러(app crash, network failure, timeout). **Gleo는 7초 후 Action을 취소(timeout)**.

예시:

```json
{
  "functionName": "ADD_REMINDER",
  "status": "success",
  "message": "The reminder was successfully added.",
  "data": "[ { \"title\": \"미팅\", \"dateTime\": \"2025-02-20T06:00:00Z\" } ]"
}
```
```json
{
  "functionName": "ADD_REMINDER",
  "status": "asking",
  "message": "A registration date is required.",
  "data": "[ { \"missingField\": \"dateTime\" } ]"
}
```
```json
{
  "functionName": "ADD_REMINDER",
  "status": "fail",
  "message": "It exceeds 10 items, so it cannot be added."
}
```
```json
{
  "functionName": "ADD_REMINDER",
  "status": "fail",
  "message": "A timeout occurred while processing the request."
}
```
```json
{
  "functionName": "PLAY_FREQUENCY",
  "status": "success",
  "message": "Starting the requested station now.",
  "data": "[ { \"stationName\": \"SBS 파워FM\", \"frequency\": \"107.7MHz\" } ]",
  "mode": "silent"
}
```
```json
{
  "functionName": "PLAY_MUSIC",
  "status": "success",
  "message": "Playback started.",
  "data": "[ { \"track\": \"Love Dive\", \"artist\": \"IVE\" } ]",
  "mode": "beep"
}
```

`silent` 시 Gleo AI는 실제 사용자 상호작용에 접근할 수 없어 Callback에 전적으로 의존합니다.

**Callback 전송 코드** — Broadcast action `ai.pleos.playground.intent.action.APP_ACTION_RESPONSE`, extras `packageName`, `response`(JSON 문자열):

```kotlin
import android.content.Intent
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

fun sendGleoBroadcast(
    functionName: String,
    status: String,
    message: String,
    data: String? = null
) {
    runCatching {
        val jsonObject = JSONObject().apply {
            put("functionName", functionName)
            put("status", status)
            put("message", message)
            data?.let { put("data", it) }
        }
        Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
            putExtra("packageName", packageName)
            putExtra("response", jsonObject.toString())
        }.also {
            sendBroadcast(it)
            Log.d("ThirdPartyApp", "respondAction success jsonObject: $jsonObject")
        }
    }.onFailure {
        Log.e("ThirdPartyApp", "respondAction onFailure >> $it")
    }
}
```

```kotlin
sendGleoBroadcast(
    functionName = "SWITCH_BAND",
    status = "success",
    message = "Successfully switched to the requested radio channel.",
    data = """[{"frequency":"91.9MHz","frequencyBand":"FM","stationName":"MBC","channelName":"MBC FM4U"}]"""
)
```

### 8.6 Use case: 라디오 앱

출처: https://document.pleos.ai/docs/pleos-only/app-integration-guide/use-cases/radio-app

App Schema(`ai.umos.mediaapp`, 3 functions):

```json
{
  "packageName": "ai.umos.mediaapp",
  "appDescription": "An app for streaming live radio stations.",
  "actionCalls": [
    {
      "type": "function",
      "function": {
        "name": "SWITCH_BAND",
        "description": "Changes between FM and AM radio bands.",
        "parameters": {
          "type": "object",
          "properties": {
            "band": {
              "type": "string",
              "enum": ["FM", "AM"],
              "description": "The radio band to switch to."
            }
          },
          "required": ["band"]
        },
        "component": "service"
      }
    },
    {
      "type": "function",
      "function": {
        "name": "SEARCH_STATION",
        "description": "Searches radio stations based on a keyword or frequency.",
        "parameters": {
          "type": "object",
          "properties": {
            "searchTerm": {
              "type": "string",
              "description": "The keyword to search for in station names or frequencies."
            }
          },
          "required": ["searchTerm"]
        },
        "component": "activity"
      }
    },
    {
      "type": "function",
      "function": {
        "name": "PLAY_FREQUENCY",
        "description": "Plays the specified radio frequency.",
        "parameters": {
          "type": "object",
          "properties": {
            "frequency": {
              "type": "number",
              "description": "The radio frequency to play. For FM band it can be both in MHz and kHz. AM frequency should be given in kHz."
            }
          },
          "required": ["frequency"]
        },
        "component": "service"
      }
    }
  ]
}
```

수신 처리(원문):

```kotlin
private fun handleIntent(intent: Intent?) {
    if (intent == null) return
    val bundle = intent.extras
    bundle?.let { data ->
        runCatching {
            val actionParam = data.getString("ACTION_PARAM") ?: "{}"
            val json = JSONObject(actionParam)
            when (intent.action) {
                "SEARCH_STATION" -> {
                    val searchTerm = json.optString("searchTerm", "Unknown")
                    Log.d("RadioReceiver", "Searching radio station with search term: $searchTerm")
                    // TODO: Implement logic to play the radio based on searchTerm
                }
                "SWITCH_BAND" -> {
                    val band = json.optString("band", "Unknown")
                    Log.d("RadioReceiver", "Switching band to: $band")
                    // TODO: Implement logic to switch band
                }
                "PLAY_FREQUENCY" -> {
                    // TODO: Implement for PLAY_FREQUENCY
                }
                else -> {
                    Log.w("RadioReceiver", "Unknown action received: ${intent.action}")
                }
            }
        }.onFailure {
            Log.e("RadioReceiver", "Failed to get intent extras: $it")
        }
    }
}

...

private fun sendCallback(functionName: String, status: String, message: String) {
    val response = JSONObject()
    try {
        response.put("functionName", functionName)
        response.put("status", status)
        response.put("message", message)
        response.put("data", data) // optional
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    // Send response back to Gleo AI (e.g., via an API or broadcast)
}
```

- `SEARCH_STATION(searchTerm="SBS Power FM")` 수신 → LLM/Search SDK 또는 SQLite로 채널 검색 후 주파수 재생.

Callback 예시:

```json
{
  "functionName": "SEARCH_STATION",
  "status": "success",
  "message": "The radio has started playing successfully.",
  "data": "[\n  {\n    \"frequency\": \"91.9MHz\",\n    \"frequencyBand\": \"FM\",\n    \"stationName\": \"MBC\",\n    \"channelName\": \"MBC FM4U\"\n  }\n]"
}
```
```json
{
  "functionName": "SEARCH_STATION",
  "status": "fail",
  "message": "Failed to play the requested radio. Please check the station name or frequency."
}
```
```json
{
  "functionName": "SEARCH_STATION",
  "status": "asking",
  "message": "Multiple stations match your request. Please specify the frequency or channel name.",
  "data": "[\n  {\n    \"frequency\": \"91.9MHz\",\n    \"frequencyBand\": \"FM\",\n    \"stationName\": \"MBC\",\n    \"channelName\": \"MBC FM4U\"\n  },\n  {\n    \"frequency\": \"95.9MHz\",\n    \"frequencyBand\": \"FM\",\n    \"stationName\": \"MBC\",\n    \"channelName\": \"MBC Standard FM\"\n  }\n]"
}
```

---

## 9. 디자인 시스템 (Pleos Connect UI Design Guide)

### 9.1 기본 디자인 원칙 / 인터페이스 가이드

출처: https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro , https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/designing-apps-for-driving

- Connect-S, Connect-W 등 폼팩터에 유연하게 대응하도록 디자인 시스템 업데이트. Connect-W는 좌석별 특화 UI·인터랙션 제공.
- **Easy & Safe**: 시각·인지 부담 최소화. 운전 중 동영상·애니메이션 이미지·**텍스트 자동 스크롤** 금지. **한 손 조작**(양손 사용 금지), 단순 조작, 손·발 움직임 유도 금지.
- **Consistency**: 아이콘·용어·인터랙션 일관성, 기본 탑재 UI와 일관 유지.
- **Glanceability**: 운전자는 **1~2초** 내 정보 파악 → 핵심 정보만. 입력 후 시스템 응답 **0.25초 이내**, 지연 시 응답 중 상태를 시각적으로 표시.
- **색상·대비**: 중요 정보는 강한 대비, 과도한 색상 지양. 아이콘·텍스트·이미지 대비율 **4.5:1 이상**.
- **타이포·가독성**:
  - 영어 단일 메시지 **120자 또는 24단어 이하**
  - 일본어 단일 메시지 **30자(영문 기준) 이하**
  - 한 줄 **80자(영문 기준) 이하**
  - 문장/단락 **3줄 이하**
  - 중요 정보(속도, 내비 안내, 경고) 기본 텍스트 **높이 5 mm 이상**, 보조 텍스트 **최소 3 mm**
- **아이콘·피드백**: 국제 통용 아이콘 + 텍스트 라벨, 햅틱/음성 피드백.
- **알림**: 간결, 운전자 필요 시에만. 알림으로 광고 표시 금지(광고주명·제품명 외 텍스트 광고 금지).
- **유연성/확장성**: 야간/주간 모드(밝기·대비 자동 조정), Gleo AI 음성 인터랙션 활용(핸즈프리, 대화 시작/종료/중단은 수동 가능), OTA 업데이트 고려, 다국어 UI, 다양한 해상도·비율 대응.
- 디자인 리소스: `/img/Pleos_Connect_display_guide_215.fig`, `/img/Pleos_Connect_design_assets_215.fig`
- 하위 가이드 링크: Colors(`foundations/colors`), Typography(`foundations/typography`), Iconography(`foundations/contents-creation/iconography`) — 해당 페이지는 입력에 포함되지 않음.

### 9.2 Display Layout

출처: https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout

화면 구성 요소:
1. **Status bar**: Driving status, Gleo AI·검색, 시계, 네트워크, 마이크 상태, 알림 센터, 프로필 선택
2. **Driving view**: Telltale, 속도, 차량 상태 등 주행/안전 정보
3. **GNB(Global Navigation Bar)**: App library, 즐겨찾는 앱, 공조(Climate) 컨트롤
4. **Application window**: 1개 또는 2개 앱 표시 → 가로 전용 게임 등 단일 모드 앱을 제외한 모든 앱은 **Landscape·Portrait 두 레이아웃** 모두 지원 필요

- 레이어: Status bar, GNB의 App library·공조 패널, 키보드 등 System UI는 앱보다 상위 → 일시적으로 앱 윈도를 가릴 수 있음.
- 화면 유형:
  - **Connect-S**(Standard): 17인치, 14.6인치, 12.9인치(기존 Connect-L, S로 재분류)
  - **Connect-W**(Wide): 가로가 긴 고급 차종용. 차량 상태에 따라 세로 확장 → **Connect-W-Extended**
  - **Connect-WF**(Waterfall): Wide 대비 작은 해상도, 핵심 기능 제공
- 픽셀 해상도·그리드 수치는 원문 이미지에만 존재(텍스트 미제공).

### 9.3 Window Policy

출처: https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window

| 항목 | Connect-S | Connect-W |
| --- | --- | --- |
| 화면 구성 | 앱 2개: 1:1 / 앱 1개: 2:0 (기본 1:1) | 2:1 또는 1:2 (기본 2:1) |
| 기본 화면 | 최초 실행·재탑승 모두 Primary: Navigation 앱 | 최초: Primary Navigation, Secondary Media 앱 / 재탑승: Primary Navigation, Secondary 마지막 사용 Media 앱 |
| 앱 실행 시 | 신규 앱은 이전 앱 orientation을 따름(landscape 1개 → landscape, portrait 2개 → primary에 portrait). 사라진 앱은 종료 아님, 표시 앱 종료 시 최근 순 재표시 | 신규 앱은 항상 landscape로 primary window에 표시. 기존 primary → secondary로, secondary 앱은 사라짐(종료 아님) |
| 위치 변경 | 3 핑거 스와이프 또는 윈도 바 탭 | 동일 |
| 크기 변경 | 3 핑거 스와이프, 윈도 바 드래그, 앱 윈도 버튼 탭으로 landscape↔portrait | 동일 |
| 앱 종료 | 3 핑거 스와이프. 오른쪽→왼쪽으로 최근 앱 순 표시(landscape 종료 시 landscape, portrait 종료 시 portrait). Navigation 앱은 종료 불가, 모두 종료 시 초기 화면 | 3 핑거 스와이프. primary 종료 시 secondary가 orientation 유지한 채 primary로, secondary 종료 시 최근 앱이 종료 앱 orientation으로 표시. 내비 앱 종료 불가 |

Connect-W 사양:
- 최대 실행 앱 2개.
- App launcher(App library)는 운전석·조수석 모두 표시.
- Status bar는 운전석 GNB와 앱 영역 사이.
- 조수석 App library에서 실행한 앱은 조수석 윈도에서만 실행.

알아두기:
- 프리로딩 앱은 landscape/portrait 모두 지원.
- Driving view의 landscape view는 삭제, resize handle 미제공.
- Deep link(App to App): App A에서 App B 실행 시 A를 덮고 실행, B 종료 시 A로 복귀.
- 후방카메라 등 3:0 window full screen 예외케이스 적용 예정.

### 9.4 Design Token

출처: https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/design-token

- 스타일 속성 값(색상, 폰트, 간격, 그림자 등)을 코드로 추상화한 단위, Single Source of Truth.
- 목적: 일관된 UI, 디자이너-개발자 공통 언어, 유지보수 비용 절감, 다크/라이트/브랜드 테마 전환 용이.
- 이름 규칙: Prefix **`ivi`**, **snake case**, 사용 위치를 이름으로 인지 가능해야 함, 축약 금지(Do `vertical` / Don't `v`, Do `vehicle_setting` / Don't `vs`). 구조도는 원문 이미지.

### 9.5 Theme

출처: https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/theme

- 테마는 아이콘, 폰트, 컬러 중심으로 브랜드 아이덴티티 유지에 사용. Types / Theme selected / Theme samples는 이미지로만 제공.

### 9.6 Experiences

#### Driving view
출처: https://document.pleos.ai/docs/connect/guide/docs-design/Experiences/driving-view

운전자용 차량 상태·주행 정보 영역.
1. **Indicator**: Telltale, 도로표지판 정보, 에너지 정보, 자율주행 indicator, 회생제동 indicator(상황별), 도어 잠김/잠금해제 switch(상황별 미노출)
2. **Driving information**: 변속단(상황별), 주행 속도, 에너지/퍼포먼스 게이지, 주행거리계, miniTBT(상황별), 드라이빙모드
3. **View contents**: 차량 외부 컨트롤, 근접 센서·주변 물체, 차선·노면 표시

#### Gleo AI
출처: https://document.pleos.ai/docs/connect/guide/docs-design/Experiences/gleo-ai

- 개인형 모빌리티 어시스턴트. 이름은 갈릴레오 모티브(42dot 표현). Chrome은 대화 상태를 빛으로 표현, **lottie file**로 status별 애니메이션.
- **Status**

| Status | 설명 |
| --- | --- |
| Sleep | 대화 중이 아닌 대기 |
| Listening_Passive | 음성 입력 시작, 발화 전 대기. 미입력 2초 후 "운전석에서 듣는 중..." 등 좌석 메시지 표시 |
| Listening_Active | 사용자 발화 입력 중 |
| Thinking | 발화 종료 후 처리 중 |
| Asking | 응답 전 추가 입력을 위해 되묻는 상태 |
| Speaking | 최종 응답 제공 |
| Standby | 대화 종료 후 Sleep 직전, 응답 화면 있으면 5초(없으면 0초) 유지 후 자동 Sleep, 화면 내려감 |

- **Elevation**: OS layer상 GNB 다음 하위 layer. PTT/wake word 또는 proactive suggestion으로 시작. 다른 앱 뷰를 일시적으로 가릴 수 있음. 조건/외부 이벤트로 닫힐 수 있음.
- **Layout**: Chrome(상태 표시), Bubble(사용자 발화/Gleo 응답), Gleo Panel(Template: list·card 타입 / Chips: 선택지). 구성 예: Speaker Bubble + Panel, Panel, Event Conflict(이미지).
- **Touch**: LIST item·Chips 터치 시 지정 기능 실행(예: 그래/아니, 답장해줘, 다음 메시지 읽어줘). Panel 상단 드래그 또는 Panel 외 영역 터치 → 대화 종료. 하단 드래그 → Gleo AI App으로 전환.
- **CCP**: CCP 가능 시 focus 표시(rotate, tilt, push 대상). Gleo 대화는 OS CCP 정책상 '팝업'과 동일 처리 → tilt 제한, Gleo가 다른 앱보다 우선 포커스. focus는 패널에 터치 가능한 선택 대상이 있을 때만 표시. Gleo AI 앱은 OS CCP 정책을 따르며 상단 타이틀/하단 히스토리 영역 구성, 최신 대화 버블이 default focus.
- **Panel Template**: display template + 하단 chips.
- **Multi Screen Size**: Standard 14.6, Standard 17, Wide Standard, Wide Extended, Waterfall. Wide에서는 발화 좌석을 인식해 가까운 위치에 Gleo 표시.

### 9.7 컴포넌트

입력 폴더에는 개별 컴포넌트 페이지가 포함되어 있지 않습니다. 원문에서 링크로 확인되는 항목만 나열합니다(상세 사양 미확인).
- **Basic Button** — `/docs/connect/guide/docs-design/components/actions/basic-button` (버튼 크기·터치 영역 간격 기준이 정의된 페이지로 주행 안전 가이드에서 참조)
- Foundations: Colors(`/foundations/colors`), Typography(`/foundations/typography`), Iconography(`/foundations/contents-creation/iconography`)
