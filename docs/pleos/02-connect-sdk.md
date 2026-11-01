# Pleos Connect SDK 레퍼런스 (Vehicle · NaviHelper · SpeechToText · TextToSpeech · FusedLocation · LLM)

> 출처: Hyundai Pleos 공식 개발자 문서 `https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/...`
> 이 문서는 다운로드된 마크다운 92개 파일만을 근거로 작성했습니다. 문서에 없는 내용은 추측하지 않았으며, 확인되지 않은 부분은 "문서에 없음"으로 표시했습니다.

---

## 0. 자료 범위 및 주의 사항

### 0.1 수집된 페이지 상태

| 구분 | 상태 |
| --- | --- |
| 가이드 페이지 (intro, importing, permissions, init/release, listeners, error code 등) | 정상 수집 — 본 문서의 근거 |
| 각 SDK `entry` 페이지 (`/Vehicle/entry`, `/NaviHelper/entry` 등) | 제목(`# Vehicle` 등)만 존재, 본문 없음 |
| **클래스 단위 API Reference (Kotlin/Dokka) 페이지 전부** | **모두 "Page Not Found"(404)** |

404로 확인된 API Reference 페이지 목록:

- Vehicle: `ai.pleos.playground.vehicle.api` 의 `brake`, `car-info`, `display`, `door`, `driving-mode`, `ev-battery`, `fuel`, `h-v-a-c`, `light`, `odometer`, `safety`, `seat`, `side-mirror`, `steering-wheel`, `tire`, `toll`, `turn-signal`, `window`, `wiper` / `ai.pleos.playground.vehicle.callback.exception.vehicle-exception`
- NaviHelper: `ai.pleos.playground.navi.helper.navi-helper`, `ai.pleos.playground.navi.helper.listener.navi-helper-event-listener`
- SpeechToText: `ai.pleos.playground.stt.speech-to-text`, `stt.listener.on-initialize-listener`, `stt.listener.on-server-connection-listener`, `stt.listener.result-listener`
- TextToSpeech: `ai.pleos.playground.tts.text-to-speech`, `tts.listener.event-listener`, `tts.listener.on-initialize-listener`, `tts.listener.on-server-connection-listener`
- FusedLocation: `ai.pleos.playground.location.fused.fused-location`, `location.fused.listener.fused-location-listener`, `location.fused.listener.proximity-alert-listener`
- LLM: `ai.pleos.playground.llm.l-l-m`, `llm.data.l-l-m-content`, `llm.data.part`, `llm.data.text-part`, `llm.data.model-parameter`, `llm.listener.result-listener`, `llm.listener.on-server-connection-listener`

**결과적으로**: 메서드 시그니처·전체 메서드 목록은 가이드 페이지에 등장한 것만 기재합니다. 특히 Vehicle 도메인 클래스별 상세 메서드(HVAC 온도 set 등)는 수집 자료에 없으며, Door/Display 예제에 나온 메서드만 확인됩니다. LLM SDK는 가이드 페이지 자체가 없어 패키지/클래스 이름 외에는 알 수 없습니다.

### 0.2 SDK 버전 / 의존성 요약

| SDK | Gradle 좌표 | 문서 기준 버전 | 서비스 측 |
| --- | --- | --- | --- |
| Vehicle | `ai.pleos.playground:Vehicle:2.3.2` | SDK v2.3.2 | Vehicle Service v2.3.21 |
| NaviHelper | `ai.pleos.playground:NaviHelper:3.0.5` | SDK v3.0.5 | Navi Service (Pleos Connect Navigation 앱) |
| SpeechToText | `ai.pleos.playground:SpeechToText:3.3.1` | SDK v3.3.1 | Speech Service |
| TextToSpeech | `ai.pleos.playground:TextToSpeech:3.3.1` | SDK v3.3.1 | Speech Service |
| FusedLocation | `ai.pleos.playground:FusedLocation:1.1.3` | SDK v1.1.3 | FusedLocation Service |
| LLM | 문서에 없음 | 문서에 없음 | 문서에 없음 |

공통 Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/`

### 0.3 퍼미션 요약 (매니페스트 `android:name` 정확한 문자열)

| SDK | 퍼미션 |
| --- | --- |
| Vehicle | `pleos.car.permission.CAR_DOORS`, `CAR_WINDOWS`, `CAR_CLIMATE`, `CAR_DRIVE`, `CAR_ENERGY`, `CAR_INFO`, `CAR_LIGHTS`, `CAR_MIRRORS`, `CAR_SAFETY`, `CAR_SEATS`, `CAR_TOLL` (모두 `pleos.car.permission.` 접두사) + 제어용 `CONTROL_CAR_*` (예: `CONTROL_CAR_DOORS`) |
| NaviHelper | `pleos.car.permission.NAVI_ROUTE`, `pleos.car.permission.NAVI_ROUTE_SEARCH`, `pleos.car.permission.NAVI_CUSTOM_MAP` |
| SpeechToText | `pleos.car.permission.STT_SERVICE` |
| TextToSpeech | `pleos.car.permission.TTS_SERVICE` |
| FusedLocation | `pleos.car.permission.FUSED_LOCATION` (보호 수준 `dangerous`) |
| LLM | 문서에 없음 |

---

## 1. 공통 패턴 (Cross-cutting)

### 1.1 프로젝트 설정
- 모든 SDK는 `settings.gradle(.kts)`의 `dependencyResolutionManagement.repositories`에 Pleos Nexus 저장소를 추가하고, 앱 모듈에 `implementation("ai.pleos.playground:<SDK>:<version>")`을 선언합니다.
- STT/TTS 문서 예제는 `google()`, `mavenCentral()`도 함께 선언합니다.

### 1.2 수명주기: 생성 → initialize → 리스너 등록 → 사용 → 리스너 해제 → release
- 모든 SDK가 `XXX(context)` 생성자 → `initialize()` → ... → `release()` 패턴을 따릅니다.
- `release()` 이후 재사용하려면 **새 객체를 생성하고 다시 초기화**해야 합니다 (NaviHelper, STT, TTS, FusedLocation 문서에 명시).
- 리스너는 등록 시 사용한 **동일 인스턴스**로 해제해야 하며, `release()` 전에 해제하는 것을 권장합니다.
- 초기화 결과 콜백 형태는 SDK마다 다릅니다:
  - Vehicle: `initialize()` (예제에는 콜백 없음)
  - NaviHelper: `initialize(onConnected, onFailed)` 람다 또는 `initialize()`
  - STT/TTS: `initialize(OnInitializeListener)` (`onSuccess()` / `onFailure(ErrorType)`) 또는 `initialize()`
  - FusedLocation: `isSupported()` 확인 후 `initialize()`

### 1.3 비동기 결과 전달
- Vehicle: IPC 기반 → get/set/capability 결과는 `onComplete` / `onFailed` 람다로 비동기 전달. **비동기 콜백 종료 전 `release()` 호출 금지.**
- NaviHelper: 요청 API(`requestRoute()` 등) 및 정보 조회 API(`get*Info()`)는 **값을 직접 반환하지 않으며**, 결과는 `NaviHelperEventListener` 콜백으로 전달.
- STT/TTS: 결과·진행 상태는 `ResultListener` / `EventListener`로 전달.
- FusedLocation: `FusedLocationListener.onChanged(LocationData?)` / `onError(Exception)`.

### 1.4 오류 모델 비교

| SDK | 오류 타입 | 전달 경로 |
| --- | --- | --- |
| Vehicle | `Exception` (일부는 `VehicleException`, `errorCode: ErrorCode`) | `onFailed(e: Exception)` |
| NaviHelper | 초기화: `Throwable` / 내비 오류: `NaviErrorCode` | `initialize(onFailed)` / `NaviHelperEventListener.onError(errorCode)` |
| STT / TTS | `ErrorType` enum | `OnInitializeListener.onFailure`, `OnServerConnectionListener.onFailed`, `ResultListener.onError` / `EventListener.onError` |
| FusedLocation | `Exception` | `FusedLocationListener.onError(Exception)` |

### 1.5 퍼미션 확인 방식
- 각 API의 정확한 요구 퍼미션은 API Reference의 `@RequiresPermission` 어노테이션에서 확인하도록 안내합니다 (Vehicle, NaviHelper, FusedLocation 공통).
- 퍼미션 누락 시: Vehicle → `ERROR_SECURITY_PERMISSION`, STT/TTS → `PERMISSION_DENIED`.

### 1.6 Capability / Area 모델 (Vehicle 전용)
- `checkXxxCapabilities`로 지원 여부(`AccessType`)와 영역별 허용 범위(`Map<Area, AreaBounds>`)를 사전 확인 → 미지원 호출 감소.
- 영역은 `DoorArea(DoorValue.ROW_1_LEFT)`처럼 Area 객체로 지정합니다.

### 1.7 개발 환경 (모든 SDK 공통 Emulator setup)
모든 SDK 인트로 페이지가 동일한 Pleos Connect Emulator 준비 절차를 안내합니다.
1. 소프트웨어 개발 환경 요구사항 확인
2. Pleos Connect Emulator 설치
3. AVD 생성 및 실행
4. Pleos Connect Virtual Console 설정
5. 마이크 접근 설정
6. Cold boot 설정

(링크: `/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator`, `/use-vehicle-emulator`, `/configure-pleos-connect-emulator`)

---

## 2. Vehicle SDK

### 2.1 목적
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro

Vehicle SDK는 차량 정보 조회 및 제어 API를 제공합니다. 3rd party 앱은 Vehicle SDK를 통해 Pleos Connect의 **Vehicle Service**에 연결하며, Vehicle Service는 Android Automotive OS 위에서 동작해 앱의 조회/제어 요청을 처리합니다.

- 문서 기준: **Vehicle SDK v2.3.2 / Vehicle Service v2.3.21**
- 진입점: `Vehicle` (패키지 `ai.pleos.playground.vehicle`)
- 도메인 인터페이스 패키지: `ai.pleos.playground.vehicle.api`
- 예외: `ai.pleos.playground.vehicle.callback.exception.VehicleException`
- 상수: `ai.pleos.playground.vehicle.constant` (`ErrorCode`, `PleosPermission`)
- Capability 모델: `ai.pleos.playground.vehicle.api.model.capability` (`CapabilityConfig`, `AccessType`, `AreaBounds`)
- Area 모델: `ai.pleos.playground.vehicle.api.model.area` (`DoorArea` 등)
- 샘플 앱 가이드: `/docs/pleos-only/tutorial/vehicle-sdk-sample-app`

### 2.2 Gradle 설정
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup

`settings.gradle`:
```kotlin
dependencyResolutionManagement {
    ...
    repositories {
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
        ...
    }
}
```
```groovy
dependencyResolutionManagement {
    ...
    repositories {
        ...
        maven {
            url "https://nexus-playground.pleos.ai/repository/maven-releases/"
        }
        ...
    }
}
```

앱 모듈 `build.gradle`:
```kotlin
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
}
```
```groovy
dependencies {
    implementation "ai.pleos.playground:Vehicle:2.3.2"
}
```

### 2.3 퍼미션
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission

`PleosPermission` 상수(`ai.pleos.playground.vehicle.constant`)는 접두사로 구분되며, 매니페스트에는 `pleos.car.permission.{상수명}` 형식으로 선언합니다.

| 접두사 | 예시 | 용도 |
| --- | --- | --- |
| `CAR_*` | `CAR_DOORS` → `pleos.car.permission.CAR_DOORS` | 도메인 데이터 조회 (get) |
| `CONTROL_CAR_*` | `CONTROL_CAR_DOORS` | 도메인 제어 (set) |

도메인별 대표 조회 퍼미션 (`CAR_*`):

| 도메인 | PleosPermission 상수 | 매니페스트 `android:name` |
| --- | --- | --- |
| Door | `CAR_DOORS` | `pleos.car.permission.CAR_DOORS` |
| Window, Wiper | `CAR_WINDOWS` | `pleos.car.permission.CAR_WINDOWS` |
| HVAC | `CAR_CLIMATE` | `pleos.car.permission.CAR_CLIMATE` |
| DrivingMode, Brake, Odometer | `CAR_DRIVE` | `pleos.car.permission.CAR_DRIVE` |
| EvBattery, Fuel | `CAR_ENERGY` | `pleos.car.permission.CAR_ENERGY` |
| CarInfo, Display | `CAR_INFO` | `pleos.car.permission.CAR_INFO` |
| Light, TurnSignal | `CAR_LIGHTS` | `pleos.car.permission.CAR_LIGHTS` |
| SideMirror | `CAR_MIRRORS` | `pleos.car.permission.CAR_MIRRORS` |
| Safety, Tire | `CAR_SAFETY` | `pleos.car.permission.CAR_SAFETY` |
| Seat | `CAR_SEATS` | `pleos.car.permission.CAR_SEATS` |
| Toll | `CAR_TOLL` | `pleos.car.permission.CAR_TOLL` |

- 세부 API마다 요구 퍼미션이 다를 수 있으므로, 호출 전 해당 함수의 `@RequiresPermission`을 확인하라고 명시되어 있습니다.
- Steeringwheel 도메인의 대표 퍼미션은 위 표에 없습니다(문서에 없음).
- `CONTROL_CAR_*` 계열은 `CONTROL_CAR_DOORS` 예시 외의 전체 목록이 문서에 없습니다.
- 사용하는 도메인에 맞는 퍼미션만 선언하면 됩니다.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.CAR_DOORS" />
    <uses-permission android:name="pleos.car.permission.CAR_INFO" />
    <!-- 사용하는 도메인에 맞는 퍼미션만 선언 -->
</manifest>
```

