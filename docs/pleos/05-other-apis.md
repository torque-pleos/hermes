# Pleos 비(非) Connect SDK API 레퍼런스 요약

> 대상: Pleos Platform SDK, Vehicle Data API, Fleet API, SDVOS API
> 출처: https://document.pleos.ai (다운로드한 markdown 기준). 문서에 없는 내용은 "문서에 명시 없음"으로 표기했습니다.

---

## 0. 한눈에 비교

| 제품 | 형태 | 실행 위치 / 대상 | 인증·접근 | Pleos Connect SDK와의 차이 |
| --- | --- | --- | --- | --- |
| **Pleos Platform SDK** | Android System API (Java/Kotlin, `compileOnly` stub) | **차량 내** 승인된 **privileged app** | `signature\|privileged` Pleos permission (manifest 선언 + 플랫폼 권한 부여). OAuth 없음 | Connect SDK는 일반 인포테인먼트 앱용 AAOS 기반 SDK. Platform SDK는 privileged 앱 전용 System API이며 CloudHub, Telephony, OTA, Diagnostic 등 플랫폼 수준 기능을 노출 |
| **Vehicle Data API** | REST (`https://api.pleos.ai`) | **서버/클라우드** (사업자 서비스 또는 개인 차량 소유자) | OAuth 2.0 기반 Bearer token (사업자: authorization code / 개인: 내 차량 API 키의 Client ID/Secret으로 `personal-token` 발급) | 차량 앱이 아니라 원격으로 현대/기아/제네시스 커넥티드 카 텔레매틱스 데이터를 조회. **현재 유럽만 지원** |
| **Fleet API** | REST + Webhook | **서버/클라우드** (Pleos Fleet 기반 앱) | REST: API Key 헤더 `x-42dot-client-id`. Pleos Playground에서 Client ID / Client Secret / Project ID 발급 | 차량·단말 관리 및 실시간 차량 데이터 수신(Webhook) |
| **SDVOS API** | (가이드/튜토리얼/샘플 구조만 확인됨) | 문서에 명시 없음 | 문서에 명시 없음 | 공개 접근 불가 — 모든 페이지가 `/404`로 리다이렉트(접근 제한) |

참고: Pleos Connect SDK 개요 — Pleos Connect 환경의 인포테인먼트 앱 개발 도구, Google AAOS 기반. 모듈: Vehicle SDK, NaviHelper SDK, TextToSpeech SDK, SpeechToText SDK, FusedLocation SDK. API 정책은 AAOS에 동일·유사 기능이 있으면 AAOS에 align, AAOS로 수용된 기능은 최소 2개 major version 동안 backward compatibility 보장. 퍼미션은 Pleos App Market과 사전 협의·승인된 범위 내에서만 사용 가능(미승인 퍼미션 사용 시 앱 심사 불통과). 도메인별 `checkXXXCapabilities`로 지원 여부 확인.
- Source: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/

---

## 1. Pleos Platform SDK

### 1.1 개요 · 대상
- Pleos Platform SDK는 **승인된 privileged 앱**에 차량 기능을 **안정된 Android System API**로 제공합니다.
- **Stub-first 모델**: 앱은 compile-time stub에만 의존하고, 실제 기능은 차량에 설치된 Pleos Platform runtime이 제공. SDK runtime 구현을 APK에 포함하지 않음.
- 앱은 backend 위치/package/transport를 몰라도 되고 `PleosManager`를 통해 domain manager만 사용.
- 문서 구성: 시작하기 → 핵심 개념 → 가이드 → API 레퍼런스 → 정책 → Permission matrix → Feature matrix → Compatibility matrix.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/intro , https://document.pleos.ai/api-reference/pleos-platform-sdk/get-started

**Connect SDK와의 차이 (문서 근거로 정리)**
| 항목 | Pleos Connect SDK | Pleos Platform SDK |
| --- | --- | --- |
| 대상 앱 | Pleos Connect 인포테인먼트 앱 (AAOS 기반) | 승인된 privileged 앱 (System API) |
| 권한 | App Market 사전 승인 퍼미션 | `ai.pleos.permission.*` (`signature\|privileged`, 일부 `signature`), 앱 서명·privileged 등록·제품별 allowlist 필요 |
| 기능 범위 | Vehicle, NaviHelper, TTS, STT, FusedLocation | CloudHub, Telephony, Wi-Fi, Network Control, OTA, Diagnostic, Certificate, Profile, ETCS 등 (아래 capability 표) |
| 지원 확인 | `checkXXXCapabilities` | `PleosFeatures.isSupported()` + `getDomainAvailability()` |

### 1.2 Setup · 의존성 좌표
- Maven 저장소 (Basic 인증):
  ```kotlin
  maven {
      url = uri("https://emu-nexus-playground.pleos.ai/repository/furie-release/maven")
      credentials {
          username = localProperties.getProperty("PleosMavenUsername")
          password = localProperties.getProperty("PleosMavenPassword")
      }
      authentication { create<BasicAuthentication>("basic") }
  }
  ```
  - `PleosMavenUsername` / `PleosMavenPassword`는 `local.properties`에 설정하며 **각 회사별 담당자에게 문의**. 버전 관리에 포함 금지.
- 의존성 (정확한 버전, `compileOnly`, dynamic version·version range 금지):
  ```kotlin
  dependencies {
      compileOnly("ai.pleos:pleos-manager-stubs-system:1.0.0-20261023")
  }
  ```
- 지원하지 않는 방식: runtime 구현을 APK에 포함, dynamic version, 게시 artifact 재패키징, 비공개 구현 class 직접 의존.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/get-started

### 1.3 최신 버전 · 호환성
| 항목 | 값 |
| --- | --- |
| 최신 SDK 버전 | `1.0.0-20261023` (외부 공개 릴리스, 2026-10-23, Tag `Platform_SDK.v1.0.0-20261023_045238`, Baseline `1.0.0.dev.369`) |
| Pleos Connect | 3.5 |
| Android API | 34 |
| 제품별 지원 범위 | unspecified (게시 자료에 명시 없음) |
| Feature matrix | `DIAGNOSTIC_LOGGING`, `WIFI` (source evidence only, runtime availability는 미추론) |

- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/compatibility-matrix , https://document.pleos.ai/api-reference/pleos-platform-sdk/feature-matrix

