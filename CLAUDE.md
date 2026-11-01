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
| `app/.../radar/` | F2 강수 레이더 엔진. `WebMercator`·`RadarFrame`·`MotionEstimator`·`Nowcaster`·`RadarMosaic`·`RouteRain`은 순수 Kotlin(JVM 단위 테스트), `RainViewerClient`·`RadarRepository`가 안드로이드 연동 |
| `app/.../ui/DiagnosticsScreen.kt` | 개발용 진단 화면 (차량 · 내비 · 강수 레이더) |

단위 테스트: `./gradlew :app:testDebugUnitTest`

## 강수 레이더 엔진 (F2)

1. 현재 위치·경유지·목적지를 덮는 z7 타일 범위 + 여유 150px(90분 × 100km/h), 최대 16타일
2. RainViewer 최근 프레임 4개(10분 간격) 병렬 다운로드(동시 8개), 타일 캐시
3. 색 → dBZ 역변환 → 모자이크 → 블록 정합(거친 4배 축소 → 원 해상도)으로 전역 이동 벡터 추정
4. 최신 프레임을 이동 벡터로 외삽(최대 90분), 외삽 시간이 길수록 탐색 반경 확대
5. 강도: Marshall–Palmer(Z=200R^1.6), 기상청 구분(약 <3, 보통 <15, 강 <30, 매우 강 ≥30 mm/h), 15dBZ 미만은 비 없음
6. 90분을 넘는 지점은 "예측 범위 밖 + 지금 그곳 관측값"을 보여준다

⚠️ RainViewer 타일은 **색상표 파라미터와 무관하게 항상 Universal Blue**로 온다(색상표 0·2 응답이 바이트 단위로 동일, 2026-11-01 확인). `RainViewerPalette`가 공식 색상표 CSV로 색 → dBZ를 역변환한다. 흑백(dBZ) 색상표를 가정하면 값이 전부 틀린다.

## 에뮬레이터 검증 결과 (2026-11-01)

| 항목 | 결과 |
| --- | --- |
| Pleos 권한 (`CAR_*`, `NAVI_*`) | 보호 수준 `normal` → 매니페스트 선언만으로 허용, 런타임 요청 불필요 |
| 와이퍼 상태 리스너 | ✅ Virtual Console 변경 시 ON/OFF 이벤트 실시간 수신 |
| 와이퍼 스위치 | ✅ Virtual Console `Low`→`CONTINUOUS_LEVEL_1`, `High`→`CONTINUOUS_LEVEL_3`, `Auto`→`AUTO`, `Off`→`OFF`. 간헐(INTERMITTENT) 단계는 에뮬레이터에서 재현 불가 |
| 기어 / 도어 잠금 | ✅ `GEAR_PARK`, 도어 영역 `HOOD`, `ROW_1_LEFT+ROW_1_RIGHT+ROW_2_LEFT+ROW_2_RIGHT` |
| NaviHelper 현재 위치 | ✅ 37.41272, 127.09468 (수정구 시흥동) |
| 경유지 좌표 | ⚠️ `WaypointInfo.latitude/longitude`가 **0.0**으로 옴(이름·거리·시간은 정상). 앱이 요청한 경유지는 요청 좌표로 보정, 그 외 좌표 없는 지점은 예측 제외 |
| 목적지 좌표 | ✅ `DestinationInfo`는 정상 좌표 |
| `requestRouteWithWaypoints` | ✅ 내비 앱이 전면에 뜨고 실제 경로 안내 시작 |
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