퍼미션 누락 시: API `onFailed`에 `Exception`이 전달되며, `VehicleException`인 경우 `ERROR_SECURITY_PERMISSION`에 해당할 수 있습니다.

### 2.4 앱 등록 / API Key
Vehicle SDK 문서에는 앱 등록 또는 API Key(Client ID/Secret) 요구 사항이 없습니다. 매니페스트 퍼미션 선언만 안내됩니다.

### 2.5 초기화 및 해제 / 도메인 객체 획득
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object

- `Vehicle` 인스턴스 생성 → `initialize()`로 차량 서비스 연결 → `getXxx()`로 도메인 객체 획득 → Get/Set/Callback/Capability API 호출 → `release()`로 해제.
- 도메인 객체(`Door`, `Brake` 등)는 **직접 생성하지 않고** 반드시 `Vehicle.getXxx()`로만 얻습니다.
- 차량 통신은 IPC이므로 결과는 `onComplete`·`onFailed` 콜백으로 비동기 전달됩니다. **비동기 콜백이 끝나기 전에 `release()`를 호출하지 않도록** 주의해야 합니다.

```kotlin
// Vehicle 인스턴스 생성 후 초기화
val vehicle = Vehicle(context)
vehicle.initialize()

// getXxx()로 도메인 객체 획득
val door = vehicle.getDoor()

// door 객체에서 Get / Set / Callback / Capability API 호출
// ...

// 사용 종료 후 리소스 해제
vehicle.release()
```
```java
// Vehicle 인스턴스 생성 후 초기화
Vehicle vehicle = new Vehicle(context);
vehicle.initialize();

// getXxx()로 도메인 객체 획득
Door door = vehicle.getDoor();

// door 객체에서 Get / Set / Callback / Capability API 호출
// ...

// 사용 종료 후 리소스 해제
vehicle.release();
```

### 2.6 네 가지 API 타입
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object

| 타입 | 네이밍 | 하는 일 | 결과 전달 | 문서 |
| --- | --- | --- | --- | --- |
| Capability | `checkXxxCapabilities` | 기능 지원 여부·허용 범위 확인 | `onComplete(config, areaMap)` / `onFailed` | `/Vehicle/check-capability` |
| Get | `getCurrentXxx` | 현재 값 조회 | `onComplete(값)` / `onFailed` | `/Vehicle/get-property` |
| Set | `setXxx` | 값 설정 | `onFailed`(실패 시만) | `/Vehicle/set-property` |
| Callback | `registerXxx` / `unregisterXxx` | 상태 변화 구독 | 리스너 `onXxxUpdated` / `onFailed` | `/Vehicle/callback` |

#### 2.6.1 Capability 확인
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability

`checkXxxCapabilities`로 기능 지원 여부와 허용 범위를 확인합니다. 반환 형태는 API마다 다르며, 이 문서 트리는 `CapabilityConfig`를 함께 반환하는 capability API를 기준으로 설명합니다.

| 타입 | 설명 |
| --- | --- |
| `CapabilityConfig` | 기능 단위 접근 수준 (`AccessType`: `NONE`, `READ`, `WRITE`, `READ_WRITE`) |
| `AreaBounds` | 영역별 허용 값 범위 (`minValue`, `maxValue`) |
| `Map<Area, AreaBounds>` | 영역(예: `DoorArea`)별 `AreaBounds` 맵 |

```kotlin
val door = vehicle.getDoor()
door.checkDoorPositionCapabilities(
    onComplete = { config, areaMap ->
        when (config.accessType) {
            AccessType.READ, AccessType.READ_WRITE -> {
                val bounds = areaMap[DoorArea(DoorValue.ROW_1_LEFT)]
                // Use bounds?.minValue / bounds?.maxValue, then call getCurrentDoorPosition
            }
            else -> {
                // NONE or WRITE only — disable UI or use an alternate flow
            }
        }
    },
    onFailed = { e: Exception ->
        if (e is VehicleException) {
            // Capability lookup failed: e.errorCode
        }
    }
)
```
```java
Door door = vehicle.getDoor();
door.checkDoorPositionCapabilities(
    (config, areaMap) -> {
        if (config.getAccessType() == AccessType.READ
            || config.getAccessType() == AccessType.READ_WRITE) {
            AreaBounds<Integer> bounds = areaMap.get(new DoorArea(DoorValue.ROW_1_LEFT));
            // Use bounds.getMinValue() / bounds.getMaxValue(), then call getCurrentDoorPosition
        } else {
            // NONE or WRITE only — disable UI or use an alternate flow
        }
        return Unit.INSTANCE;
    },
    e -> {
        if (e instanceof VehicleException) {
            // Capability lookup failed: ((VehicleException) e).getErrorCode()
        }
        return Unit.INSTANCE;
    }
);
```

#### 2.6.2 Get
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/get-property

```kotlin
val door = vehicle.getDoor()
door.getCurrentDoorPosition(
    area = DoorArea(DoorValue.ROW_1_LEFT),
    onComplete = { pos: Int? ->
        // do something
    },
    onFailed = { e: Exception ->
        // do something
    }
)

val display = vehicle.getDisplay()
display.getCurrentTemperatureDisplayUnit(
    onComplete = { unit: TemperatureUnit? ->
        // do something
    },
    onFailed = { e: Exception ->
        // do something
    }
)
```
```java
Door door = vehicle.getDoor();
door.getCurrentDoorPosition(
    new DoorArea(DoorValue.ROW_1_LEFT),
    pos -> {
        // do something
        return Unit.INSTANCE;
    },
    e -> {
        // do something
        return Unit.INSTANCE;
    }
);

Display display = vehicle.getDisplay();
display.getCurrentTemperatureDisplayUnit(
    unit -> {
        // do something
        return Unit.INSTANCE;
    },
    e -> {
        // do something
        return Unit.INSTANCE;
    }
);
```
- 영역(area)이 있는 속성은 `area` 파라미터를 받고(Door), 영역이 없는 속성은 콜백만 받습니다(Display 온도 단위).
- 값은 nullable(`Int?`, `TemperatureUnit?`)로 전달됩니다.

#### 2.6.3 Set
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/set-property

```kotlin
val door = vehicle.getDoor()
door.setDoorPosition(
    area = DoorArea(DoorValue.ROW_1_LEFT),
    value = 1, // Integer type
    onFailed = { e: Exception ->
        // do something
    }
)
```
```java
Door door = vehicle.getDoor();
door.setDoorPosition(
    new DoorArea(DoorValue.ROW_1_LEFT),
    1,
    e -> {
        // do something
        return Unit.INSTANCE;
    }
);
```
- Set은 성공 콜백이 없으며 실패 시에만 `onFailed`가 호출됩니다 (API 타입 표: "Set — `onFailed`(실패 시)").

#### 2.6.4 Callback (구독)
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/callback

```kotlin
// Create callback listener
val callbackListener = object : DoorPositionListener {
    override fun onPositionUpdated(pos: Int?) {
        // do something
    }
    override fun onFailed(e: Exception) {
        // do something
    }
}

val door = vehicle.getDoor()

// register callback
door.registerDoorPosition(
    doorPositionListener = callbackListener
)

// unregister callback
door.unregisterDoorPosition(
    doorPositionListener = callbackListener
)
```
```java
// Create callback listener
DoorPositionListener callbackListener = new DoorPositionListener() {
    @Override
    public void onPositionUpdated(@Nullable int pos) {
        // do something
    }
    @Override
    public void onFailed(@NonNull Exception e) {
        // do something
    }
};

Door door = vehicle.getDoor();
// register callback
door.registerDoorPosition(callbackListener);
// unregister callback
door.unregisterDoorPosition(callbackListener);
```
- 리스너 인터페이스는 `XxxListener` (예: `DoorPositionListener`), 값 갱신 메서드 `onXxxUpdated`, 실패 메서드 `onFailed(e: Exception)`.
- 오류 문서에서는 리스너 계열을 `VehicleListener`로 통칭합니다.

### 2.7 도메인 클래스 목록
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro , https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission

> 도메인별 API Reference 페이지는 모두 404이므로, 아래 "확인된 메서드" 열에는 가이드 예제에 실제로 등장한 메서드만 기재했습니다. 나머지 도메인은 동일한 네이밍 규칙(`checkXxxCapabilities` / `getCurrentXxx` / `setXxx` / `registerXxx`·`unregisterXxx`)을 따르지만 구체 메서드명은 문서에 없습니다.