### 1.4 핵심 개념
**PleosService 아키텍처** — 흐름: `Versioned SDK Stub → Runtime Manager → Root Facade → Domain Frontend → Remote Domain Component 또는 Local Domain Service`
- Root Service Facade: 단일 진입점, domain routing, 공통 접근 제어, Availability Registry 접근.
- Remote Domain Service: 공통 Remote Domain Component(proxy/connector/adapter + lifecycle). 일부는 Optional(제품 구성에 따라 연결 가능 또는 unavailable frontend).
- Local Domain Service: Calibration, Diagnostic, Power 등 — backend 연결 없이 PleosService 내부 구현 사용.
- Feature Support Catalog: PleosService 연결과 독립적으로 제품 구성 정보를 제공.
- 패턴: Facade, Proxy, Adapter, Null Object/Fallback, Observer, Ports and Adapters, Lazy Initialization.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/concepts/pleos-service-architecture

**Permission / Feature / Availability 구분**
| 개념 | 의미 | 대표 신호 | 앱 동작 |
| --- | --- | --- | --- |
| Permission | 호출자가 operation을 실행할 권리 | `SecurityException` | 기능 실행 안 함, 권한/통합 설정 확인 |
| Feature | 제품이 optional capability를 지원하는가 | `PleosFeatures.isSupported()==false`, `UnsupportedOperationException` | 숨기거나 승인된 fallback |
| Availability | 지원 domain이 현재 동작 가능한가 | `DomainAvailability.UNAVAILABLE` | 복구 가능 상태로 재시도·안내 |
| 잘못된 인자 | API별 인자/결과 계약 | — | 요청 수정 |
- 오류 분류(Error handling): Unsupported / Unavailable / Permission denied / Operation failure. 미지원에는 반복 재시도 금지.
- `UNAVAILABLE`만으로 미지원과 일시 장애를 구분하지 않음. 알 수 없는 상태도 `UNAVAILABLE`. Certificate API는 `PleosDomain` availability 대상이 아님.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/concepts/domain-availability , https://document.pleos.ai/api-reference/pleos-platform-sdk/concepts/error-handling , https://document.pleos.ai/api-reference/pleos-platform-sdk/concepts/permissions

### 1.5 권한(Access) 모델
- 서비스 연결: `PleosManager.connect()`에 `ai.pleos.permission.BIND_PLEOS_SERVICE` 필요.
- Manager 획득: 별도 capability 검사 없음. **예외**: `getPleosLocationManager()` → `CAR_LOCATION`, `getPleosNaviLocationManager()` → `CAR_NAVI_LOCATION` 필요 (frontend 미제공 시 `null`, 권한 부족 시 `SecurityException`).
- Leaf operation: 각 method의 `@RequiresPermission`.
- 대부분 `signature|privileged`; `MANAGE_CERTIFICATE_AUTHORITY`는 `signature` 전용. 일반 Android runtime permission이 아니므로 `requestPermissions()` 사용 안 함.
- Manifest 선언만으로 부여 보장 안 됨 (앱 서명, privileged 등록, 제품별 allowlist 필요).
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/get-started , https://document.pleos.ai/api-reference/pleos-platform-sdk/policies/pleos-platform-sdk-policy

### 1.6 기본 사용 흐름
1. 정확한 SDK 버전 `compileOnly` 선언
2. `BIND_PLEOS_SERVICE` + 필요한 capability permission 선언
3. `PleosManager.connect(context, PleosConnectionCallback)` → `onSuccess(PleosManager)` / `onFailure(PleosConnectionFailureReason)`; 종료 시 `PleosConnection.close()`
4. `PleosFeatures.isSupported(context, PleosFeature.WIFI)` 등으로 feature 확인
5. Domain manager 획득 (예: `getWifiManager()`, `getMediaManager()`)
6. `getDomainAvailability(PleosDomain.X)` / `addDomainAvailabilityListener()` 확인
7. API Reference의 permission·오류·lifecycle 계약에 따라 호출
- 연결 상태: `addConnectionStateListener()` / `removeConnectionStateListener()`. 연결 닫힌 뒤 accessor 호출 시 `IllegalStateException` 가능.
- 재연결 후 listener는 새로 획득한 manager에 다시 등록.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/guides/connection , https://document.pleos.ai/api-reference/pleos-platform-sdk/guides/domain-manager , https://document.pleos.ai/api-reference/pleos-platform-sdk/guides/feature-support , https://document.pleos.ai/api-reference/pleos-platform-sdk/guides/lifecycle

### 1.7 Capability 카탈로그 (주요 API 개요)
| Capability | 공개 진입점 | 제공 기능 |
| --- | --- | --- |
| CloudHub | `CloudHubManager` | Cloud readiness, HTTP, MQTT, provisioning, SMS |
| Connected Car Service Status | `ConnectedCarServiceStatusManager` | 서비스별 이용 가능 사유·유형별 활성 상태 게시/조회/구독 |
| Network Control | `NetworkControlManager` | Network 구성 조회·변경, traffic control |
| Telephony | `TelephonyManager` | Modem, SIM, call, message, data, engineering |
| Wi-Fi | `WifiManager` | 상태, scan, saved network, connect/disconnect |
| HID | `HidManager` | HID device/report 관찰, virtual HID |
| Hardware Input | `HardwareInputManager` | Hardware key event, input source 관찰 |
| Log | `LogManager` | Live/historical log, buffering |
| Media | `MediaManager` | WebRTC, audio effect, sound cue, special-event recording |
| Pleos Location | `PleosLocationManager`, `PleosNaviLocationManager` | DR 보정 위치 조회·구독, 내비 보정 데이터 입력 |
| NDAS | `NdasManager` | RoadGraph, PositionMetaData, RoadGraphPosition, NavigationGuidance 전송 |
| Profile | `ProfileManager` | User, active driver, digital key, NFC key |
| Trust & Authentication | `TrustAgentManager` | Managed user, PIN, credential authentication UI |
| OS Property | `OsPropertyManager` | Pleos typed system property 조회·변경 |
| Vehicle Property Vocabulary | (Manager 없음) | Vendor vehicle property identifier / value constant |
| Power | `PowerManager` | Vehicle reset, shutdown |
| Reservation | `ReservationManager` | SW update, charging, preconditioning 예약 |
| Diagnostic | `DiagnosticManager` | ECU/DTC 조회·삭제 |
| Calibration | `CalibrationManager` | Calibration 요청·상태 관찰 |
| Vehicle Status Event | `VehicleStatusEventManager` | Vehicle failure/unavailable status event |
| ETCS | `ElectronicTollCollectionManager` | 통행료 상태, 결제, transaction |
| OTA | `OtaManager` | SW update 상태, config, action, event |
| Certificate | `CertificateManager`, `CertificateAuthorityManager` | App-scoped certificate, device CA |
| UI | `NavigationUiManager`, `OverlayUiManager`, `ThemeManager`, `WindowStateManager` | Send-to-POI, overlay, theme, window/task state, task panel |

