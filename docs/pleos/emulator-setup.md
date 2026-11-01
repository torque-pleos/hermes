# Pleos Connect 에뮬레이터 설치 기록 (macOS Apple Silicon)

설치일: 2026-11-01 · 환경: macOS 26.6.2, Apple M3 Max, 36GB RAM

## 설치된 구성

| 항목 | 값 |
| --- | --- |
| Android Studio | Homebrew cask `android-studio` (2026.2.2) |
| JDK (CLI용) | Homebrew `openjdk@17` → `JAVA_HOME=/opt/homebrew/opt/openjdk@17` |
| Android SDK | `~/Library/Android/sdk` (cmdline-tools latest, platform-tools, emulator 37.2.13, platforms;android-34, build-tools;34.0.0) |
| 시스템 이미지 | `system-images;android-34;pleos_car_emulator;arm64-v8a` (Pleos Connect v2.1.0, Android 14) |
| AVD | `Pleos_Connect_v2.1.0` — 2560×1440, 160dpi, Cold boot, RAM 8GB |

> Android Studio 번들 JBR(`/Applications/Android Studio.app/Contents/jbr`)은 첫 실행 시 Gatekeeper 검사로 CLI에서 멈춰서 별도 JDK 17 사용.

## 시스템 이미지 수동 설치 방법 (Android Studio GUI 대신)

1. 매니페스트: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
2. 이미지 zip(4.7GB, SHA-1 `24d28eb2f18eba5e87c861a7eb2bdccebb049419`):
   `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp_car_arm64_emulator.zip`
3. 압축 해제(약 20GB) → `~/Library/Android/sdk/system-images/android-34/pleos_car_emulator/arm64-v8a/` 에 배치 + `package.xml` 작성
4. 이미지의 `devices.xml`(디바이스 `Pleos_Connect_v2_0`)을 `~/.android/devices.xml`로 복사
5. `avdmanager create avd -n Pleos_Connect_v2.1.0 -k "system-images;android-34;pleos_car_emulator;arm64-v8a" -d Pleos_Connect_v2_0`
6. `config.ini`: `fastboot.forceColdBoot=yes`, `hw.gpu.mode=host`, `hw.ramSize=8192`

## 실행 ⚠️ `-no-audio` 필수

```bash
~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2.1.0 -no-snapshot -no-boot-anim -no-metrics -no-audio
```

- **오디오를 켜면 부팅 완료 직후(약 10초 내) QEMU 메인 루프가 멈추고 종료됨** (emulator 36.1 / 36.4 / 37.2 모두 동일, GPU 모드·RAM 무관).
  - 원인 추정: 부팅 후 음성 비서가 마이크 입력을 열면서 호스트 오디오(마이크 권한) 처리에서 블로킹.
  - 영향: 앱 소리(TTS 재생) 확인 불가. 소리 확인이 필요하면 macOS 설정 > 개인정보 보호 > 마이크에서 에뮬레이터(qemu) 권한을 허용한 뒤 오디오를 켜고 재시도.
- 크래시 후 재실행 시 "crash report" 동의 대화상자가 떠서 부팅이 멈출 수 있음 → `rm -rf /tmp/android-$USER/emu-crash-*.db` 후 재실행.
- Android Studio Device Manager에서 실행할 경우에도 AVD 설정의 오디오를 끄거나 위 명령으로 실행.

## Virtual Console

```bash
adb forward tcp:8088 tcp:8088
open http://127.0.0.1:8088/
```

## 에뮬레이터에서 확인한 사실 (2026-11-01)

| 항목 | 결과 |
| --- | --- |
| Android 버전 | 14 (`Pleos/crp_avd_arm64:14/RELEASE.release_connect_v2.1.0`) |
| Android 표준 TTS 엔진 | **존재** — `ai.pleos.playground.caas/.service.speech.SpeechService`가 `android.intent.action.TTS_SERVICE` 제공 (`tts_default_synth`는 미설정). **단, 3rd party 앱에서는 사용 불가** — `TextToSpeech` 기본 생성자·엔진 명시(`ai.pleos.playground.caas`) 모두 초기화 실패(code -1, "not bound to TTS engine"). Hermes 진단 화면으로 확인 |
| LLM 서비스 | `ai.pleos.playground.caas/.service.llm.LLMService` 존재 (SDK는 Playground 메뉴에 없음) |
| 위치 | 위치 서비스 ON, `gps`/`fused`/`passive` 프로바이더 존재 |
| 내비 | `ai.umos.maps.android.navigation.app`, `ai.pleos.playground.service.navi` (NaviHelper 백엔드) |
| Gleo | `ai.pleos.gleoai.model.provider` |
| Vehicle | `ai.pleos.playground.service.vehicle`, `ai.pleos.hardware.vehicle.emulator` |
| 지도 화면 | 첫 실행 시 지도 타일이 비어 있음 (네트워크/설정 확인 필요) |