| 도메인 (`ai.pleos.playground.vehicle.api`) | 설명 (문서 원문 요약) | 대표 조회 퍼미션 | 가이드에서 확인된 획득/메서드 |
| --- | --- | --- | --- |
| `Brake` | 브레이크 페달, 주차 브레이크 상태 조회 | `CAR_DRIVE` | `vehicle.getBrake()` |
| `CarInfo` | 차량 기본 정보(외부 크기, 제조사, 연식 등) 조회 | `CAR_INFO` | — |
| `Display` | 인포테인먼트 표시 데이터 단위(온도 단위 등) 조회/설정 | `CAR_INFO` | `vehicle.getDisplay()`, `getCurrentTemperatureDisplayUnit(onComplete, onFailed)` → `TemperatureUnit?` |
| `Door` | 도어 열림/닫힘, 잠금/해제 제어 및 상태 조회 | `CAR_DOORS` (제어: `CONTROL_CAR_DOORS`) | `vehicle.getDoor()`, `checkDoorPositionCapabilities`, `getCurrentDoorPosition(area, ...)`, `setDoorPosition(area, value, onFailed)`, `registerDoorPosition` / `unregisterDoorPosition(DoorPositionListener)` |
| `DrivingMode` | 드라이브 모드, 크루즈 컨트롤 정보 조회 | `CAR_DRIVE` | — |
| `EvBattery` | EV 배터리·충전 상태 및 전비(효율) 조회 | `CAR_ENERGY` | — |
| `Fuel` | 연비 등 연료 관련 정보 조회 | `CAR_ENERGY` | — |
| `HVAC` | 난방·환기·공조(온도, 풍량, 습도 등) 제어 및 조회 | `CAR_CLIMATE` | — |
| `Light` | 조명 상태 조회 및 앰비언트 라이트 등 제어 | `CAR_LIGHTS` | — |
| `Odometer` | 총 주행 거리, 트립 주행 거리/시간, 주행 거리 단위 조회 | `CAR_DRIVE` | — |
| `Safety` | 차량 경고(Warning) 정보 조회 | `CAR_SAFETY` | — |
| `Seat` | 좌석 위치·각도·기능 제어 및 조회 | `CAR_SEATS` | — |
| `SideMirror` | 사이드미러 열선 상태 조회/설정 | `CAR_MIRRORS` | — |
| `Steeringwheel` (Reference URL은 `steering-wheel`) | 핸들(스티어링휠) 상태, 열선 조회/제어 | 문서에 없음 | — |
| `Tire` | 타이어 상태 조회 | `CAR_SAFETY` | — |
| `Toll` | ETCS(하이패스) 음성 안내 음량 등 조회/설정 | `CAR_TOLL` | — |
| `TurnSignal` | 방향 지시등 상태 조회 | `CAR_LIGHTS` | — |
| `Window` | 창문 제어 및 상태 조회 | `CAR_WINDOWS` | — |
| `Wiper` | 와이퍼 제어 및 상태 조회 | `CAR_WINDOWS` | — |

Area 관련 확인된 식별자: `DoorArea`, `DoorValue.ROW_1_LEFT`. 다른 도메인의 Area 클래스는 문서에 없습니다.

### 2.8 API 개요 표

| 클래스 | 주요 메서드 | 설명 |
| --- | --- | --- |
| `Vehicle` | `Vehicle(context)` | 진입점 생성 |
| | `initialize()` | Vehicle Service 연결 시작 |
| | `release()` | 연결/리소스 해제 (비동기 콜백 완료 후) |
| | `getDoor()`, `getBrake()`, `getDisplay()` 등 `getXxx()` | 도메인 객체 획득 |
| `Door` | `checkDoorPositionCapabilities(onComplete, onFailed)` | 도어 위치 기능의 AccessType·영역별 범위 확인 |
| | `getCurrentDoorPosition(area, onComplete, onFailed)` | 영역별 도어 위치 조회 (`Int?`) |
| | `setDoorPosition(area, value, onFailed)` | 영역별 도어 위치 설정 |
| | `registerDoorPosition(doorPositionListener)` / `unregisterDoorPosition(...)` | 도어 위치 변화 구독/해제 |
| `Display` | `getCurrentTemperatureDisplayUnit(onComplete, onFailed)` | 온도 표시 단위 조회 (`TemperatureUnit?`) |
| `DoorPositionListener` | `onPositionUpdated(pos: Int?)`, `onFailed(e: Exception)` | 도어 위치 변경 콜백 |
| `CapabilityConfig` | `accessType` | `AccessType` (`NONE`/`READ`/`WRITE`/`READ_WRITE`) |
| `AreaBounds<T>` | `minValue`, `maxValue` | 영역별 허용 범위 |
| `VehicleException` | `errorCode` | `ErrorCode` 값 |

### 2.9 오류 코드
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-error-code

- `onFailed` 시그니처: Get/Set APIs, `checkXXXCapabilities`, `VehicleListener`는 모두 Kotlin 표준 `Exception`을 사용하며, `ErrorCode`가 콜백 인자로 직접 전달되지는 않습니다.
- `ErrorCode`는 `ai.pleos.playground.vehicle.constant` 패키지의 enum이며, 공개 API에서는 `VehicleException.errorCode`로 노출됩니다.
- **모든 `onFailed`가 `VehicleException`은 아닐 수 있습니다** → `is VehicleException` 체크 필요.
- 사전에 `checkXXXCapabilities`로 미지원 호출을 줄이고, 퍼미션 오류는 Permission 문서를 확인하라고 권고합니다.

| ErrorCode | 설명 | 권장 처리 |
| --- | --- | --- |
| `ERROR_NOT_SUPPORTED_PROPERTY` | 해당 속성(기능) 미지원 | UI 비활성·대체 흐름 |
| `ERROR_NOT_SUPPORTED_AREA` | 해당 영역 미지원 | 영역 선택 변경 |
| `ERROR_NOT_SUPPORTED_VALUE_TYPE` | 지원하지 않는 값 타입 | 파라미터·타입 재확인 |
| `ERROR_SECURITY_PERMISSION` | 퍼미션 부족 | Permission·`@RequiresPermission` 확인 |
| `ERROR_SERVICE_CONNECTION_FAILURE` | Vehicle 서비스 연결 실패 | `initialize()`·에뮬레이터 상태 확인 |
| `ERROR_INTERNAL_SERVICE` | 내부 서비스 오류 | 재시도·일시 오류 안내 |
| `ERROR_DRIVING_RESTRICTED` | 주행 중 기능 제한 | 주행 제한 안내 |
| `ERROR_EMPTY_RESULT` | 결과 없음 | 빈 상태 UI 처리 |
| `ERROR_UNDEFINED` | 정의되지 않은 오류 | 로그·재시도 |

```kotlin
door.getCurrentDoorPosition(
    area = DoorArea(DoorValue.ROW_1_LEFT),
    onComplete = { pos -> },
    onFailed = { e ->
        if (e is VehicleException) {
            when (e.errorCode) {
                ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY,
                ErrorCode.ERROR_NOT_SUPPORTED_AREA -> { /* UI 비활성 */ }
                ErrorCode.ERROR_SECURITY_PERMISSION -> { /* 매니페스트 퍼미션 확인 */ }
                ErrorCode.ERROR_SERVICE_CONNECTION_FAILURE,
                ErrorCode.ERROR_INTERNAL_SERVICE -> { /* initialize·재시도 */ }
                else -> Log.e("Vehicle API 오류: ${e.errorCode} — ${e.message}")
            }
        } else {
            Log.e("Vehicle API 오류: ${e.message}")
        }
    }
)
```
```java
door.getCurrentDoorPosition(
    new DoorArea(DoorValue.ROW_1_LEFT),
    pos -> Unit.INSTANCE,
    e -> {
        if (e instanceof VehicleException) {
            ErrorCode code = ((VehicleException) e).getErrorCode();
            // code에 따라 분기 처리
        }
        return Unit.INSTANCE;
    }
);
```

### 2.10 Vehicle 권장 호출 흐름 (문서 내용 종합)
1. 매니페스트에 사용 도메인의 `pleos.car.permission.CAR_*` (제어 시 `CONTROL_CAR_*`) 선언
2. `Vehicle(context)` → `initialize()`
3. `getXxx()`로 도메인 객체 획득
4. `checkXxxCapabilities`로 `AccessType`·`AreaBounds` 확인 → UI 활성/비활성 결정
5. `getCurrentXxx` / `setXxx` / `registerXxx` 호출, `onFailed`에서 `VehicleException.errorCode` 분기
6. `unregisterXxx` → (비동기 콜백 종료 확인 후) `release()`

---

## 3. NaviHelper SDK

### 3.1 목적
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro

NaviHelper SDK는 경로 안내 제어와 내비게이션 정보 조회 API를 제공합니다. 3rd party 앱은 NaviHelper SDK를 통해 Pleos Connect의 **Navi Service**와 연동되며, 목적지·경유지 설정, 경로 변경, 정보 조회, Navigation 화면 표시 요청을 Navi Service가 처리하고 결과와 주행 상태 변화를 이벤트로 전달합니다. 요청은 **Pleos Connect Navigation 앱**에 전달되며, 실제 전달 이벤트는 Navigation 앱의 구현과 현재 상태에 따라 달라질 수 있습니다.

- 문서 기준: **NaviHelper SDK v3.0.5**
- 진입점: `ai.pleos.playground.navi.helper.NaviHelper`
- 리스너: `ai.pleos.playground.navi.helper.listener.NaviHelperEventListener`
- 상수: `ai.pleos.playground.navi.constants.NaviErrorCode`
- 데이터: `ai.pleos.playground.navi.data.*` (`RouteStartInfo`, `RouteInfo`, `RouteWithWaypointsRequest`, `RequestWaypointInfo` 등)

제공 기능:
- 목적지 설정·확인·취소
- 경로 옵션 변경 및 재탐색
- 경유지 추가·조회·제거
- 경유지를 포함한 경로 요청
- 경로 탐색과 주행 상태 이벤트 수신
- 내비게이션 정보 조회 및 Navigation 화면 요청 (API Reference 참고 — 해당 페이지는 404)

사용 순서: `NaviHelper` 생성 → `initialize()` → `addListener()` → 경로 요청/정보 조회 API 호출 → 콜백 처리 → `removeListener()` + `release()`.

### 3.2 Gradle 설정
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk

`settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
        }
    }
}
```
`settings.gradle`:
```groovy
dependencyResolutionManagement {
    repositories {
        maven {
            url "https://nexus-playground.pleos.ai/repository/maven-releases/"
        }
    }
}
```
`build.gradle.kts` / `build.gradle`:
```kotlin
dependencies {
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
}
```
```groovy
dependencies {
    implementation "ai.pleos.playground:NaviHelper:3.0.5"
}
```
의존성 추가 후 Gradle 동기화. 사용할 SDK 버전은 프로젝트의 배포 기준에 맞춰 지정합니다.

### 3.3 퍼미션
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions

| Permission | 주요 API |
| --- | --- |
| `pleos.car.permission.NAVI_ROUTE` | `get*Info()`, `requestTrip()` |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | `requestRoute()`, `requestRouteWithWaypoints()`, `cancelRoute()`, `requestReRoute()`, `addWaypoint()`, `removeWaypoint()`, `changeRouteOption()` |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | `requestNavigationScreen()` |