- `PnP Device`, `System Monitor`, `Preset Mode`, `Notification`은 현재 `PleosManager`에서 manager를 제공하지 않아 카탈로그에서 제외(정책 문서 기준).
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/get-started , https://document.pleos.ai/api-reference/pleos-platform-sdk/policies/pleos-platform-sdk-policy

### 1.8 도메인 가이드

#### CloudHub (`ai.pleos.cloudhub.*`)
- 차량 앱이 **HMC CCS 센터(서버)**와 연동하기 위한 API. 내부적으로 HTTP·MQTT 송수신 대행, **JWT 토큰 발급·갱신 요청**, 재시도, MQTT 연결 준비/세션 유지, Provisioning 데이터 DB 관리 수행.
- 하위 Manager: `HttpManager`, `MqttManager`(일반 MQTT + StateSync + Vehicle Push), `ProvisioningManager`, `SmsManager`(현재 송수신 미지원).

| 권한 (`ai.pleos.permission.`) | 기능 |
| --- | --- |
| `BIND_PLEOS_SERVICE` | PleosService 연결 |
| `READ_CLOUD_STATE` | 준비 상태 조회·알림 |
| `CONTROL_CLOUD_HTTP` | HTTP |
| `CONTROL_CLOUD_MQTT` | 일반 MQTT, StateSync, Vehicle Push |
| `MANAGE_CLOUD_PROVISIONING` | Provisioning 조회·변경 |
| `CONTROL_CLOUD_SMS` | SMS |

