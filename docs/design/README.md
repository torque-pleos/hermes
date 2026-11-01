# Hermes 디자인

## 원칙 (Pleos Connect UI Design Guide 기준)

| Pleos 원칙 | Hermes 적용 |
| --- | --- |
| Glanceability 1~2초 | 주행 화면 3줄: 다음 정차·도착 시각 / 가는 길 최대 강수(큰 색 블록)·시각·근거 / 이후 구간 |
| 대비 4.5:1 이상 | 주간·야간 토큰 모두 본문 텍스트를 배경 대비 4.5:1 이상으로 지정 |
| 중요 정보 텍스트 5mm 이상 | 주행 화면 핵심 44sp, 보조도 24sp 이상. 홈 본문 20sp, 캡션 18sp |
| 자동 스크롤·애니메이션 금지 | 주행 화면에 스크롤·애니메이션·입력 없음 |
| Landscape·Portrait 모두 지원 | 앱 폭 1000dp 초과 시 2열(브리핑 62% · 근거 38%), 이하(1:1 분할 창 등) 1열 스크롤 |
| 주간/야간 모드 | 시스템 야간 모드를 따라 `IviColors` 두 벌 전환 |
| 아이콘 + 텍스트 라벨 | 강수 색 블록 옆에 항상 단계 텍스트, 범례 표시. "예보 없음"은 색이 아니라 빗금 무늬로 구분 |
| 디자인 토큰 `ivi_` 접두·snake case | `ui/theme/HermesTheme.kt`의 `IviColors`(예: `ivi_surface_card`, `ivi_rain_heavy`), `IviType`, `IviSpace` |
| 큰 터치 영역 | 버튼 최소 높이 64dp |

## 화면

- **운행 브리핑(주차 중 홈)**: 결론 배너(가장 강한 비의 시각·구간·근거 배지) → 경로 강수 띠(지금~도착 시간축, 60분 경계 점선, 정차지 눈금) → 정차지 목록(도착 시각·도착 시 강수·가는 길 최대 강수) / 근거 카드(레이더 이동 방향·속도, 주변 Hermes 차량, 내 와이퍼) / 출처 표기
- **주행 중**: `CarUxRestrictions` DO 필요 시 자동 전환. 진단 화면의 "주행 화면 미리보기"로 주차 중 확인 가능(화면 탭으로 닫기)
- **진단**: 개발·시연용 SDK 상태 화면(헤더의 "진단" 버튼)

## 강수 색

없음(초록 계열) → 약한 비(파랑) → 보통 비(노랑) → 강한 비(주황) → 매우 강한 비(빨강). 기상청 강도 구분과 같은 4단계 + 없음. 예보 없음은 빗금.

## 레퍼런스 (Figma Community, 2026-11-01 열람)

에셋을 가져오지 않고 배치·스타일 방향만 참고했다.

| 파일 | 라이선스 | 참고한 점 |
| --- | --- | --- |
| [Android Auto Design System](https://www.figma.com/community/file/1441273997313085146/android-auto-design-system) (Harsai) | CC BY 4.0 | 어두운 면 위 둥근 목록 카드, 제목 + 색 강조 보조 정보(시간·거리) |
| [High-fidelity Car Infotainment UI](https://www.figma.com/community/file/1677699486578294598/high-fidelity-car-infotainment-ui) (Edwin García) | Figma Make 공개 파일 | 남색 바탕 테두리 카드, 상단 상태 배너 + 배지, 모노스페이스 대문자 섹션 라벨, 큰 숫자 + 작은 단위 |
| [다시, 여기 — Pleos 차량용 앱 UX](https://www.figma.com/community/file/1697540292233650865/pleos-ux) | CC BY 4.0 | 번호가 붙은 정차지 타임라인 목록 |

공식 리소스: Pleos Connect display guide / design assets (`Pleos_Connect_design_assets_215.fig`, document.pleos.ai) — 컬러·타이포 세부 수치는 이 파일로 대조 필요(미확인).