API별 정확한 요구 퍼미션은 API Reference의 `@RequiresPermission`에서 확인합니다.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE" />
    <uses-permission android:name="pleos.car.permission.NAVI_ROUTE_SEARCH" />
    <uses-permission android:name="pleos.car.permission.NAVI_CUSTOM_MAP" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```

### 3.4 앱 등록 / API Key
NaviHelper 문서에는 앱 등록이나 Client ID/Secret 요구 사항이 없습니다.

### 3.5 초기화 및 해제
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/initialization-and-release

앱 시작 또는 생성 시점에 `NaviHelper` 객체를 생성하고 `initialize()`를 호출해야 API를 사용할 수 있습니다.

| Overload | Description |
| --- | --- |
| `initialize(onConnected, onFailed)` | 서비스 연결 결과를 콜백으로 받음. 최초 연결 성공 시 `onConnected`, 초기 연결 재시도가 모두 실패하면 `onFailed` |
| `initialize()` | 인자 없이 초기화 |

연결 상태 콜백을 제공하는 API 사용을 권장합니다.

```kotlin
import ai.pleos.playground.navi.helper.NaviHelper

val naviHelper = NaviHelper(applicationContext)
naviHelper.initialize(
    onConnected = {
        // 최초 서비스 연결이 성공했습니다. 리스너를 등록하고 API를 호출합니다.
    },
    onFailed = { throwable ->
        // 초기 연결 재시도가 모두 실패했습니다.
    }
)
```
```java
import ai.pleos.playground.navi.helper.NaviHelper;
import kotlin.Unit;

NaviHelper naviHelper = new NaviHelper(getApplicationContext());
naviHelper.initialize(
    () -> {
        // 최초 서비스 연결이 성공했습니다. 리스너를 등록하고 API를 호출합니다.
        return Unit.INSTANCE;
    },
    throwable -> {
        // 초기 연결 재시도가 모두 실패했습니다.
        return Unit.INSTANCE;
    }
);
```
```kotlin
naviHelper.initialize()
```

해제 — 등록한 리스너를 제거한 후 `release()`:
```kotlin
override fun onDestroy() {
    naviHelper.removeListener(naviEventListener)
    naviHelper.release()
    super.onDestroy()
}
```
```java
@Override
protected void onDestroy() {
    naviHelper.removeListener(naviEventListener);
    naviHelper.release();
    super.onDestroy();
}
```
`release()` 이후 재사용하려면 객체를 생성하고 초기화를 다시 수행합니다.

### 3.6 이벤트 리스너 (`NaviHelperEventListener`)
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners

| Category | Callback |
| --- | --- |
| 경로 탐색 | `onRouteStarted()`, `onRoutingCompleted()`, `onRouteCancelled()`, `onReRouteEnded()`, `onRouteOptionChanged()` |
| 목적지와 경유지 | `onDestinationChanged()`, `onDestinationArrived()`, `onWaypointChanged()`, `onWayPointArrived()` |
| 정보 조회 | `onBookmarkInfo()`, `onBookMarkChanged()`, `onRecentDestinationInfo()`, `onCurrentLocationInfo()`, `onDestinationInfo()`, `onWayPointInfo()`, `onRouteStateInfo()`, `onTBTInfo()`, `onChargerOperatorInfo()`, `onTripInfo()`, `onNavigationStatus()`, `onDrivingInfo()`, `onCurrentRoadSpeedLimit()`, `onCameraAlert()`, `onCautionPointInfo()` |
| 오류 | `onError()` |

필요한 콜백만 재정의하고, SDK 초기화 후 리스너를 등록합니다. 전달 이벤트는 Navigation 앱 구현 시나리오에 따라 달라질 수 있습니다.

```kotlin
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

private val naviEventListener = object : NaviHelperEventListener {
    override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
        // 경로 탐색 시작을 처리합니다.
    }
    override fun onRoutingCompleted() {
        // 경로 탐색 완료를 처리합니다.
    }
    override fun onError(errorCode: NaviErrorCode) {
        // 내비게이션 오류를 처리합니다.
    }
}
naviHelper.addListener(naviEventListener)
```
```java
private final NaviHelperEventListener naviEventListener =
    new NaviHelperEventListener() {
        @Override
        public void onRouteStarted(RouteStartInfo routeStartInfo) {
            // 경로 탐색 시작을 처리합니다.
        }
        @Override
        public void onRoutingCompleted() {
            // 경로 탐색 완료를 처리합니다.
        }
        @Override
        public void onError(NaviErrorCode errorCode) {
            // 내비게이션 오류를 처리합니다.
        }
    };
naviHelper.addListener(naviEventListener);
```
```kotlin
naviHelper.removeListener(naviEventListener)
```

가이드에서 확인된 콜백 시그니처:
- `onRouteStarted(routeStartInfo: RouteStartInfo)`
- `onRoutingCompleted()`
- `onDestinationInfo(destinationInfo: DestinationInfo)`
- `onRouteOptionChanged(routeOption: RouteOption)`
- `onWayPointInfo(list: List<WaypointInfo>)` (Kotlin 예제에서는 `onWaypointInfo(waypointInfo: List<WaypointInfo>)`로 표기되어 문서 간 대소문자 불일치가 있음 — 이벤트 표·Java 예제·본문은 `onWayPointInfo`)
- `onError(errorCode: NaviErrorCode)`

### 3.7 목적지 설정·확인·취소
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations

`requestRoute()`로 목적지 설정 → `getDestinationInfo()`로 확인 → `cancelRoute()`로 취소. 재설정도 `requestRoute()`.

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)
// 목적지 확인
naviHelper.getDestinationInfo()
// onDestinationInfo callback으로 데이터 수신
override fun onDestinationInfo(destinationInfo: DestinationInfo) {
    Log.d(logTag, "onDestinationInfo $destinationInfo")
}
// 목적지 취소
naviHelper.cancelRoute()
```
```java
// 목적지 설정
naviHelper.requestRoute(new RouteInfo(
    127.03636666,
    37.48544722,
    null,
    null,
    "42dot",
    "36khp37hh8gqcm5nqhe5",
    "서울시 강남구 남부순환로 2621",
    "0",
    RouteOption.RECOMMENDED
));
// 목적지 확인
naviHelper.getDestinationInfo();
// onDestinationInfo callback으로 데이터 수신
@Override
public void onDestinationInfo(DestinationInfo destinationInfo) {
    Log.d(logTag, "onDestinationInfo = " + destinationInfo);
}
// 목적지 취소
naviHelper.cancelRoute();
```

경로 탐색 과정 콜백:

| Callback | Description |
| --- | --- |
| `onRouteStarted(routeStartInfo)` | Navigation 앱이 경로 탐색을 시작하면 호출 |
| `onRoutingCompleted()` | 경로 탐색 완료 시 호출 |
| `onDestinationChanged()` | 목적지가 설정되거나 제거되면 호출 |
| `onError(errorCode)` | 경로 요청 처리 중 오류 발생 시 호출 |

> `requestRoute()` 호출이 정상 수행되더라도 경로 탐색 완료를 의미하지 않습니다. 최종 결과는 이벤트 콜백으로 확인합니다. 정보 조회 API는 값을 직접 반환하지 않습니다.

`RouteInfo` 필드(Kotlin 예제 기준): `latitude`, `longitude`, `displayLongitude`, `displayLatitude`, `poiName`, `poiId`, `poiSubId`, `address`, `routeOption`(Routes 문서에서 `RouteInfo.routeOption`으로 언급).
> 주의: Java 예제의 위치 인자 순서가 문서마다 다릅니다. Destinations/Routes/Waypoints 예제는 `(127.03636666, 37.48544722, ...)`로 경도가 먼저이고 마지막에 `RouteOption.RECOMMENDED`가 있는 반면, Route-with-waypoints 예제는 `(37.48544722, 127.03636666, ...)`로 위도가 먼저이고 `RouteOption` 인자가 없습니다. Kotlin에서는 named argument 사용을 권장할 만합니다(정확한 생성자 시그니처는 Reference 404로 확인 불가).

### 3.8 경로 옵션 변경 및 재탐색
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/routes

`RouteOption` 값:

| Value | Description |
| --- | --- |
| `RECOMMENDED` | 추천 경로 |
| `SHORT_DISTANCE` | 최단 거리 경로 |
| `SHORT_TIME` | 최단 시간 경로 |
| `FREE` | 무료 도로 우선 경로 |
| `HIGHWAY` | 고속도로 우선 경로 |
| `COMFORTABLE` | 편안한 경로 |
| `SCHOOL_ZONE` | 스쿨존 고려 경로 |

목적지 최초 설정 시 `RouteInfo.routeOption`으로 지정하거나, 안내 중 `changeRouteOption()`으로 변경합니다.

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)
// 경로옵션: 고속우선으로 변경
naviHelper.changeRouteOption(RouteOption.HIGHWAY)
// onRouteOptionChanged callback으로 변경 데이터 수신
override fun onRouteOptionChanged(routeOption: RouteOption) {
    Log.d(logTag, "onRouteOptionChanged $routeOption")
}
// 목적지 취소
naviHelper.cancelRoute()
```

재탐색 (현재 위치·목적지 기준):
```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)
// 경로 재탐색 요청
naviHelper.requestReRoute()
// 목적지 취소
naviHelper.cancelRoute()
```
> 경로 옵션 변경과 재탐색은 **활성 경로가 있는 상태**에서 요청합니다. 처리 결과와 콜백은 Navigation 앱 상태에 따라 달라질 수 있습니다.

### 3.9 경유지
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/waypoint-settings

경유지는 **최대 두 개**, `WaypointIndex.FIRST` / `WaypointIndex.SECOND`로 구분. `addWaypoint()`로 추가, `removeWaypoint()`로 삭제, `removeWaypoint(WaypointIndex.ALL)`로 전체 제거, `getWaypointInfo()`로 조회(결과는 `onWayPointInfo()`), 경유지 도착 시 `onWayPointArrived()`.

```kotlin
// 목적지 설정
naviHelper.requestRoute(
    RouteInfo(
        latitude = 37.48544722,
        longitude = 127.03636666,
        displayLongitude = null,
        displayLatitude = null,
        poiName = "42dot",
        poiId = "36khp37hh8gqcm5nqhe5",
        poiSubId = "0",
        address = "서울시 강남구 남부순환로 2621"
    )
)
// 경유지 추가
naviHelper.addWaypoint(
    RequestWaypointInfo(
        latitude = 37.41275833,
        longitude = 127.09481944,
        waypointIndex = WaypointIndex.FIRST,
        poiId = "36khp2hdj2cfcm5nqhe3",
        poiName = "소프트웨어드림센터",
        poiSubId = "0",
        address = "경기도 성남시 수정구 창업로40번길 6"
    )
)
// onWaypointInfo callback으로 데이터 수신
override fun onWaypointInfo(waypointInfo: List<WaypointInfo>) {
    Log.d(logTag, "onWaypointInfo $waypointInfo")
}
// 경유지 삭제
naviHelper.removeWaypoint(WaypointIndex.FIRST)
// 목적지 취소
naviHelper.cancelRoute()
```
```kotlin
naviHelper.getWaypointInfo()
// 모든 경유지 제거
naviHelper.removeWaypoint(WaypointIndex.ALL)
```

### 3.10 경유지를 포함한 경로 요청
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints

목적지는 `RouteInfo`, 경유지는 `RequestWaypointInfo`로 구성해 `RouteWithWaypointsRequest`에 전달합니다. 퍼미션 `pleos.car.permission.NAVI_ROUTE_SEARCH` 필요. 결과는 `onRouteStarted`, `onRoutingCompleted`, `onError` 등으로 확인.

```kotlin
naviHelper.requestRouteWithWaypoints(
    RouteWithWaypointsRequest(
        destination = RouteInfo(
            latitude = 37.48544722,
            longitude = 127.03636666,
            poiName = "42dot",
            poiId = "36khp37hh8gqcm5nqhe5",
            address = "서울시 강남구 남부순환로 2621",
            poiSubId = "0"
        ),
        waypoints = listOf(
            RequestWaypointInfo(
                latitude = 37.41275833,
                longitude = 127.09481944,
                poiName = "판교 현대백화점",
                waypointIndex = WaypointIndex.FIRST,
                poiId = "36khp2hdj2cfcm5nqhe3",
                address = "경기도 성남시 수정구 창업로 0번길 6",
                poiSubId = "0"
            )
        )
    )
)
```
```java
import java.util.Arrays;