- `CloudHubManager`: `isReady()`, `addReadyListener()`/`removeReadyListener()`, `getHttpManager()`, `getMqttManager()`, `getProvisioningManager()`, `getSmsManager()`, `close()`.
- 공통 결과 `CloudHubResult` (`isOk()`, `getCode()`, `getMessage()`), `CloudHubResultCode`: `OK(0)`, `PERMISSION_DENIED(-1)`, `NOT_READY(-2)`, `ERROR(-3)`.
- **HTTP**: `requestSync()`, `request()`(비동기, `HttpListener.onResponse/onFailure`), `cancelRequest()`, `setHttpListener()`/`clearHttpListener()`. `HttpRequest`: `setDestination("CCS")`, `setUri()`, `setMethod(HttpMethod.GET|POST|PUT|DELETE|HEAD|PATCH)`, `addParameter()`, `addCustomHeader()`, `setBodyData(bodyData, multiBody)` (POST/PUT/PATCH는 Body 필수).
- **MQTT**: `publish(MqttPublishOptions)`, `subscribe(MqttSubscribeOptions, qos, RetainOptions)`, `unsubscribe()`, `getCommonTopics()`, `getSubscribedTopics()`, `getConnectionStatus()`, `getClientRegistrationDetails()`. 반환값 `>0` 작업 ID / `0` 미연결 / `-1` 실패. QoS 0/1/2. 발행 토픽에 `+`, `#` 금지. `MqttConnectionStatus`: `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `RECONNECTING`, `FAILED`.
- **StateSync / Vehicle Push**: manifest `<meta-data android:name="ai.pleos.cloudhub.APP_PROTOCOLS" android:resource="@raw/cloudhub_protocols" />` + `res/raw/cloudhub_protocols.json` (`schemaVersion: 1`, `protocols` 최대 256개, `type`은 `STATESYNC`/`VPUSH`, 파일 최대 64 KiB, ID는 서버 승인 값). `syncVehicleState(StateSyncType.GET|UPDATE|WATCH_SUBSCRIBE|WATCH_UNSUBSCRIBE, StateSyncOptions)`, `setVehiclePushListener()`, `respondToVehiclePush()` (`RPC_REQUEST`에만 응답, `NOTIFICATION`은 응답 안 함).
- **Provisioning**: `getStatus()`/`setStatusAsync()` (`ProvisioningStatus`: `UNKNOWN(-1)`…`ENROLLED(4)`…`EXHIBITION_CAR_DEPROVISIONED(8)`), `getEnvironment()` (`DEFAULT/DEVELOPMENT/STAGING/PRODUCTION`), 토큰: `getTokenInfo("CCS"|"SAS")`, `setTokenInfoAsync()`, `clearTokenInfoAsync()`, `isCcsAuthenticated()`, `isSasAuthenticated()` (만료까지 60초 초과 남아야 `true`). 토큰은 cloudsvc가 발급·갱신·저장 관리.
- **SMS**: `send()`는 현재 `MESSAGE_ID_FAILED(-1)` 반환, 콜백 미호출.
- 1.0.0-20261023에서 HTTP 요청의 앱 세션별 격리, 자원·리스너 정리, 취소 처리 버그 수정.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/domain-guides/cloudhub

#### Connected Car Service Status (`ai.pleos.connectedcar.status`)
- CCS App이 판단한 차량 공용 서비스 상태(서비스별 사유, 유형별 활성 상태)를 다른 앱과 공유. 도메인 `PleosDomain.CONNECTED_CAR_STATUS`.
- 권한: `READ_CONNECTED_CAR_SERVICE_STATUS`(조회·구독), `MANAGE_CONNECTED_CAR_SERVICE_STATUS`(게시).
- API: `setServiceAvailabilityReasons(Map<String,Integer>)`, `getServiceAvailabilityReason()`, `register/unregisterServiceAvailabilityListener()`, `setServiceTypeActive()`, `isServiceTypeActive()`, `register/unregisterServiceTypeStatusListener()`. 사유 예: `CONSENT_NOT_GIVEN`.
- 값은 영구 저장되지 않음(Service 재시작 시 미게시 상태). 호출은 동기 Binder IPC → worker thread에서 실행. CloudHub `isReady()` 불필요.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/domain-guides/connected-car-status

#### Pleos Location (`ai.pleos.location.*`)
- DR(Dead Reckoning) 보정 위치 및 내비게이션 보정 데이터. `android.location.LocationManager`와 별개.
- `PleosLocationManager`(`CAR_LOCATION`): 위치 조회·구독, calibration 상태, VSS-B, Replay·DLT. `PleosNaviLocationManager`(`CAR_NAVI_LOCATION`): 맵매칭, HIPPO raw, RTX 보정 데이터 입력, raw listener.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/domain-guides/pleos-location

#### NDAS (`ai.pleos.ndas`)
- NDAS(NVIDIA Drive Autonomous System)로 RoadGraph, PositionMetaData, RoadGraphPosition, NavigationGuidance 바이너리 전송: `sendRoadGraph()`, `sendPositionMetadata()`, `sendRoadGraphPosition()`, `sendNavigationGuidance()` + `SendCallback.onResult(boolean)`. 별도 도메인 권한 없음.
- **알려진 제한 (1.0.0-20261023)**: API 스텁만 제공. `getNdasManager()`는 `null` 반환, 전송 메서드는 `UnsupportedOperationException`.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/domain-guides/ndas , https://document.pleos.ai/api-reference/pleos-platform-sdk/release-notes

#### Navigation UI / Window State
- `NavigationUiManager`: 시스템 내비게이션의 Send-to-POI 요청 수신 (`SendToPoiData`: `poiName`, `address`, `latitude`, `longitude`). `BIND_PLEOS_SERVICE`만 필요.
- `WindowStateManager`: fullscreen, task resizing, overlay, task slot, active task, GNB, home-screen readiness, task panel. `TaskPanel` 값: `APP`, `APP_PASSENGER`, `EVENT_WIDGET`, `OTHER`, `UNKNOWN`, `UNSUPPORTED`. 지원 확인은 `isTaskPanelSupported()`. Task panel 조회·구독은 자기 UID task 대상, 보고 API는 SystemUI 전용.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/domain-guides/navigation-ui , https://document.pleos.ai/api-reference/pleos-platform-sdk/domain-guides/window-state

### 1.9 Permission matrix (요약)
- 공개 Domain API 클래스별 API 선언 / 권한 조합("필수", "모두 필요", "하나 이상 필요", "원본 선언 확인 필요") / 필요한 권한 / 권한 부여 조건을 표로 제공. 권한 선언은 권한 부여나 제품 지원을 보장하지 않음.
- 포함 클래스(1.0.0-20261023): CloudHub(`CloudHubManager`, `HttpManager`, `MqttManager`, `ProvisioningManager`, `SmsManager`), `ConnectedCarServiceStatusManager`, `HardwareInputManager`, `HidManager`, `PnpManager`, `PleosLocationManager`, `PleosNaviLocationManager`, `CalibrationManager`, `DiagnosticManager`, `DiagnosticReportManager`, Media(`AudioEffectManager`, `Equalizer`, `LoudnessEnhancer`, `Virtualizer`, `SpecialEventRecorderManager`, `SoundCueManager`, `PlaybackHandle`, `WebRtcManager`), `NetworkControlManager`, `WifiManager`, `VehicleNotificationManager`, `LogManager`, `DiagnosticLogManager`, `VehicleStatusEventManager`, `ElectronicTollCollectionManager`, `PowerManager`, `PresetModeManager`, `ProfileManager`, `OsPropertyManager`, vehicle property ID 클래스, `ReservationManager`, `CertificateManager`, `CertificateAuthorityManager`, `TrustAgentManager`, Telephony(`TelephonyManager`, `ModemManager`, `SimManager`, `VoiceCallManager`, `MessagingManager`, `DataConnectionManager`, `CellularNetworkManager`, `EngineeringModeManager` 등), `TripManager`, `SystemDialogManager`, `OtaManager`.
- 자주 등장하는 권한 예: `MANAGE_CLOUD_PROVISIONING`, `MANAGE_TELEPHONY_DATA`, `CONTROL_AUDIO_EFFECTS`, `CAR_LOCATION`, `LOG`, `CONTROL_CLOUD_MQTT`, `MANAGE_CERTIFICATE_AUTHORITY`, `PROFILE_QUERY`, `READ_OTA`, `MANAGE_APP_CERTIFICATES`, `CHANGE_WIFI_STATE` 등 (모두 `ai.pleos.permission.` 접두사). 부여 조건은 대부분 `signature|privileged`, 일부 `signature`.
- 릴리스별 권한 변화: alpha.2 추가 — `CONTROL_SYSTEM_DIALOGS`, `MANAGE_CURRENT_USER_TRIP`, `PNP_MONITOR`, `PUBLISH_VEHICLE_NOTIFICATION_EVENTS`, `READ_TRIP_INFORMATION`, `READ_VEHICLE_NOTIFICATIONS`; alpha.3 추가 — `CONTROL_VEHICLE_PRESET_MODE`, `READ_VEHICLE_PRESET_MODE`, `REPORT_VEHICLE_DIAGNOSTICS`, `android.car.permission.CAR_VENDOR_EXTENSION`, `android.car.permission.CONTROL_CAR_CLIMATE`. 1.0.0-20261023에서 새로 공개된 vehicle property ID에는 `Car.PERMISSION_VENDOR_EXTENSION` 요구사항 선언.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/permission-matrix , https://document.pleos.ai/api-reference/pleos-platform-sdk/release-notes

### 1.10 정책 (계약·호환성 / 릴리스·버전)
- **버전 체계**: 단일 Maven artifact `ai.pleos:pleos-manager-stubs-system:<version>`, SemVer `MAJOR.MINOR.PATCH`. MAJOR=API 제거·변경/비호환 permission·동작/최소 플랫폼 상향, MINOR=호환 추가, PATCH=signature 유지 수정. Pleos Connect·Android API Level은 버전에 포함하지 않고 artifact 호환 정보(`pleos.connect.targetVersion=3.5`, `android.api.level=34`)로 관리. 게시된 버전은 재게시 불가.
- **릴리스 단계**: `1.0.0-dev.<build>`(내부, 호환성 약속 없음) → `alpha.N`(비정기) → `beta.N`(2주 주기, 최소 2회·4주 이상) → `rc.N`(필요 시 최대 주 1회, 공개 API 동결) → Stable. 최초 정식은 Pleos Connect 3.5와 함께 `1.0.0`.
- **호환성 보장**: Source / Binary / Behavior / Feature / Permission compatibility. Breaking change는 MAJOR + 사유·영향 + migration guide + 대체 API/지원 종료 일정.
- **PCTS (Pleos Compatibility Test Suite)**: 플랫폼 구현이 공개 SDK 계약을 준수하는지 검증 (API surface, binary, permission, feature, return/error, lifecycle, behavior, security/privacy). 앱 검증을 대체하지 않음.
- **Rate limit**: Platform SDK 문서에 별도 rate limit 명시 없음. 다만 일시적 unavailable 상태에서 무제한 반복 호출을 피하고 listener를 polling보다 우선 사용하도록 규정.
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/policies/pleos-platform-sdk-policy , https://document.pleos.ai/api-reference/pleos-platform-sdk/policies/pleos-platform-sdk-release-policy

### 1.11 릴리스 노트
| 버전 | 주요 내용 |
| --- | --- |
| **1.0.0-20261023** (2026-10-23, 최신) | **Breaking**: Task panel API 이름 변경 (`TaskPanelLocation`→`TaskPanel`, `TaskPanelLocationListener`→`TaskPanelListener`, `onTaskPanelLocationChanged()`→`onTaskPanelChanged()`, `get/setTaskPanelLocation()`→`get/setTaskPanel()` 등), `TaskPanel.APP_PASSENGER` 추가. NDAS API 스텁 추가(미구현). `PleosVehiclePropertyIds`에 통행료 단말·프리셋 모드·진단·보정·전원·예약 속성 ID 공개. CloudHub HTTP 버그 수정. Added 74 / Removed 15 declarations. Migration 필요 |
| 1.0.0-alpha.3 | Optional feature catalog, Android user scope별 domain 전환, locked-boot trust 접근, CloudHub StateSync update state payload, preset mode/ETCS/power의 vehicle property 이전, diagnostic report API 등. Added 2610 / Removed 237. Migration 필요(removed-public-api) |
| 1.0.0-alpha.2 | `LogManager` diagnostic logging, Trip domain, PnP domain, vehicle notification 등. Added 183 / Removed 0. Migration 불필요 |
- Source: https://document.pleos.ai/api-reference/pleos-platform-sdk/release-notes , https://document.pleos.ai/api-reference/pleos-platform-sdk/release-notes/1.0.0-20261023/migration-guide

---

## 2. Vehicle Data API

### 2.1 개요 · 대상
- 현대/기아/제네시스 커넥티드 카에서 생성되는 **텔레매틱스 데이터를 조회하는 오픈 REST API**. 데이터 공유에 동의한 범위 내에서 제공 (현대 블루링크, 기아 커넥트, 제네시스 커넥티드 서비스를 통해 적법하게 수집된 데이터).
- **서버/클라우드용** API (차량 내 앱 SDK가 아님). 사용자 유형: **Private Users**(개인 차량 소유자) / **Business Users**(사업자).
- **사용 가능 지역: 현재(2026.8월) 유럽만.** Private Users용 내 차량 API 키 발급도 유럽 지역만. 이용 국가(FAQ): 그리스, 네덜란드, 노르웨이, 덴마크, 독일, 루마니아, 몰도바, 몰타, 벨기에, 보스니아 헤르체고비나, 불가리아, 세르비아, 스웨덴, 스위스, 스페인, 슬로바키아, 슬로베니아, 아이슬란드, 아일랜드, 에스토니아, 영국, 오스트리아, 우크라이나, 이탈리아, 조지아, 체코, 카나리아 제도, 크로아티아, 키프로스, 포르투갈, 폴란드, 프랑스, 핀란드, 헝가리.
- 제공 데이터
  - 차량 상태 정보(충전, 파워트레인, 위치, 운행, 센서, 공조 등): Business는 동의 직후, Private는 API 키 획득 후 바로.
  - 차량 기초 데이터(파워트레인·속도·운행·센서 기초): Business는 **동의 후 24시간 경과 후**부터, Private는 바로.
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/intro , https://document.pleos.ai/api-reference/vehicle-data-api/faq

### 2.2 Base URL · 공통 헤더
- Base URL: `https://api.pleos.ai`
- 헤더: `Authorization: Bearer <ACCESS_TOKEN>` (데이터 API 필수), `Content-Type: application/json` (Optional), `Brand: hyundai|kia|genesis` (**필수**, 인증 API 포함).
- Private Users는 `Brand`, `Authorization` 토큰의 제조사, `vin`의 제조사가 일치해야 함.
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/call-api , https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-private-users/call-api

