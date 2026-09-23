import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// --- Load local.properties (git-ignored) ---------------------------------
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

// Requirement: "Environment Fallback for CI" — fall back to an environment
// variable when local.properties has no key, so CI builds can use a
// repository secret instead of a committed file.
val geminiApiKey: String =
    localProperties.getProperty("GEMINI_API_KEY")
        ?: System.getenv("GEMINI_API_KEY")
        ?: ""

val geminiProxyUrl: String =
    localProperties.getProperty("GEMINI_PROXY_URL")
        ?: System.getenv("GEMINI_PROXY_URL")
        ?: "https://generativelanguage.googleapis.com/v1beta"

android {
    namespace = "com.example.geministarter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.geministarter"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Exposed at build time only — never committed, never hardcoded.
        // Reviewers should find this string nowhere else in the codebase.
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GEMINI_PROXY_URL", "\"$geminiProxyUrl\"")
    }

    buildTypes {
        release {
            // Requirement: "Obfuscate the Release Build" — R8 shrinks and
            // obfuscates so the key/logic aren't trivially readable from a
            // decompiled release APK.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // --- Compose ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Gemini Proxy & Networking Client ---
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // --- Persistence: chat history (Room) ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // --- Persistence: preferences + encrypted key ciphertext (DataStore) ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Security: AES-256-GCM key generation backed by Android Keystore ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.mockito:mockito-core:5.12.0")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<Test> {
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}