naviHelper.requestRouteWithWaypoints(new RouteWithWaypointsRequest(
    new RouteInfo(
        37.48544722,
        127.03636666,
        null,
        null,
        "42dot",
        "36khp37hh8gqcm5nqhe5",
        "서울시 강남구 남부순환로 2621",
        "0"
    ),
    Arrays.asList(new RequestWaypointInfo(
        37.41275833,
        127.09481944,
        "판교 현대백화점",
        WaypointIndex.FIRST,
        "36khp2hdj2cfcm5nqhe3",
        "경기도 성남시 수정구 창업로 0번길 6",
        "0"
    ))
));
```

### 3.11 오류 처리
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/error-handling

초기 서비스 연결 실패와 Navigation 앱의 요청 처리 오류는 서로 다른 경로로 전달됩니다.

초기화 에러 — `initialize()` 콜백:
```kotlin
naviHelper.initialize(
    onConnected = {
        // 리스너를 등록하고 API를 호출합니다.
    },
    onFailed = { throwable ->
        // 연결 실패 원인을 기록하거나 사용자에게 안내합니다.
    }
)
```

내비게이션 에러 — `NaviHelperEventListener.onError()`:
```kotlin
override fun onError(errorCode: NaviErrorCode) {
    when (errorCode) {
        NaviErrorCode.CAUSE_LOW_BATTERY -> {
            // 배터리 부족 상태를 안내합니다.
        }
        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> {
            // 탐색 가능 거리를 초과했음을 안내합니다.
        }
        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> {
            // 목적지가 현재 위치와 너무 가까움을 안내합니다.
        }
        NaviErrorCode.CAUSE_SEARCH_ROUTE -> {
            // 경로 탐색 서버 오류를 처리합니다.
        }
    }
}
```

| `NaviErrorCode` | Description |
| --- | --- |
| `CAUSE_LOW_BATTERY` | 배터리가 부족합니다. |
| `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | 경로 탐색 가능 거리를 초과했습니다. |
| `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | 현재 위치와 목적지 사이의 거리가 너무 짧습니다. |
| `CAUSE_SEARCH_ROUTE` | 경로 탐색 서버에서 오류가 발생했습니다. |

오류 발생 시 점검 항목:
- SDK 연결 완료 후 API를 호출했는지
- 사용하는 API에 필요한 퍼미션을 선언했는지
- 좌표, POI ID 및 경유지 인덱스가 올바른지
- 현재 Navigation 앱 상태에서 요청을 처리할 수 있는지

오류 코드 최신 목록은 NaviHelper API Reference를 보라고 안내하지만 해당 페이지는 404입니다.

### 3.12 API 개요 표

| 클래스 | 메서드 | 설명 | 퍼미션 |
| --- | --- | --- | --- |
| `NaviHelper` | `NaviHelper(context)` | 진입점 생성 | — |
| | `initialize(onConnected, onFailed)` / `initialize()` | Navi Service 연결 | — |
| | `addListener(listener)` / `removeListener(listener)` | 이벤트 리스너 등록/해제 | — |
| | `release()` | 리소스 해제 | — |
| | `requestRoute(RouteInfo)` | 목적지 설정(재설정 포함) | `NAVI_ROUTE_SEARCH` |
| | `requestRouteWithWaypoints(RouteWithWaypointsRequest)` | 목적지+경유지 경로 요청 | `NAVI_ROUTE_SEARCH` |
| | `cancelRoute()` | 목적지/경로 취소 | `NAVI_ROUTE_SEARCH` |
| | `changeRouteOption(RouteOption)` | 안내 중 경로 옵션 변경 | `NAVI_ROUTE_SEARCH` |
| | `requestReRoute()` | 경로 재탐색 | `NAVI_ROUTE_SEARCH` |
| | `addWaypoint(RequestWaypointInfo)` | 경유지 추가 (최대 2) | `NAVI_ROUTE_SEARCH` |
| | `removeWaypoint(WaypointIndex)` | 경유지 삭제 (`ALL` 가능) | `NAVI_ROUTE_SEARCH` |
| | `getDestinationInfo()` | 목적지 정보 요청 → `onDestinationInfo` | `NAVI_ROUTE` (`get*Info()`) |
| | `getWaypointInfo()` | 경유지 목록 요청 → `onWayPointInfo` | `NAVI_ROUTE` (`get*Info()`) |
| | 기타 `get*Info()` | 정보 조회 (구체 목록은 문서에 없음, 콜백 목록으로 유추 가능) | `NAVI_ROUTE` |
| | `requestTrip()` | 퍼미션 표에만 등장 (설명 문서에 없음) | `NAVI_ROUTE` |
| | `requestNavigationScreen()` | Navigation 화면 요청 (상세 문서에 없음) | `NAVI_CUSTOM_MAP` |
| `NaviHelperEventListener` | 3.6 표 참조 | 결과·상태 콜백 | — |
| `RouteOption` | enum 7종 | 경로 옵션 | — |
| `WaypointIndex` | `FIRST`, `SECOND`, `ALL` | 경유지 인덱스 | — |
| `NaviErrorCode` | 4종 | 내비게이션 오류 | — |

---

## 4. SpeechToText (STT) SDK

### 4.1 목적
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro

음성을 텍스트로 변환하는 API. 3rd party 앱은 Pleos Connect의 **Speech Service**와 연동하며, Speech Service가 인식 결과 텍스트를 앱에 전달합니다.

- **음성 명령 기능은 제공하지 않습니다.** `"음악 틀어줘"` 같은 텍스트만 반환하며, 실제 동작(음악 재생 등)은 실행하지 않습니다. 명령 실행이 필요하면 **Gleo Interface**(`/docs/pleos-only/app-integration-guide/guide/core-concepts`)를 참고합니다.
- 문서 기준: **SpeechToText SDK v3.3.1**
- 진입점: `ai.pleos.playground.stt.SpeechToText` — 생성자에 애플리케이션 `Context`와 인식 `Mode` 전달 (`Mode` 생략 시 `HYBRID`)
- 패키지: `ai.pleos.playground.stt.constant` (`Mode`, `ErrorType`), `ai.pleos.playground.stt.listener` (`OnInitializeListener`, `OnServerConnectionListener`, `ResultListener`)

#### 인식 모드 (`Mode`)
| 모드 | 동작 | 네트워크 | 앱 등록 |
| --- | --- | --- | --- |
| `HYBRID` (기본) | 서버 인식 우선, 네트워크 실패 시 On-device 엔진으로 자동 전환 | 서버 사용 시 필요 | 서버 인식 사용 시 필요 |
| `SERVER` | 원격 Speech Service로 전송하여 인식, 실패해도 On-device로 전환하지 않음 | 필요 | 필요 |
| `ON_DEVICE` | 차량 On-device 엔진에서 인식 | 불필요 | 불필요 |

#### 사용 순서
1. `SpeechToText` 생성
2. `initialize()`
3. `addListener()`로 `ResultListener` 등록
4. 필요 시 `setLocale()`
5. `HYBRID`/`SERVER` 모드면 앱 등록
6. `request()`로 인식 시작
7. 종료 시 `stop()`
8. `removeListener()` 후 `release()`

```kotlin
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode

val speechToText = SpeechToText(applicationContext, Mode.HYBRID)
```
```java
import ai.pleos.playground.stt.SpeechToText;
import ai.pleos.playground.stt.constant.Mode;

SpeechToText speechToText =
    new SpeechToText(getApplicationContext(), Mode.HYBRID);
```

### 4.2 Gradle 설정
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/importing-sdk

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```
```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url "https://nexus-playground.pleos.ai/repository/maven-releases/"
        }
    }
}
```
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}
```
```groovy
// app/build.gradle
dependencies {
    implementation "ai.pleos.playground:SpeechToText:3.3.1"
}
```

### 4.3 퍼미션
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.STT_SERVICE" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```
SDK 초기화, 결과 리스너 등록, 앱 등록, 음성 인식 요청 및 오디오 데이터 전송 등 SpeechToText 서비스에 접근하는 API 전반에 필요합니다. 미선언 시 초기화/API 호출이 실패할 수 있으며 `PERMISSION_DENIED`가 전달될 수 있습니다.

### 4.4 앱 등록 (Client ID / Client Secret)
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/app-registration

서버 기반 인식에는 **Pleos Playground에서 My Project 생성 시 발급받은 `Client ID`와 `Client Secret`**으로 앱을 등록해야 합니다. **SDK 초기화 완료 후** `registerApp()` 호출.
- `SERVER` 모드: 등록 필수
- `HYBRID` 모드: 서버 인식을 쓰려면 등록 필요
- `ON_DEVICE` 전용: 등록 불필요

```kotlin
speechToText.registerApp(
    clientID = clientId,
    clientSecret = clientSecret,
    listener = object : OnServerConnectionListener {
        override fun onConnected() {
            // 서버 기반 음성 인식을 요청할 수 있습니다.
        }
        override fun onFailed(errorType: ErrorType) {
            // 등록 또는 서버 연결 실패를 처리합니다.
        }
    }
)
```
```java
speechToText.registerApp(
    clientId,
    clientSecret,
    new OnServerConnectionListener() {
        @Override
        public void onConnected() {
            // 서버 기반 음성 인식을 요청할 수 있습니다.
        }
        @Override
        public void onFailed(ErrorType errorType) {
            // 등록 또는 서버 연결 실패를 처리합니다.
        }
    }
);
```
`onConnected()` 호출 이후 서버 기반 요청을 시작합니다. (start-and-stop 문서와 오류 표에는 "UA 인증" / "UA 앱 정보"라는 대안 인증 경로도 언급되지만 상세 설명은 문서에 없습니다.)

### 4.5 초기화 및 해제
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/initialization-and-release

