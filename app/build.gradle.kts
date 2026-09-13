import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Read Gemini API key from local.properties.
// If unavailable locally, fall back to environment variable for CI.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")

    if (file.exists()) {
        file.inputStream().use {
            load(it)
        }
    }
}

val geminiApiKey: String =
    localProperties
        .getProperty("GEMINI_API_KEY")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: System.getenv("GEMINI_API_KEY")
            ?.trim()
            .orEmpty()

android {

    namespace = "com.fahim.geminiApiComposeStarter"

    compileSdk {
        version = release(36)
    }

    defaultConfig {

        applicationId =
            "com.fahim.geminiApiComposeStarter"

        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"" +
                    geminiApiKey
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"") +
                    "\""
        )
    }

    buildTypes {

        release {

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    buildFeatures {

        compose = true
        buildConfig = true
    }
}

dependencies {

    // Core Android
    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )

    // Preferences DataStore
    implementation(
        "androidx.datastore:datastore-preferences:1.2.1"
    )

    // Room
    implementation(
        libs.androidx.room.runtime
    )

    implementation(
        libs.androidx.room.ktx
    )

    ksp(
        libs.androidx.room.compiler
    )

    // Compose BOM
    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    // Compose
    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.compose.material3
    )

    // Responsive WindowSizeClass
    implementation(
        libs.androidx.material3.window.size
    )

    // Gemini
    implementation(
        libs.google.generativeai
    )

    // Unit tests
    testImplementation(
        libs.junit
    )

    testImplementation(
        libs.kotlinx.coroutines.test
    )

    // Android / Compose UI tests
    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.junit
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    // IMPORTANT: Compose UI testing library
    androidTestImplementation(
        "androidx.compose.ui:ui-test-junit4"
    )

    // Compose tooling
    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    // Required by createComposeRule()
    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
}