### 2.3 Onboarding
**Business Users**
1. Pleos Playground(https://developers.pleos.ai) 회원 가입
2. **My Project**에서 프로젝트 생성
3. 프로젝트 → **API > Vehicle Data** → 신청하기. 입력 항목 중 인증에 필요한 값:
   - 사용자 인증 정보 > OAuth 로그인 > **로그인 리다이렉트 URL**
   - 개인정보 제3자동의 > **동의완료 리다이렉트 URL**
   - 개인정보 제3자동의 > **동의철회 콜백 URL**
4. 심사 후 승인/반려 (승인 시 이메일). **API 신청 내역**에서 상태 확인
5. 심사 문의: 현대·제네시스 — Hyundai Connected Mobility GmbH, 기아 — Kia Connect GmbH
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/api-access-request

**Private Users**
1. Pleos Playground 회원 가입 → **Workspace > My Vehicle Data API** (유럽 사용자에게만 표시)
2. 제조사 탭 선택 (키는 **제조사별 1개**), 이용약관·개인정보 처리방침 동의 → **API 키 발급** → 제조사 계정으로 추가 로그인 → Client ID / Client Secret 발급
3. My Project 생성이나 사업자 승인 불필요
- API 키 상태: 미발급 / 발급됨 / 폐기됨 / 만료됨. 재발급 시 기존 키로 발급된 토큰 무효화, 폐기 시 모든 토큰 무효화(재발급 불가), 만료 시 토큰 무효화.
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-private-users/request-my-vehicle-api-key , https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-private-users/manage-my-vehicle-api-key

### 2.4 인증 (OAuth 2.0 기반 토큰)

**Business Users — Authorization Code 흐름**
1. Playground **My Project > API 탭**에서 Client ID / Client Secret 확인 (외부 노출 금지)
2. **Login API** 호출로 차량 소유자에게 제조사 로그인 창 표시 (`redirectUrl`은 신청 시 입력한 로그인 리다이렉트 URL과 동일해야 함; 다르면 'Invalid Request')
3. 로그인 성공 시 `redirectUrl`로 authorization `code` 반환
4. 토큰 발급

| 용도 | Endpoint | Body |
| --- | --- | --- |
| Create Access Token | `POST https://api.pleos.ai/v1/auth/token` | `clientId`, `clientSecret`, `code`, `redirectUrl` |
| Refresh Access Token | `POST https://api.pleos.ai/v1/auth/token-refresh` | `clientId`, `clientSecret`, `refreshToken` |

- 응답: `data.accessToken`, `data.refreshToken` + `meta`.
- **Access token 유효 1시간, Refresh token 유효 90일·1회용** (갱신 시마다 새 refresh token 발급).
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/authorization , https://document.pleos.ai/api-reference/vehicle-data-api/authentication-authorization/for-business-users/get-access-token , https://document.pleos.ai/api-reference/vehicle-data-api/authentication-authorization/for-business-users/get-access-token-refresh

**Private Users — API 키(Client ID/Secret) 기반**
| 용도 | Endpoint | Body |
| --- | --- | --- |
| Create Access Token | `POST https://api.pleos.ai/v1/auth/personal-token` | `clientId`, `clientSecret` |
- 응답: `data.accessToken` (refresh token 없음). 만료되면 동일 API로 재발급.
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-private-users/authorization , https://document.pleos.ai/api-reference/vehicle-data-api/authentication-authorization/for-private-users/get-access-token

**Scopes**: 문서에 OAuth scope 파라미터 명시 없음. 접근 범위는 Playground API 신청 시 선택한 Vehicle Data API와 차량 소유자의 데이터 공유 동의로 결정됨.

### 2.5 데이터 공유 동의 (Business Users)
1. `GET https://api.pleos.ai/v1/vehicles/selections` (Select Vehicles) → 차량 선택·데이터 공유 동의 웹뷰 URL 반환 (`state`, `lang` 파라미터 예시)
2. 소유자 동의/거절 후 **동의완료 리다이렉트 URL** 호출. 동의 시 `vehicles` 파라미터(VIN 배열, **Base64 인코딩**) 전달
3. `GET https://api.pleos.ai/v1/vehicles/consent` (Get Vehicles)로 동의 차량 목록 조회
- 개발사는 동의 철회 기능을 반드시 구현해야 함 (**Stop Data Sharing** API 사용).
- **동의 철회 알림(Webhook 성격)**: 소유자가 계약 해지·계정 탈퇴·커넥티드 서비스 해지·앱(My Hyundai / Kia App / MY GENESIS)에서 철회 시 **동의철회 콜백 URL**로 `POST` — body `{"userIds": ["UUID"], "vins": ["vin1", ...]}`. 수신 시 해당 차량 데이터를 **즉시 일괄 삭제**하고 노출 중단해야 함.
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/request-for-data-sharing , https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/notify-data-sharing-consent-withdraw

### 2.6 엔드포인트 개요
| API | Endpoint | 설명 | 분류 |
| --- | --- | --- | --- |
| Get Battery Charging Status | `GET /v1/vehicles/{vin}/batteries` | 플러그 연결, 충전/방전, 잔량, 남은 충전 시간 | 상태 |
| Get Powertrain Status | `GET /v1/vehicles/{vin}/powertrains` | 엔진·모터·배터리 상태 | 상태 |
| Get Location Info | `GET /v1/vehicles/{vin}/locations` | 위도·경도, 고도, 방향, 속도 | 상태 |
| Get Vehicle Operation Status | `GET /v1/vehicles/{vin}/driving` | 시동, 누적 주행거리, 주차 상태 | 상태 |
| Get Vehicle Status | `GET /v1/vehicles/{vin}/status` | 도어·트렁크, 타이어 공기압, 경고등, 공조 | 상태 |
| Get Freezer Status | `GET /v1/vehicles/{vin}/freezer` | 냉동고 작동 상태·내부 온도 | 상태 |
| Get Powertrain Data | `GET /v1/vehicles/{vin}/powertrains/data` | 배터리 팩 전압, 엔진 RPM·오일 온도·누적 운행 시간 | 기초 |
| Get Speed Data | `GET /v1/vehicles/{vin}/speed` | 속도 관련 기초 데이터 | 기초 |
| Get Vehicle Operation Data | `GET /v1/vehicles/{vin}/driving/data` | 운행 관련 기초 데이터 | 기초 |
| Get Vehicle Data | `GET /v1/vehicles/{vin}/status/data` | 센서 기반 기초 데이터 | 기초 |
| Select Vehicles (Business) | `GET /v1/vehicles/selections` | 동의 웹뷰 URL 조회 | 동의 |
| Get Vehicles | `GET /v1/vehicles/consent` | 동의(또는 내) 차량 목록 | 동의 |
- 개별 API는 호출 순서 없이 단독 호출 가능(토큰 선발급 필요). 데이터 필드는 차종에 따라 다르며 미수집 데이터는 `null` (API compatibility 페이지 참조).
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/intro , https://document.pleos.ai/api-reference/vehicle-data-api/api-compatibility

### 2.7 응답 · 에러
- 성공: `{ "data": {...}, "meta": { code, message, success, timestamp, requestId } }`
- 실패: `{ "error": { code, message, details[], timestamp, requestId } }`

| HTTP | Code | 의미 |
| --- | --- | --- |
| 400 | 401 / 403 / 404 | Unauthorized / Forbidden / NotFound |
| 400 | 4002 | Invalid Request Parameters |
| 400 | 4009 | Invalid or Expired Authorization Code or Refresh Token |
| 400 | 4011 | Invalid Authorization Header |
| 400 | 40002 | No Auth Header |
| 400 | 5006 | No Permission (공유 미동의/철회 차량) |
| 404 | 4045 | No Data (기초 데이터는 동의 후 24시간 경과 필요, 커넥티드 서비스 미사용·수집 미동의·ePrivacy 미허용 시) |
| 410 | 4101 | Unregistered Vehicle |
| 500 | 5001 | Internal Server Error |

Private Users 인증 에러: `40001` 잘못된 파라미터, `40002` Authorization 헤더 없음, `40003` 토큰 만료, `40004` 유효하지 않은 토큰, `40404` 사용자/Key 없음, `50001` 내부 오류, `50201` 외부 연동 오류.
- 문의 시 호출 URL, 에러 상황, `requestId` 전달.
- **Rate limit**: 문서에 명시 없음.
- Source: https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-business-users/error-code , https://document.pleos.ai/api-reference/vehicle-data-api/getting-started/for-private-users/error-code , https://document.pleos.ai/api-reference/vehicle-data-api/faq

---

## 3. Fleet API

### 3.1 개요 · 대상
- 차량 및 Fleet을 관리하는 API로 **Pleos Fleet 기반 애플리케이션**(서버/클라우드) 구축용. 제공 형태: **REST API**, **Webhook**.
- Component: Vehicle(차량), Device(단말), Vehicle Device Matching(장/탈착), Device Model / 지원 예정: Abnormal event, Reservation, Driver, Maintenance, Trip.
- 주요 기능: 차량/단말 등록·수정·삭제, 등록 차량 데이터 조회, Webhook 실시간 차량 데이터 수신.
- Source: https://document.pleos.ai/api-reference/fleet-api/intro

### 3.1a Fleet REST API
- 차량·단말 기본 정보, 실시간 데이터, 차량 제어, 운전자, 정비, 이상 상태 등을 원하는 플랫폼에 연동하도록 돕는 REST API. 사전 준비는 Getting started의 "REST API 시작하기" 섹션을 참조하라고 안내함 (해당 페이지 본문은 수집되지 않음).
- OpenAPI 스펙 버전: `0.0.1` (License 표기: Apache 2.0).
- **Base URL**: 문서에 명시 없음 (각 엔드포인트는 `/api/...` 상대 경로로만 표기).
- **인증**: Security Scheme `apiKey` — 헤더 `x-42dot-client-id`. **별도 토큰 발급 엔드포인트(OAuth)는 문서에 없음.** (Client ID/Secret/Project ID는 Pleos Playground에서 발급 — Webhook Pre-requisite 기준)
- **공통 헤더**: 문서상 확인되는 것은 `x-42dot-client-id`뿐. 그 외 공통 헤더 명시 없음.
- 개념
  - Vehicle: 차량 등록·관리. 관제 단말(Telematics) 장착 시 실시간 데이터 조회 가능. 관제 단말이 여러 대면 **주 관제 단말(Main telematics device)** 데이터가 차량 데이터로 간주됨 (주 단말은 변경 가능).
  - Device: 관제 단말(Telematics)과 비관제 단말(Dashcam 등)로 분류. 차량 1대에 n개 단말 장착 가능.
  - Vehicle Device Matching: 차량에 단말 장착/탈착.
  - Device model: 단말 등록 시 필요. 지원 모델 — `Hyundai`, `Kia`, `Genesis` (DeviceSourceId = 차량의 VIN). 단말 없이 차량이 직접 데이터를 생산하는 경우 차량 자체를 단말로 등록.

| Method | Path | 용도 | 주요 파라미터 / 비고 |
| --- | --- | --- | --- |
| `GET` | `/api/vehicles` | 차량 정보 검색 | search target과 search value로 검색 |
| `GET` | `/api/vehicles/:vehicleId` | 단일 차량 조회 (retrieveVehicle) | `vehicleId` (path) |
| `POST` | `/api/vehicles` | 차량 등록 | Body 상세 명시 없음 |
| `POST` | `/api/vehicles/devices` | 차량 등록 + 단말 등록(Optional) | 단말을 함께 등록하면 해당 차량에 연결되고 단말 정보 반환 |
| `PATCH` | `/api/vehicles/:vehicleId` | 차량 정보 수정 | `vehicleId` (path) |
| `DELETE` | `/api/vehicles/:vehicleId` | 차량 삭제 | 단말과 매칭된 차량은 삭제 불가 |
| `GET` | `/api/devices` | 단말 정보 검색 | Device ID, Device Source ID, Device Name으로 검색 |
| `POST` | `/api/devices` | 단말 등록 | Device Source ID 형식은 Device Model에 따라 다름 → 먼저 Device Model 조회 |
| `PATCH` | `/api/devices/:deviceId` | 단말 정보 수정 | `deviceId` (path) |
| `DELETE` | `/api/devices/:deviceId` | 단말 삭제 | 차량과 매칭된 단말은 삭제 불가 |
| `GET` | `/api/device-models` | 단말 모델 조회 | 단말 등록에 필요한 `DeviceModelId` 획득 |
| `POST` | `/api/vehicles/:vehicleId/devices/:deviceId` | 차량-단말 매칭(장착) | `vehicleId`, `deviceId` (path) |
| `DELETE` | `/api/vehicles/:vehicleId/devices/:deviceId` | 차량-단말 매칭 해제(탈착) | `vehicleId`, `deviceId` (path) |

- Request body/query 스키마 상세는 다운로드본에 렌더링되지 않아 문서에서 확인 불가.
- **에러/응답 코드**: 모든 엔드포인트 공통으로 `200 OK`, `400 Bad Request`, `500 Internal Server Error`만 명시. 세부 에러 코드 체계는 명시 없음.
- Source: https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/rest-api-overview , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/fleet-api , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/retrieve-vehicles , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/retrieve-vehicle , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/register-vehicle , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/register-vehicle-with-device , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/modify-vehicle , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/remove-vehicle , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/retrieve-devices , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/register-device , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/modify-device , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/remove-device , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/retrieve-device-models , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/match-the-vehicle-and-device , https://document.pleos.ai/api-reference/fleet-api/fleet-rest-api/unmatch-the-vehicle-and-device

### 3.2 Onboarding (Webhook)
1. **Pleos Playground**에서 Client ID, Client Secret, Project ID 발급
2. **Pleos Fleet**에서 Webhook 등록 (Webhook 관리 페이지에서 생성/수정, Custom Header Key-Value 설정 가능 — 예: `Authorization: Bearer your_token_here`, `X-Custom-Header`)
- 동작: 구독 등록 → 이벤트 발생(문열림·충격 감지, 또는 FMS 이벤트: 트립 생성·차량 정보 수정 등) → 등록 endpoint로 HTTP 요청 → 유효 응답(예: `200 OK`)이면 성공, 아니거나 타임아웃이면 **최대 재시도 횟수만큼 재시도** (구체 횟수·타임아웃 명시 없음).
- Source: https://document.pleos.ai/api-reference/fleet-api/fleet-api-webhook/webhook-overview

### 3.3 Webhook 이벤트 타입
| Event Type | 설명 |
| --- | --- |
| 주기 데이터 (Periodic data) | 차량에서 주기적으로 발생하는 데이터 스냅샷 (속도, 위치 등) |
| 차량 이벤트 데이터 (Vehicle event) | 문열림·충격 감지 등 이벤트 발생 시 정보 |

**공통 요청**: HTTPS(필수), `POST`. 헤더 `Content-Type: application/json; charset=utf-8`, `X-API-Version: v1.0`, (Periodic만) `X-42dot-transmissionType`(수집/전송 주기, 예 `10S_30S`), `X-Custom-Key`(등록한 Custom Header, Optional).

**Payload 공통**: `vin`, `vehicleId`, `itemsCount`, `items[]` (`messageTime` ISO 8601, `data` 객체). 값은 `ValueObject<T>` = `{ "value": T, "eventTime": "ISO 8601" }`. `items[]` 내부 데이터는 수신된 경우에만 포함(Optional).

**Periodic data 필드** (수집/전송 주기 10s / 30s): `Vehicle.Latitude`, `Vehicle.Longitude`, `Vehicle.Speed`(km/h), `Vehicle.Odometer`(km), `AuxiliaryBattery.Voltage`, `Vehicle.Tire.Pressure.{FrontLeft,FrontRight,RearLeft,RearRight}`, `ChargeSystem.Active`(`ACTIVE`/`INACTIVE`), `TractionBattery.SOC`(%), `Vehicle.DTE`(km), `TractionBattery.RegenerativeEnergy`(kWh).

**Vehicle event 필드**: `Vehicle.Ignition1/2/3`, `Vehicle.IgnitionAcc`, `Vehicle.DrivingReady`(OFF/ON), `Door.Open.Driver`, `Door.Lock.Driver`, `Hood.Open`, `Trunk.Open`, `ChargePort.Plug`(PLUG/UNPLUG), `Emergency.Crash`(CRASH/UNCRASH), `Seat.Buckle.Driver/Passenger`, `ParkingBrake.Engage`, `WarningLamp.*` (Airbag, EBD, ABS, EPS, ChargingSystem, TirePressure.*, HeadLamp, EPB, ESC, RegenerativeBrake, TractionBatterySOC, EVPowerDown, EVService, WasherFluid, BrakeOil, AuxiliaryBatterySOC).
- Source: https://document.pleos.ai/api-reference/fleet-api/fleet-api-webhook/ , https://document.pleos.ai/api-reference/fleet-api/fleet-api-webhook/webhook-event-type/periodic-data , https://document.pleos.ai/api-reference/fleet-api/fleet-api-webhook/webhook-event-type/vehicle-event

### 3.4 Callback response 규약 (수신 endpoint가 반환해야 하는 형식)
| 코드 | 용도 |
| --- | --- |
| `200 OK` | 즉시 처리 완료 (`{"status":"completed","message":...}`) |
| `202 Accepted` | 비동기 처리 접수 (`status: accepted`, `internal_code` 예 3000) |
| `204 No Content` | 성공, 본문 없음 |
| `400` | Syntax Error / Missing Required Fields(`ERR_MISSING_FIELD`) / Validation Failure(`ERR_INVALID_VALUE`) — `error`, `message`, `errors[{field,message,code}]` |
| `500` | Internal Server Error |
| `503` | Service Unavailable — `retry_after`(초) 포함 |
| `502` / `504` | Bad Gateway / Gateway Timeout |
- Source: https://document.pleos.ai/api-reference/fleet-api/fleet-api-webhook/webhook-callback-response

- **Rate limit / 버전**: Rate limit 명시 없음. Webhook 헤더 기준 Fleet API 버전 `v1.0`.

---

## 4. SDVOS API

- **SDVOS 문서는 공개적으로 접근할 수 없음**: 모든 SDVOS 페이지가 `/404`로 리다이렉트됨(접근 제한). 따라서 개요, 대상, 인증, 의존성, 버전 등은 **공개 문서에서 확인 불가**.
- 확인된 페이지 구조:
  - Guide: architecture, programming-model, app-framework(application-lifecycle, anr-watchdog), fault-tolerance(container-orchestration, sw-redundancy), integration/connect, persistent-store, power-management, security/access-control, time-synchronization, tools(console, recorder)
  - Tutorial: setting-up-environment(installation, creating-workspace), app / service / io-agent (각각 creating-project, writing-code, building-and-running)
  - Sample project: vehicle-api-hello-world, vehicle-api-hello-world-cpp, vehicle-api-hae-release-cpp (C++ 샘플 존재가 이름으로만 추정됨)
- Source: https://document.pleos.ai/api-reference/sdvos/tutorial/ , https://document.pleos.ai/api-reference/sdvos/guide/architecture/ 등 (모두 404)

---

## 5. 문서 공백 (확인 필요)
- Fleet API: Base URL, Request body 스키마, 세부 에러 코드, Webhook 재시도 횟수/타임아웃 — 문서에 명시 없음 (Getting started 페이지 본문 미수집). 인증은 `x-42dot-client-id` API Key 헤더만 확인됨.
- Vehicle Data API: OAuth scope, rate limit, Login/Select Vehicles/Stop Data Sharing 상세 파라미터 (개별 API 레퍼런스 페이지 미수집), API compatibility 표 본문(다운로드본에 헤더만 존재).
- Platform SDK: Maven 계정은 회사별 담당자 통해 발급, rate limit 개념 없음.
- SDVOS: 전체 페이지가 `/404`로 리다이렉트(접근 제한, 비공개).