```kotlin
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.OnInitializeListener

speechToText.initialize(object : OnInitializeListener {
    override fun onSuccess() {
        // SpeechToText API를 사용할 수 있습니다.
    }
    override fun onFailure(errorType: ErrorType) {
        // 초기화 실패 원인을 처리합니다.
    }
})
```
```java
import ai.pleos.playground.stt.constant.ErrorType;
import ai.pleos.playground.stt.listener.OnInitializeListener;

speechToText.initialize(new OnInitializeListener() {
    @Override
    public void onSuccess() {
        // SpeechToText API를 사용할 수 있습니다.
    }
    @Override
    public void onFailure(ErrorType errorType) {
        // 초기화 실패 원인을 처리합니다.
    }
});
```
```kotlin
speechToText.initialize()
```

해제 — 객체는 컴포넌트 수명주기에 맞춰 재사용, 종료 시 리스너 제거 후 `release()`:
```kotlin
override fun onDestroy() {
    speechToText.removeListener(resultListener)
    speechToText.release()
    super.onDestroy()
}
```
```java
@Override
protected void onDestroy() {
    speechToText.removeListener(resultListener);
    speechToText.release();
    super.onDestroy();
}
```
`release()` 후 재사용 시 새 객체 생성 + 재초기화.

### 4.6 결과 리스너 (`ResultListener`)
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/result-listeners

| Callback | Description |
| --- | --- |
| `onReady()` | 인식 요청 처리 준비 완료 |
| `onStartedRecognition()` | 인식 시작 |
| `onUpdated(stt, completed)` | 인식 텍스트 갱신. `completed == true`면 해당 결과 완료(최종) |
| `onUpdatedEpdData(on, off)` | EPD(End Point Detection)가 감지한 음성 시작(`on`)/종료(`off`) 위치 갱신. 값은 입력 오디오 버퍼의 byte offset |
| `onUpdatedRms(rms)` | 입력 신호 RMS 갱신 (dB 값 아님) |
| `onEndedRecognition()` | 인식 종료 |
| `onError(errorType)` | 오류 발생 |

```kotlin
private val resultListener = object : ResultListener {
    override fun onReady() = Unit
    override fun onStartedRecognition() = Unit
    override fun onUpdated(stt: String, completed: Boolean) {
        // 중간 또는 최종 인식 결과를 처리합니다.
    }
    override fun onUpdatedEpdData(on: Long, off: Long) = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onEndedRecognition() = Unit
    override fun onError(errorType: ErrorType) {
        // 오류를 처리합니다.
    }
}
speechToText.addListener(resultListener)
```
```java
private final ResultListener resultListener = new ResultListener() {
    @Override public void onReady() {}
    @Override public void onStartedRecognition() {}
    @Override
    public void onUpdated(String stt, boolean completed) {
        // 중간 또는 최종 인식 결과를 처리합니다.
    }
    @Override public void onUpdatedEpdData(long on, long off) {}
    @Override public void onUpdatedRms(double rms) {}
    @Override public void onEndedRecognition() {}
    @Override
    public void onError(ErrorType errorType) {
        // 오류를 처리합니다.
    }
};
speechToText.addListener(resultListener);
```
- 이벤트 누락 방지를 위해 `request()` 또는 `sendAudio()` **호출 전에** 리스너를 등록합니다.
- 해제: `speechToText.removeListener(resultListener)` (동일 인스턴스)

### 4.7 로케일 설정
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/set-locale

`setLocale(locale: Locale)` — `request()` 전에 호출. 설정한 locale은 다음 인식 요청의 기준으로 사용됩니다.

지원 locale: `ko-KR`(한국어), `en-US`(영어), `fr-CA`(프랑스어), `es-US`/`es-MX`(스페인어)

미호출 시: 같은 앱에서 이전에 `setLocale()`로 설정한 값이 있으면 그 값을, 없으면 시스템 locale을 사용.

```kotlin
import java.util.Locale
speechToText.setLocale(Locale.KOREA)
```
```java
import java.util.Locale;
speechToText.setLocale(Locale.KOREA);
```

### 4.8 인식 시작/중지
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop

`request()` — SDK가 차량 마이크를 활성화하고 인식 시작. 호출 전 초기화·리스너 등록 완료, 서버 모드에서는 앱 등록(또는 UA 인증) 완료 필요.
```kotlin
speechToText.addListener(resultListener)
speechToText.request()
```
```java
speechToText.addListener(resultListener);
speechToText.request();
```
`stop()` — 진행 중 인식 중지 및 마이크 사용 종료. 종료되면 `ResultListener.onEndedRecognition()` 호출.
```kotlin
speechToText.stop()
```

### 4.9 오디오 데이터 직접 전송
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/sending-audio-data

차량 마이크 대신 앱이 보유한 PCM 오디오를 `sendAudio(ByteArray)`로 전달. **`ON_DEVICE` 모드에서만 사용 가능.** 전체를 한 번에 보내거나 청크로 나눠 전송 가능. 마지막 전송 후 반드시 `completeAudioSend()` 호출. 결과는 `ResultListener`로 전달.

> 예시의 3,200-byte 청크 / 100ms 간격은 16 kHz, 16-bit, mono PCM을 실시간 속도로 전송하는 경우입니다. 다른 형식은 100ms 분량에 맞춰 청크 크기를 계산합니다.

전체 버퍼 전송:
```kotlin
val pcmData = applicationContext.assets
    .open("test_audio.pcm")
    .use { it.readBytes() }
speechToText.sendAudio(pcmData)
speechToText.completeAudioSend()
```
청크 스트리밍:
```kotlin
private const val BYTES_PER_100_MS = 3_200

val pcmData = applicationContext.assets
    .open("test_audio.pcm")
    .use { it.readBytes() }
pcmData.asList()
    .chunked(BYTES_PER_100_MS)
    .forEach { chunk ->
        speechToText.sendAudio(chunk.toByteArray())
        Thread.sleep(100L)
    }
speechToText.completeAudioSend()
```
```java
private static final int BYTES_PER_100_MS = 3_200;

byte[] buffer = new byte[BYTES_PER_100_MS];
try (InputStream input = getAssets().open("test_audio.pcm")) {
    int read;
    while ((read = input.read(buffer)) != -1) {
        speechToText.sendAudio(Arrays.copyOf(buffer, read));
        Thread.sleep(100L);
    }
}
speechToText.completeAudioSend();
```

### 4.10 오류 코드 (`ErrorType`)
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/error-code

오류는 `OnInitializeListener`, `OnServerConnectionListener`, `ResultListener`에서 처리합니다.

| ErrorType | Description | Recommended action |
| --- | --- | --- |
| `AUDIO_ERROR` | 입력 오디오 데이터 처리 실패 | 입력 데이터와 오디오 형식 확인 후 재요청 |
| `INVALID_REQUEST_ERROR` | 요청 값이나 요청 상태가 올바르지 않음 | 호출 순서, 중복 요청, 입력 값 확인 |
| `MIC_ERROR` | 마이크 입력 시작/처리 중 오류 | 마이크 상태와 센서 개인정보 보호 설정 확인 |
| `NETWORK_ERROR` | 네트워크 통신 실패 | 네트워크 연결 확인 후 재요청 |
| `NOT_SUPPORTED_API` | 현재 모드/환경에서 지원하지 않는 API | 선택한 `Mode`와 플랫폼 지원 범위 확인 |
| `ON_DEVICE_ENGINE_ERROR` | On-device 인식 엔진 오류 | 엔진 상태 확인 또는 `HYBRID`/`SERVER` 검토 |
| `PERMISSION_DENIED` | STT 서비스 접근 권한 없음 | manifest의 `STT_SERVICE` 권한과 실행 환경 권한 상태 확인 |
| `SERVER_INTERNAL_ERROR` | Speech Service 내부 오류 | 잠시 후 재요청, 반복 시 서비스 관리자 문의 |
| `TIMEOUT` | 제한 시간 내 미완료 | 네트워크·서버 상태 확인 후 재요청 |
| `UNAUTHENTICATED` | 앱 인증 실패 | Pleos Playground My Project 자격 증명 또는 UA 앱 정보와 접속 환경 확인 |
| `UNAVAILABLE_LANGUAGE` | 요청 언어 사용 불가 | 지원되는 `Locale`로 변경 |
| `UNKNOWN` | 분류되지 않은 오류 | 호출 시점·환경 정보 수집 후 서비스 관리자 문의 |

새 타입이 추가될 수 있으므로 `when`/`switch`에 기본 분기를 두는 것을 권장합니다.
```kotlin
override fun onError(errorType: ErrorType) {
    when (errorType) {
        ErrorType.PERMISSION_DENIED -> checkSttPermission()
        ErrorType.UNAUTHENTICATED -> checkCredentials()
        ErrorType.NETWORK_ERROR,
        ErrorType.TIMEOUT -> retryWhenNetworkIsAvailable()
        else -> logError(errorType.name)
    }
}
```

### 4.11 API 개요 표

| 클래스 | 메서드 | 설명 |
| --- | --- | --- |
| `SpeechToText` | `SpeechToText(context, mode)` | 생성 (`mode` 생략 시 `HYBRID`) |
| | `initialize(OnInitializeListener)` / `initialize()` | SDK 초기화 |
| | `addListener(ResultListener)` / `removeListener(ResultListener)` | 결과 리스너 등록/해제 |
| | `registerApp(clientID, clientSecret, listener: OnServerConnectionListener)` | 서버 인식용 앱 등록 |
| | `setLocale(locale: Locale)` | 인식 언어/지역 설정 |
| | `request()` | 차량 마이크로 인식 시작 |
| | `stop()` | 인식 중지·마이크 종료 |
| | `sendAudio(ByteArray)` | PCM 직접 전송 (`ON_DEVICE` 전용) |
| | `completeAudioSend()` | 오디오 입력 종료 알림 |
| | `release()` | 리소스 해제 |
| `OnInitializeListener` | `onSuccess()`, `onFailure(ErrorType)` | 초기화 결과 |
| `OnServerConnectionListener` | `onConnected()`, `onFailed(ErrorType)` | 앱 등록/서버 연결 결과 |
| `ResultListener` | 4.6 표 참조 | 인식 이벤트 |
| `Mode` | `HYBRID`, `SERVER`, `ON_DEVICE` | 인식 모드 |
| `ErrorType` | 12종 | 오류 분류 |

---

## 5. TextToSpeech (TTS) SDK

### 5.1 목적
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro

텍스트를 음성으로 합성·재생하는 API. 3rd party 앱은 Pleos Connect의 **Speech Service**와 연동하며, Speech Service가 합성한 음성을 **차량의 오디오 시스템으로 재생**합니다.

- 문서 기준: **TextToSpeech SDK v3.3.1**
- 진입점: `ai.pleos.playground.tts.TextToSpeech` — 생성자에 `Context`와 실행 `Mode` (생략 시 `HYBRID`)
- 패키지: `ai.pleos.playground.tts.constant` (`Mode`, `ErrorType`), `ai.pleos.playground.tts.listener` (`OnInitializeListener`, `OnServerConnectionListener`, `EventListener`)

#### 합성 모드 (`Mode`)
| 모드 | 동작 | 네트워크 | 앱 등록 |
| --- | --- | --- | --- |
| `HYBRID` (기본) | 서버 기반 streaming TTS 우선, 네트워크 실패 시 On-device로 자동 전환 | 서버 사용 시 필요 | 서버 합성 사용 시 필요 |
| `SERVER` | 서버 기반 streaming TTS만 사용, 실패 시 전환하지 않고 오류 이벤트 전달 | 필요 | 필요 |
| `ON_DEVICE` | 차량 On-device 엔진에서 Streaming TTS 실행 | 불필요 | 불필요 |

