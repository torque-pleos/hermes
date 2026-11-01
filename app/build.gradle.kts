import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zigae.hermes"
    // CarUxRestrictionsManager (주행 규제 상태)
    useLibrary("android.car")
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zigae.hermes"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // 비밀값은 hermes/secrets.properties (커밋 금지)에서 읽는다
        val secrets = Properties().apply {
            rootProject.file("secrets.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        buildConfigField("String", "KMA_SERVICE_KEY", "\"${secrets.getProperty("KMA_SERVICE_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            // 실제 RainViewer 데이터 검증: ./gradlew :app:testDebugUnitTest --tests '*Hindcast*' -Phindcast
            it.systemProperty("hermes.hindcast", project.hasProperty("hindcast").toString())
            it.systemProperty("hermes.cacheDir", layout.buildDirectory.dir("rainviewer-cache").get().asFile.path)
            it.outputs.upToDateWhen { !project.hasProperty("hindcast") }
            it.testLogging.showStandardStreams = project.hasProperty("hindcast")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Pleos Connect SDK (Playground에서 사용 가능한 것만)
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")

    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
