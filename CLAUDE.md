# Hermes — 개발 안내

화물 기사용 운행 점검 & 경로 강수 레이더 앱. 기획은 [README.md](README.md), Pleos 레퍼런스는 [docs/pleos/](docs/pleos/README.md).

## 빌드 · 실행

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -S -n com.zigae.hermes/.MainActivity
adb logcat -s Hermes/Vehicle Hermes/Navi Hermes/TTS
```

- 에뮬레이터는 반드시 `-no-audio`로 실행 ([docs/pleos/emulator-setup.md](docs/pleos/emulator-setup.md)).
- `local.properties`(sdk.dir)는 커밋하지 않는다.
- 구성: AGP 8.13.2 · Gradle 8.14.3 · Kotlin 2.2.21 · Compose · compileSdk 36 / targetSdk 34 / minSdk 28.

## 구조

| 경로 | 역할 |
| --- | --- |
| `app/.../vehicle/VehicleMonitor.kt` | Vehicle State 조회 (와이퍼 상태·스위치, 기어, 도어 잠금). 연결 콜백이 없어 2초 주기 조회 + 와이퍼 상태 리스너 |
| `app/.../vehicle/WiperRain.kt` | 와이퍼 스위치 단계 → 추정 강수 강도 (F4 입력) |
| `app/.../navi/NaviMonitor.kt` | NaviHelper: 현재 위치, 경로 상태, 목적지·경유지 남은 거리/시간, 테스트 경로 요청 |
| `app/.../voice/Speaker.kt` | Android 표준 TTS 시도용 래퍼 (에뮬레이터에서는 사용 불가로 확인됨) |
| `app/.../ui/DiagnosticsScreen.kt` | 개발용 SDK 진단 화면 |

## 에뮬레이터 검증 결과 (2026-11-01)

| 항목 | 결과 |
| --- | --- |
| Pleos 권한 (`CAR_*`, `NAVI_*`) | 보호 수준 `normal` → 매니페스트 선언만으로 허용, 런타임 요청 불필요 |
| 와이퍼 상태 리스너 | ✅ Virtual Console 변경 시 ON/OFF 이벤트 실시간 수신 |
| 와이퍼 스위치 | ✅ Virtual Console `Low`→`CONTINUOUS_LEVEL_1`, `High`→`CONTINUOUS_LEVEL_3`, `Auto`→`AUTO`, `Off`→`OFF`. 간헐(INTERMITTENT) 단계는 에뮬레이터에서 재현 불가 |
| 기어 / 도어 잠금 | ✅ `GEAR_PARK`, 도어 영역 `HOOD`, `ROW_1_LEFT+ROW_1_RIGHT+ROW_2_LEFT+ROW_2_RIGHT` |
| NaviHelper 현재 위치 | ✅ 37.41272, 127.09468 (수정구 시흥동) |
| 경유지 남은 거리·시간 | ✅ `requestRouteWithWaypoints` 후 `WaypointInfo.remainDistance`(m), `remainTime`(초) 수신. 예: 대전 141,000m / 6,420s, 부산신항 398,000m / 17,100s |
| Android 표준 TTS | ❌ 기본 엔진 미설정 + `ai.pleos.playground.caas` 지정 시에도 초기화 실패(code -1, "not bound to TTS engine") → 음성 안내 미사용 |
| `VehicleException.errorCode` | 배포 AAR에서 난독화되어 참조 불가 → 예외 메시지만 사용 |

## Virtual Console로 차량 값 바꾸기

```bash
adb forward tcp:8088 tcp:8088
curl -X PUT "http://127.0.0.1:8088/vehicle/body?front_wiper=High"   # Off | Auto | Low | High
curl -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"         # Park | Reverse | Neutral | Drive
curl -s http://127.0.0.1:8088/vehicle/state                          # 현재 상태
```