#### 사용 순서
1. `TextToSpeech` 생성
2. `initialize()`
3. `addEventListener()`로 이벤트 리스너 등록
4. 필요 시 `setLocale()`
5. `HYBRID`/`SERVER`면 앱 등록
6. 서버 연결이 필요한 모드에서는 `OnServerConnectionListener.onConnected()` 이후 `speak()`
7. 종료 시 `stop()` (문서 원문은 "인식이 끝나면"으로 표기 — STT 문서 문구가 그대로 남은 것으로 보임)
8. `removeEventListener()` 후 `release()`

```kotlin
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode

val textToSpeech = TextToSpeech(applicationContext, Mode.HYBRID)
```
```java
import ai.pleos.playground.tts.TextToSpeech;
import ai.pleos.playground.tts.constant.Mode;

TextToSpeech textToSpeech =
    new TextToSpeech(getApplicationContext(), Mode.HYBRID);
```

### 5.2 Gradle 설정
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk

저장소 설정은 STT와 동일(`google()`, `mavenCentral()`, Pleos Nexus).
```kotlin
// app/build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
}
```
```groovy
// app/build.gradle
dependencies {
    implementation "ai.pleos.playground:TextToSpeech:3.3.1"
}
```

### 5.3 퍼미션
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="pleos.car.permission.TTS_SERVICE" />
    <application>
        <!-- ... -->
    </application>
</manifest>
```
SDK 초기화, 이벤트 리스너 등록, 앱 등록, 음성 합성 요청 등 TTS 서비스 접근 API 전반에 필요. 미선언 시 `PERMISSION_DENIED` 가능.

### 5.4 앱 등록
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/app-registration

STT와 동일: Pleos Playground My Project의 `Client ID` / `Client Secret`, 초기화 완료 후 `registerApp()`. `SERVER` 필수, `HYBRID` 서버 합성 시 필요, `ON_DEVICE` 전용은 불필요.

```kotlin
textToSpeech.registerApp(
    clientID = clientId,
    clientSecret = clientSecret,
    listener = object : OnServerConnectionListener {
        override fun onConnected() {
            // 서버 기반 음성 합성을 요청할 수 있습니다.
        }
        override fun onFailed(errorType: ErrorType) {
            // 등록 또는 서버 연결 실패를 처리합니다.
        }
    }
)
```
`onConnected()` 이후 서버 기반 요청을 시작합니다.

### 5.5 초기화 및 해제
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release

```kotlin
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.tts.constant.ErrorType

textToSpeech.initialize(object : OnInitializeListener {
    override fun onSuccess() {
        // TextToSpeech API를 사용할 수 있습니다.
    }
    override fun onFailure(errorType: ErrorType) {
        // 초기화 실패 원인을 처리합니다.
    }
})
```
```kotlin
textToSpeech.initialize()
```
```kotlin
override fun onDestroy() {
    textToSpeech.removeEventListener(eventListener)
    textToSpeech.release()
    super.onDestroy()
}
```
```java
@Override
protected void onDestroy() {
    textToSpeech.removeEventListener(eventListener);
    textToSpeech.release();
    super.onDestroy();
}
```
- 객체는 컴포넌트 수명주기에 맞춰 재사용, 앱 종료 시 반드시 `release()`.
- `release()` 후 재사용 시 새 객체 생성 + 재초기화.

### 5.6 이벤트 리스너 (`EventListener`)
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners

| Callback | Description |
| --- | --- |
| `onReady()` | `speak()` 요청 처리 준비 완료 |
| `onStart()` | 음성 재생 시작 |
| `onUpdatedRms(rms)` | 재생 중 RMS 갱신 |
| `onDone()` | 재생 정상 종료 |
| `onStop()` | 진행 중 재생 중지됨 |
| `onError(errorType)` | 오류 발생 |

```kotlin
private val eventListener = object : EventListener {
    override fun onReady() = Unit
    override fun onStart() = Unit
    override fun onUpdatedRms(rms: Double) = Unit
    override fun onDone() = Unit
    override fun onStop() = Unit
    override fun onError(errorType: ErrorType) {
        // 오류를 처리합니다.
    }
}
textToSpeech.addEventListener(eventListener)
```
```java
private final EventListener eventListener = new EventListener() {
    @Override public void onReady() {}
    @Override public void onStart() {}
    @Override public void onUpdatedRms(double rms) {}
    @Override public void onDone() {}
    @Override public void onStop() {}
    @Override
    public void onError(ErrorType errorType) {
        // 오류를 처리합니다.
    }
};
textToSpeech.addEventListener(eventListener);
```
```kotlin
textToSpeech.removeEventListener(eventListener)
```
> STT는 `addListener`/`removeListener`, TTS는 `addEventListener`/`removeEventListener`로 메서드명이 다릅니다.

### 5.7 로케일 설정
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale

`setLocale(locale: Locale)` — `speak()` 전에 호출. 지원 locale 및 미호출 시 기본 동작은 STT와 동일 (`ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`; 이전 설정값 → 시스템 locale).
```kotlin
import java.util.Locale
textToSpeech.setLocale(Locale.KOREA)
```

### 5.8 합성 시작/중지
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop

`speak(text, isFinal = true)` — `isFinal` 기본값 `true`(하나의 완성된 요청). 여러 조각을 하나의 발화로 보낼 때는 마지막을 제외하고 `isFinal = false`.
```kotlin
textToSpeech.speak("안녕하세요. 무엇을 도와드릴까요?")
```
```java
textToSpeech.speak("안녕하세요. 무엇을 도와드릴까요?", true);
```
```kotlin
textToSpeech.speak("안녕하세요. ", isFinal = false)
textToSpeech.speak("TextToSpeech SDK입니다.", isFinal = true)
```
앱 등록이 필요한 모드에서는 `OnServerConnectionListener.onConnected()` 이후 요청.

`stop()` — 진행 중인 합성과 재생 중지. 완료되면 `EventListener.onStop()` 호출.
```kotlin
textToSpeech.stop()
```

### 5.9 오류 코드 (`ErrorType`)
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/error-code

오류는 `OnInitializeListener`, `OnServerConnectionListener`, `EventListener`에서 처리.

| ErrorType | Description | Recommended action |
| --- | --- | --- |
| `AUDIO_ERROR` | 오디오 출력 또는 재생 처리 실패 | 오디오 설정과 출력 장치 상태 확인 후 재요청 |
| `INVALID_REQUEST_ERROR` | 요청 값/상태가 올바르지 않음 | 텍스트와 호출 순서 등 요청 조건 확인 |
| `MIC_ERROR` | 오디오 입력 장치 처리 중 오류 | 실행 환경의 오디오 장치 상태 확인 |
| `NETWORK_ERROR` | 네트워크 통신 실패 | 네트워크 연결 확인 후 재요청 |
| `NOT_SUPPORTED_API` | 현재 모드/환경 미지원 API | 선택한 `Mode`와 플랫폼 지원 범위 확인 |
| `ON_DEVICE_ENGINE_ERROR` | On-device 합성 엔진 오류 | 엔진 상태 확인 또는 `HYBRID`/`SERVER` 검토 |
| `PERMISSION_DENIED` | TTS 서비스 접근 권한 없음 | manifest의 `TTS_SERVICE` 권한과 실행 환경 권한 상태 확인 |
| `SERVER_INTERNAL_ERROR` | Speech Service 내부 오류 | 잠시 후 재요청, 반복 시 서비스 관리자 문의 |
| `TIMEOUT` | 제한 시간 내 미완료 | 네트워크·서버 상태 확인 후 재요청 |
| `UNAUTHENTICATED` | 앱 인증 실패 | My Project 자격 증명 또는 UA 앱 정보와 접속 환경 확인 |
| `UNAVAILABLE_LANGUAGE` | 요청 언어 사용 불가 | 지원 `Locale`로 변경 |
| `UNKNOWN` | 분류되지 않은 오류 | 호출 시점·환경 정보 수집 후 서비스 관리자 문의 |

```kotlin
override fun onError(errorType: ErrorType) {
    when (errorType) {
        ErrorType.PERMISSION_DENIED -> checkTtsPermission()
        ErrorType.UNAUTHENTICATED -> checkCredentials()
        ErrorType.NETWORK_ERROR,
        ErrorType.TIMEOUT -> retryWhenNetworkIsAvailable()
        else -> logError(errorType.name)
    }
}
```

### 5.10 API 개요 표

| 클래스 | 메서드 | 설명 |
| --- | --- | --- |
| `TextToSpeech` | `TextToSpeech(context, mode)` | 생성 (`mode` 생략 시 `HYBRID`) |
| | `initialize(OnInitializeListener)` / `initialize()` | 초기화 |
| | `addEventListener(EventListener)` / `removeEventListener(EventListener)` | 이벤트 리스너 등록/해제 |
| | `registerApp(clientID, clientSecret, listener)` | 서버 합성용 앱 등록 |
| | `setLocale(locale: Locale)` | 합성 언어/지역 설정 |
| | `speak(text, isFinal = true)` | 합성·재생 요청 (조각 스트리밍 가능) |
| | `stop()` | 합성·재생 중지 |
| | `release()` | 리소스 해제 |
| `EventListener` | 5.6 표 참조 | 재생 이벤트 |
| `OnInitializeListener` | `onSuccess()`, `onFailure(ErrorType)` | 초기화 결과 |
| `OnServerConnectionListener` | `onConnected()`, `onFailed(ErrorType)` | 등록/서버 연결 결과 |
| `Mode` | `HYBRID`, `SERVER`, `ON_DEVICE` | 합성 모드 |
| `ErrorType` | 12종 | 오류 분류 |

---

## 6. FusedLocation SDK

### 6.1 목적
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro

앱이 **차량 위치 데이터**를 수신할 수 있도록 하는 API. Pleos Connect 앱은 FusedLocation SDK를 통해 Pleos Connect의 **FusedLocation Service**와 연동합니다.

- 문서 기준: **FusedLocation SDK v1.1.3**
- 진입점: `FusedLocation` (패키지 `ai.pleos.playground.location.fused`)
- 리스너: `ai.pleos.playground.location.fused.listener.FusedLocationListener`, `ProximityAlertListener`
- 모델: `ai.pleos.playground.location.fused.model.LocationData` — 위도, 경도, 고도, 진행 방향, 신뢰도, 타임스탬프 포함 (필드명·타입은 Reference 404로 문서에 없음)

제공 기능:
| 기능 | API |
| --- | --- |
| 마지막으로 확인된 위치 1회 조회 | `getLastKnownLocation()` |
| 위치 업데이트 수신 | `registerFusedLocationCallback()` |
| 위치 업데이트 중지 | `unregisterFusedLocationCallback()` |
| 지원 환경 확인 | `isSupported()` |
| 위치 접근 차단 안내 팝업 | `showLocationAccessBlockedPopup()` |
| 설정 영역 진입/이탈 알림 등록 | `addProximityAlert()` |
| 설정 영역 진입/이탈 알림 해제 | `removeProximityAlert()` |

- `getLastKnownLocation()`은 새 위치를 계산하지 않고 현재 사용 가능한 마지막 위치를 반환 → 데이터가 없으면 콜백에 `null` 전달 가능.
- `addProximityAlert()` / `removeProximityAlert()`의 시그니처·사용법은 문서에 없습니다(`ProximityAlertListener` Reference도 404).

### 6.2 Gradle 설정
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/project-setup

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```
```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven {
            url "https://nexus-playground.pleos.ai/repository/maven-releases/"
        }
    }
}
```
```kotlin
// build.gradle.kts
dependencies {
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
}
```
```groovy
// build.gradle
dependencies {
    implementation "ai.pleos.playground:FusedLocation:1.1.3"
}
```

