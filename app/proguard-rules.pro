# Hermes R8 규칙
# Pleos SDK(navi-data, vehicle, vehicle-data 등)는 AAR에 consumer 규칙을 포함한다. 여기에는 부족한 것만 추가한다.

# Vehicle SDK가 내부에서 Gson TypeToken(익명 하위 클래스)으로 응답 리스트를 해석한다.
# R8이 제네릭 시그니처를 지우면 "TypeToken must be created with a type argument"로
# 도어 잠금 capability 조회가 실패한다(2026-11-02 릴리스 빌드에서 확인).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
# Gson이 리플렉션으로 채우는 Vehicle 데이터 모델
-keep class ai.pleos.playground.vehicle.data.** { *; }
-keep class ai.pleos.playground.vehicle.api.model.** { *; }
-keep class ai.pleos.playground.vehicle.constant.** { *; }
