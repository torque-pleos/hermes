# Hermes — 개발 안내

화물 기사용 운행 점검 & 경로 강수 레이더 앱. 기획은 [README.md](README.md), Pleos 레퍼런스는 [docs/pleos/](docs/pleos/README.md).

## 개발 원칙

- **공모전 제출용 앱이다. 상용 수준으로 만들지 않는다.** 서버 프록시, 키 보호, 상용 API 교체, 대규모 트래픽 대비는 하지 않고 필요하면 "상용 시" 메모만 남긴다.
- 우선순위: 데모 시연 안정성 → Pleos 앱 심사 요건 → 평가 항목(차량특화성·창의성·기술 활용성·사업성)에 드러나는 기능과 근거 자료.

## 빌드 · 실행

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -S -n com.zigae.hermes/.MainActivity
adb logcat -s Hermes/Vehicle Hermes/Navi Hermes/Radar Hermes/KMA Hermes/Fleet
```

- 에뮬레이터는 반드시 `-no-audio`로 실행 ([docs/pleos/emulator-setup.md](docs/pleos/emulator-setup.md)).
- `local.properties`(sdk.dir)는 커밋하지 않는다.
- 기상청 키: `secrets.properties.example` → `secrets.properties`로 복사해 `KMA_SERVICE_KEY` 입력(커밋 금지). 없으면 레이더만 사용.
  공공데이터포털 개발계정(기상청_단기예보 조회서비스, 2026-11-01 승인, 24개월, 기능별 일 10,000건). 새 포털은 일반 인증키 하나(64자 hex)만 제공.
  이용허락범위가 **저작자표시**이므로 화면·제출 자료에 출처 "기상청"을 표기한다.
- 차량 간 공유 서버: `secrets.properties`의 `FLEET_URL`·`FLEET_KEY`(publishable 키), `SUPABASE_DB_PASSWORD`(CLI `db push`용). 없으면 공유 기능 꺼짐.
- 구성: AGP 8.13.2 · Gradle 8.14.3 · Kotlin 2.2.21 · Compose · compileSdk 36 / targetSdk 34 / minSdk 28.

## 구조

| 경로 | 역할 |
| --- | --- |
| `app/.../vehicle/VehicleMonitor.kt` | Vehicle State 조회 (와이퍼 상태·스위치, 기어, 도어 잠금). 연결 콜백이 없어 2초 주기 조회 + 와이퍼 상태 리스너 |
| `app/.../vehicle/WiperRain.kt` | 와이퍼 스위치 단계 → 추정 강수 강도 (F4 입력) |
| `app/.../navi/NaviMonitor.kt` | NaviHelper: 현재 위치, 경로 상태, 목적지·경유지 남은 거리/시간, 테스트 경로 요청 |
| `app/.../radar/` | F2 강수 레이더 엔진. `WebMercator`·`RadarFrame`·`MotionEstimator`(전역 벡터 + 블록 벡터장)·`Nowcaster`·`RadarMosaic`(`RouteTiles`)·`RouteRain`(구간 표본)은 순수 Kotlin(JVM 단위 테스트), `RainViewerClient`·`RadarRepository`가 안드로이드 연동 |
| `app/.../weather/` | 기상청 초단기·단기예보: `KmaGrid`(격자 변환)·`KmaForecast`(파서·발표시각)·`BlendedForecaster`(60분 이내 레이더, 이후 기상청)·`KmaRepository`(격자 캐시·재시도 제한) |
| `app/.../geo/` | `GeoMath`(거리·대권 보간), `CandidatePicker`(남은 거리로 지오코딩 후보 선택), `WaypointLocator`(Nominatim 지오코딩 + 기기 저장) |
| `app/.../fleet/` | 차량 간 와이퍼 공유: `FleetFusion`(주변 관측을 비구름 이동으로 도착 시각까지 옮겨 합산, 순수 Kotlin) · `FleetForecaster`(레이더+기상청 예측 위에 덧입힘) · `FleetRepository`(Supabase REST 업로드·조회·시연 RPC, 동의·익명 ID) |
| `supabase/` | Supabase 프로젝트 `hermes`(ref `kayyybsxqnvbsimbearq`, 서울, 개인 조직 zigae) 설정과 스키마(`migrations/0001_wiper_reports.sql`). 적용: `supabase link --project-ref kayyybsxqnvbsimbearq` → `supabase db push` |
| `app/.../vehicle/DrivingState.kt` | `CarUxRestrictionsManager`로 주행 중(DO 필요) 판단 |
| `app/.../ui/DriveScreen.kt` | 주행 중 화면: 다음 정차지·도착 시각, 가는 길 최대 강수(색 블록)·시각, 이후 구간 한 줄 |
| `app/.../ui/DiagnosticsScreen.kt` | 개발용 진단 화면 (차량 · 내비 · 강수 레이더) |

단위 테스트: `./gradlew :app:testDebugUnitTest`

## 강수 레이더 엔진 (F2)

1. 현재 위치·경유지·목적지를 덮는 z7 타일 범위 + 여유 150px(90분 × 100km/h), 최대 16타일
2. RainViewer 최근 프레임 4개(10분 간격) 병렬 다운로드(동시 8개), 타일 캐시
3. 색 → dBZ 역변환 → 모자이크 → 블록 정합(거친 4배 축소 → 원 해상도)으로 전역 이동 벡터 추정
4. 최신 프레임을 이동 벡터로 외삽(최대 90분), 외삽 시간이 길수록 탐색 반경 확대
5. 강도: Marshall–Palmer(Z=200R^1.6), 기상청 구분(약 <3, 보통 <15, 강 <30, 매우 강 ≥30 mm/h), 15dBZ 미만은 비 없음
6. 90분을 넘는 지점은 "예측 범위 밖 + 지금 그곳 관측값"을 보여준다
7. 블록(128px) 벡터장으로 지역별 이동을 보정하고 벡터장을 따라 10분 단위로 역추적한다
8. 타일은 경로 표본마다 "도착까지 비구름이 이동해 올 거리"만큼만 받는다(최대 24). 받지 않은 타일은 NO_DATA(NaN)로 맑음과 구분
9. 경로 강수: 지금부터 60분 이내는 레이더, 이후는 기상청 예보 1시간 칸(`BlendedForecaster`)
10. 구간: NaviHelper가 경로 좌표열을 주지 않아 현재 위치→경유지→목적지를 대권으로 잇고 15km 간격 표본, 도착 시각은 거리 비율로 보간(직선 근사)

⚠️ RainViewer 타일은 **색상표 파라미터와 무관하게 항상 Universal Blue**로 온다(색상표 0·2 응답이 바이트 단위로 동일, 2026-11-01 확인). `RainViewerPalette`가 공식 색상표 CSV로 색 → dBZ를 역변환한다. 흑백(dBZ) 색상표를 가정하면 값이 전부 틀린다.

## 차량 간 와이퍼 공유 (F4 확장)

1. 공유 동의한 차량만 1분마다 + 와이퍼 단계 변경 시 관측 업로드: 무작위 차량 ID, 좌표 약 500m 반올림, 와이퍼→강도(AUTO는 올리지 않음)
2. 경로 표본 범위 ±0.5°의 최근 20분 관측 조회. 내 차는 서버를 거치지 않고 로컬 관측을 바로 쓴다
3. 관측 위치를 비구름 이동 속도(레이더 벡터장)로 도착 시각까지 옮겨 반경 8km 안이면 근거로 센다(지금부터 60분 이내 지점만, 차량별 최신 1건)
4. 비를 본 차량 ≥2 → 예보와 "두 번째로 강한 차량 관측" 중 큰 값 / 맑음 ≥3·비 0·예보 약한 비 이하 → 비 없음
5. RLS: anon은 지금 시각(±2분)의 실제 관측만 삽입, 최근 30분만 조회, 수정·삭제 불가. 시연 데이터는 `seed_demo_fleet` RPC로만(최대 20대, 10분 지난 시연 데이터 자동 정리)
6. 시연 버튼: 경로상 20~40분 뒤 지날 표본의 상류(비구름이 오는 쪽)에 강한 비를 보는 가상 차량 5대 생성

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
| 주행 규제 | ✅ DO 미선언 시 기어 D·주행 중 "주행 중에는 사용할 수 없습니다"로 차단(프로세스는 계속 동작). `distractionOptimized` 선언 후 주행 중 표시됨 (`UXR: DO: true UxR: 80`) |
| 주행 중 기어 | ✅ `GEAR_DRIVE` 수신 |
| 주행 중 위치·남은 거리 | ❌ 재현 불가 — Virtual Console로 속도를 줘도 내비 위치가 움직이지 않음(남은 거리 고정) |
| `RouteStateInfo.isRouting` | ⚠️ 내비가 실제로 안내 중인데도 `false` → 안내 여부 판단에 쓰지 말고 목적지/경유지 존재로 판단 |
| 레이더 오프라인 | ✅ 실패 시 "네트워크 연결 없음 · 1분 후 다시 시도" 표시, 직전 예측 유지, 1분 간격 재시도, 복구 후 자동 재개 |
| 레이더 장시간 (12분) | ✅ PSS 87~90MB 안정(갱신 순간 최대 107MB), 5분 주기 갱신으로 새 관측 프레임 반영 |
| 레이더 첫 로딩 | ✅ 약 20초 (9~16타일 × 4프레임 병렬, 첫 로딩 요청 최대 65건 → 100 req/min 제한 이내. 이후 새 프레임마다 타일 수 + 1건) |
| 예측 정확도 | ✅ [hindcast 보고서](docs/verification/hindcast-2026-11-01-1240Z.md): 모든 리드에서 이동벡터 외삽 > 지속성 (약한 비 30분 CSI 0.690 vs 0.665) |
| `requestTrip` / `TripInfo` | ⚠️ 진행 중 경로가 아니라 **현재 위치→목적지를 경유지 없이 새로 계산**한 경로. `routePoints`는 목적지 1개뿐, 구간 혼잡도는 4% 단위 → 경유지 좌표·경로 좌표열 용도로 못 씀 |
| 경유지 좌표 보완 | ✅ 내비 주소 "서구 둔산동"을 Nominatim으로 검색, 후보 2개 중 남은 거리(141km)와 맞는 대전 둔산동 선택, 재실행 시 저장값 사용(재검색 0건) |
| 경로 구간 예측 | ✅ 서울→대전 10표본, 대전→부산 14표본. 구간별 최대 강수·시각 표시 |
| 경로 타일 | ✅ 서울→대전→부산 4타일(직사각형 방식 9타일). 경로 없을 때 1타일 |
| 기상청 API | ✅ 개발계정 키(자동승인, 즉시 사용 가능)로 서울→대전→부산 경로 격자 23개 초단기(6h)·단기(82h) 모두 수신, 오류 0. 격자 변환(서울 60,127 / 부산 98,76 / 제주 53,38), 잘못된 키 응답(HTTP 403, 코드 30) 처리, 실패 격자 재시도 제한도 확인 |
| 기상청 응답 특이사항 | 초단기예보에도 POP(강수확률)가 포함됨. 초단기는 다음 정시부터 6시간이라 현재 시각이 속한 시간은 단기예보로 채움(`mergeKma`) |
| 주행 화면 전환 | ✅ 기어 D·주행 → `requiresDO=true`(UxR 80) → 운행 화면, 정차·P → 진단 화면 |
| 예측 방식 비교 | ✅ [8개 구성 hindcast](docs/verification/hindcast-2026-11-01-1330Z-configs.md): 벡터장 ≥ 전역, 반경은 CSI↔POD 교환 → F r2+/15 채택 |
| 차량 공유 서버 | ✅ 실제 관측 삽입 201, 시연 플래그·과거 시각 삽입은 RLS 거부(42501), 삭제 0건, 시연 RPC 정상 |
| 차량 공유 앱 | ✅ 동의 후 1분 주기 업로드(14:23:51, 14:24:51, 좌표 37.415/127.095로 반올림), 주변 관측 조회, 시연 버튼 → 서울→대전 구간 "차량 보정 강한 비 ← 비 없음 · 주변 차량 3대(비 3) · 22분 뒤" |
| 2번째 Pleos 에뮬레이터 동시 실행 | ⚠️ 다른 작업이 `emulator-5556`을 띄우자 `emulator-5554`의 Virtual Console이 `emulator_inactive`(와이퍼·기어 변경 불가). adb는 `ANDROID_SERIAL=emulator-5554`로 지정 |
| 에뮬레이터 Wi-Fi | ⚠️ 비행기 모드 해제 후 자동 재연결 안 됨 → 설정 > 연결 > Wi-Fi에서 AndroidWifi 탭 (adb `cmd wifi connect-network`는 권한 없음) |

### 검증하지 못한 항목

| 항목 | 이유 | 확인 방법 |
| --- | --- | --- |
| 실차에서 경유지 좌표가 오는지 | 에뮬레이터는 0.0 반환 | 실차/다른 플랫폼 버전에서 `WaypointInfo` 로그 확인 |
| 주행 중 남은 시간 갱신 → 예측 시각 이동 | 에뮬레이터 위치가 움직이지 않음 | 실차 주행 또는 내비 모의주행 기능 확인 |
| 간헐 와이퍼 1~5단 | Virtual Console이 Off/Auto/Low/High만 제공 | 실차 |
| 예측 정확도 일반화 | RainViewer가 과거 2시간만 제공 → 2회 실행 모두 같은 기상 상황(층운형, 동진 약 60km/h). 구성 간 차이 0.01 수준은 유의하지 않음 | 날씨가 다른 날 hindcast 반복 실행해 보고서 누적 |
| 기상청 예보 정확도 | 예보값을 받아 표시하는 것까지만 확인. 예보가 실제 강수와 맞았는지는 비교하지 않음(기상청 공식 예보라 공모전 범위에서는 검증 대상 아님) | — |
| 구간 직선 근사의 오차 | 실제 도로 좌표를 얻을 방법이 없음 | 실차 주행 로그(현재 위치 이력)와 직선 표본 비교 |
| 지오코딩 일반화 | 에뮬레이터 경유지 1건("서구 둔산동")만 확인. 동명 주소가 남은 거리로도 구분 안 되는 경우 가능 | 실차에서 다양한 경유지로 확인. 상용은 VWorld/카카오 로컬 API로 교체 |
| 실차 Android TTS | 에뮬레이터만 확인 | 실차 (단, 기획은 TTS 없이 확정) |
| 와이퍼 변경 즉시 업로드, 주행 화면의 "주변 차량 N대가 비 관측" 표시 | 두 번째 에뮬레이터 때문에 Virtual Console 비활성(와이퍼·기어 변경 불가) | 다른 에뮬레이터 종료 후 Virtual Console로 와이퍼 High·기어 D |
| 차량 공유 판단 규칙의 실효성 | 실제 여러 차량 데이터 없음(시연은 가상 차량) | 공모전 범위에서는 규칙·시연으로 설명 |
| DO 앱 심사 통과 여부 | 주행 화면은 만들었으나 심사 기준과 항목별 대조 안 함, 알림음·오디오 포커스 미구현 | 심사 체크리스트(`docs/pleos/04-test-and-publish.md`) 대조 |

## Virtual Console로 차량 값 바꾸기

```bash
adb forward tcp:8088 tcp:8088
curl -X PUT "http://127.0.0.1:8088/vehicle/body?front_wiper=High"   # Off | Auto | Low | High
curl -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"         # Park | Reverse | Neutral | Drive
curl -s http://127.0.0.1:8088/vehicle/state                          # 현재 상태
```