### 6.3 퍼미션
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/declaring-permissions

| 권한 | 보호 수준 | 사용 API |
| --- | --- | --- |
| `pleos.car.permission.FUSED_LOCATION` | `dangerous` | `getLastKnownLocation`, `registerFusedLocationCallback`, `unregisterFusedLocationCallback` |

```xml
<manifest ...>
    <uses-permission android:name="pleos.car.permission.FUSED_LOCATION" />
    <application ... />
</manifest>
```
- 보호 수준이 `dangerous`이므로 권한 요청과 결과 처리는 **앱의 권한 처리 흐름에 맞게 구현**하라고 안내합니다(런타임 요청 코드 예제는 문서에 없음).
- 권한 미허용 상태에서 호출 시 정상 동작하지 않을 수 있습니다.
- **위치 접근 차단 상태**: 차량 개인정보 보호 설정으로 위치 접근이 차단된 경우 `showLocationAccessBlockedPopup()`으로 사용자에게 안내할 수 있습니다. 이 API는 **접근을 허용해 주는 API가 아니며**, 위치가 전달되지 않을 때는 콜백의 `null` 및 `onError(Exception)`을 함께 처리하고 화면·기능을 제한해야 합니다.
- Android의 `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` 동작 및 포그라운드/백그라운드 위치 정책은 이 SDK 문서에서 보장하지 않으며, Google Fused Location SDK 설명을 Pleos Connect 동작 보장으로 해석하면 안 됩니다.

### 6.4 앱 등록 / API Key
FusedLocation 문서에는 앱 등록 요구 사항이 없습니다.

### 6.5 초기화 및 해제
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/initialization-and-release

애플리케이션 생명주기에 맞춰 초기화·해제합니다.

| 단계 | API | 설명 |
| --- | --- | --- |
| 지원 환경 확인 | `isSupported()` | 현재 환경이 SDK를 지원하는지 확인 |
| 초기화 | `initialize()` | 인스턴스 생성 후 초기화 |
| 위치 데이터 수신 | `getLastKnownLocation()`, `registerFusedLocationCallback()` | `initialize()` 후 조회/업데이트 수신, 권한 확인 필요 |
| 위치 업데이트 수신 중지 | `unregisterFusedLocationCallback()` | 등록한 리스너 해제 |
| 리소스 해제 | `release()` | SDK 리소스 해제 |

- `initialize()`를 호출했다면 반드시 `release()` 호출.
- `unregisterFusedLocationCallback()`(리스너 해제)와 `release()`(SDK 해제)는 **서로 대체 불가**.
- `release()` 전에 리스너를 먼저 해제.
- `release()` 후 같은 인스턴스로 API 호출 금지 → 새 인스턴스 생성 + `isSupported()` 확인 + 초기화.
- 지원되지 않는 환경에서는 FusedLocation 기능을 제공하지 않도록 처리.

```kotlin
class MyApplication : Application() {
    lateinit var fusedLocation: FusedLocation
        private set

    override fun onCreate() {
        super.onCreate()
        // 1. 애플리케이션 생성 단계에서 인스턴스를 생성합니다.
        fusedLocation = FusedLocation(this)
        // 2. 지원 여부를 확인한 후 SDK를 초기화합니다.
        if (fusedLocation.isSupported()) {
            fusedLocation.initialize()
        } else {
            // 위치 관련 UI와 기능을 비활성화합니다.
        }
    }

    fun shutdownFusedLocation(listener: FusedLocationListener) {
        // 3. 등록된 위치 업데이트 리스너를 먼저 해제합니다.
        fusedLocation.unregisterFusedLocationCallback(listener)
        // 4. SDK 사용이 끝나면 SDK 리소스를 해제합니다.
        fusedLocation.release()
    }
}
```

### 6.6 위치 데이터 접근 / 리스너
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access

- `getLastKnownLocation()`: 마지막 위치를 콜백으로 전달, `LocationData?`가 `null`일 수 있음 (호출 예제 코드는 문서에 없음).
- `registerFusedLocationCallback(FusedLocationListener)`: 위치 변경 시 `onChanged(LocationData?)`, 오류 시 `onError(Exception)`.

```kotlin
val listener = object : FusedLocationListener {
    override fun onChanged(location: LocationData?) {
        location ?: return
        // Use the location data.
    }
    override fun onError(exception: Exception) {
        // Handle the error.
    }
}
fusedLocation.registerFusedLocationCallback(listener)
```
```kotlin
fusedLocation.unregisterFusedLocationCallback(listener)
```
등록 시 사용한 동일 리스너로 해제합니다.

### 6.7 오류 처리
출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/error-handling

- 오류는 `FusedLocationListener.onError(Exception)`으로 전달. 별도의 오류 코드 enum은 문서에 없습니다.
- 앱은 예외를 기록하고, 위치 불가 상태를 안내하거나 기능을 제한해야 합니다.
- `onChanged(LocationData?)` 인자는 `null`일 수 있으므로 사용 전 null 확인, **`null`을 유효한 위치로 처리하지 말 것**.
- 위치 접근 차단 시 `showLocationAccessBlockedPopup()` 안내 가능.
- 예외의 구체 종류·재시도 가능 여부는 SDK가 전달한 예외와 실행 환경을 기준으로 판단.

### 6.8 API 개요 표

| 클래스 | 메서드 | 설명 | 퍼미션 |
| --- | --- | --- | --- |
| `FusedLocation` | `FusedLocation(context)` | 생성 | — |
| | `isSupported()` | 지원 환경 확인 | — |
| | `initialize()` | 초기화 | — |
| | `getLastKnownLocation(...)` | 마지막 위치 1회 조회 (콜백, nullable) | `FUSED_LOCATION` |
| | `registerFusedLocationCallback(FusedLocationListener)` | 위치 업데이트 수신 | `FUSED_LOCATION` |
| | `unregisterFusedLocationCallback(FusedLocationListener)` | 위치 업데이트 중지 | `FUSED_LOCATION` |
| | `showLocationAccessBlockedPopup()` | 차단 상태 안내 팝업 | 문서에 없음 |
| | `addProximityAlert(...)` / `removeProximityAlert(...)` | 영역 진입/이탈 알림 등록/해제 | 문서에 없음 |
| | `release()` | SDK 해제 | — |
| `FusedLocationListener` | `onChanged(location: LocationData?)`, `onError(exception: Exception)` | 위치 콜백 | — |
| `ProximityAlertListener` | 문서에 없음 | 근접 알림 콜백 | — |
| `LocationData` | 위도·경도·고도·진행 방향·신뢰도·타임스탬프 | 위치 결과 모델 | — |

---

## 7. LLM SDK

출처: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/LLM/entry

수집된 자료에는 LLM SDK의 **가이드 페이지(intro, importing, permissions, init 등)가 존재하지 않습니다.** `entry` 페이지는 제목(`# LLM`)만 있고, 클래스 Reference 페이지는 모두 404입니다. 따라서 Gradle 좌표, 버전, 퍼미션, 앱 등록, 초기화 방법, 코드 예제, 오류 코드는 **모두 문서에 없음**입니다.

404 페이지 URL에서 확인할 수 있는 패키지/클래스 이름만 기록합니다:

| 패키지 | 클래스 (URL 기반 추정 이름) | Reference URL |
| --- | --- | --- |
| `ai.pleos.playground.llm` | `LLM` | `/LLM/l-l-m/ai.pleos.playground.llm/l-l-m/` |
| `ai.pleos.playground.llm.data` | `LLMContent` | `/LLM/l-l-m/ai.pleos.playground.llm.data/l-l-m-content/` |
| `ai.pleos.playground.llm.data` | `Part` | `/LLM/l-l-m/ai.pleos.playground.llm.data/part/` |
| `ai.pleos.playground.llm.data` | `TextPart` | `/LLM/l-l-m/ai.pleos.playground.llm.data/text-part/` |
| `ai.pleos.playground.llm.data` | `ModelParameter` | `/LLM/l-l-m/ai.pleos.playground.llm.data/model-parameter/` |
| `ai.pleos.playground.llm.listener` | `ResultListener` | `/LLM/l-l-m/ai.pleos.playground.llm.listener/result-listener/` |
| `ai.pleos.playground.llm.listener` | `OnServerConnectionListener` | `/LLM/l-l-m/ai.pleos.playground.llm.listener/on-server-connection-listener/` |

> 리스너 이름(`OnServerConnectionListener`, `ResultListener`)이 STT/TTS와 동일하므로 서버 연결(앱 등록) → 결과 리스너 구조일 가능성이 있으나, 이는 **문서로 확인되지 않은 추정**입니다. 실제 사용 전 원문 문서 재확인이 필요합니다.

---

## 8. 문서 간 불일치·주의점 모음

| 위치 | 내용 |
| --- | --- |
| NaviHelper Java `RouteInfo` 생성자 | Destinations/Routes/Waypoints 예제는 (경도, 위도, ..., `RouteOption.RECOMMENDED`) 9개 인자, Route-with-waypoints 예제는 (위도, 경도, ...) 8개 인자 → 위치 인자 순서 상이 |
| NaviHelper Java `RequestWaypointInfo` | Waypoints 예제는 (127.09…, 37.41…) 경도 먼저, Route-with-waypoints는 (37.41…, 127.09…) 위도 먼저 |
| NaviHelper 경유지 콜백명 | Kotlin 예제 `onWaypointInfo` vs 이벤트 표·Java·본문 `onWayPointInfo`; `onWaypointChanged` vs `onWayPointArrived` 대소문자 혼재 |
| Vehicle 도메인명 | 인트로 표 `Steeringwheel`, Reference URL `steering-wheel`; 퍼미션 표에 Steeringwheel 없음 |
| Vehicle Callback Java 예제 | `onPositionUpdated(@Nullable int pos)` — primitive `int`에 `@Nullable` 표기 (원문 그대로) |
| TTS 인트로 사용 순서 7번 | "인식이 끝나면 `stop()`" — TTS 문맥상 합성/재생 종료를 의미 |
| STT/TTS `registerApp` | Kotlin named arg는 `clientID`(대문자 ID) |
| STT/TTS start 문서·오류 표 | "UA 인증", "UA 앱 정보" 언급 — 상세 설명 문서 없음 |
| 모든 SDK | API Reference(Dokka) 페이지 전부 404 → `@RequiresPermission` 등 상세 확인 불가 |
