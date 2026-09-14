import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// =========================================================
// GEMINI API KEY
// =========================================================
// Reads the API key from local.properties.
// If it is not found there, it checks the environment
// variable GEMINI_API_KEY.
//
// NEVER hardcode the real API key in this file.
// =========================================================

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
            .orEmpty()
            .trim()


// =========================================================
// ANDROID CONFIGURATION
// =========================================================

android {

    namespace = "com.fahim.geminiApiComposeStarter"

    compileSdk {
        version = release(36)
    }

    defaultConfig {

        applicationId = "com.fahim.geminiApiComposeStarter"

        minSdk = 26

        targetSdk = 36

        versionCode = 1

        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // -------------------------------------------------
        // Expose API key through BuildConfig.
        // The actual key should remain in local.properties.
        // -------------------------------------------------

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


    // =====================================================
    // BUILD TYPES
    // =====================================================

    buildTypes {

        release {

            // R8 / code shrinking
            isMinifyEnabled = true

            // Remove unused resources
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }


    // =====================================================
    // JAVA / KOTLIN
    // =====================================================

    compileOptions {

        sourceCompatibility = JavaVersion.VERSION_17

        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {

        jvmTarget = "17"
    }


    // =====================================================
    // BUILD FEATURES
    // =====================================================

    buildFeatures {

        compose = true

        buildConfig = true
    }
}


// =========================================================
// DEPENDENCIES
// =========================================================

dependencies {

    // =======================================================
    // ANDROID CORE
    // =======================================================

    implementation(
        libs.androidx.core.ktx
    )


    // =======================================================
    // ANDROID SECURITY
    // =======================================================

    implementation(
        "androidx.security:security-crypto:1.1.0-alpha06"
    )


    // =======================================================
    // LIFECYCLE
    // =======================================================

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.9.3"
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )


    // =======================================================
    // ACTIVITY / COMPOSE
    // =======================================================

    implementation(
        libs.androidx.activity.compose
    )


    // Compose BOM
    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )


    // Compose UI
    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )


    // Material 3
    implementation(
        libs.androidx.compose.material3
    )


    // Material Icons
    implementation(
        libs.androidx.compose.material.icons.extended
    )


    // Responsive Window Size Class
    implementation(
        "androidx.compose.material3:material3-window-size-class"
    )


    // =======================================================
    // ROOM DATABASE
    // =======================================================

    implementation(
        libs.androidx.room.runtime
    )

    implementation(
        libs.androidx.room.ktx
    )

    ksp(
        libs.androidx.room.compiler
    )


    // =======================================================
    // DATASTORE
    // =======================================================

    implementation(
        libs.androidx.datastore.preferences
    )


    // =======================================================
    // GEMINI API
    // =======================================================

    implementation(
        libs.google.generativeai
    )


    // =======================================================
    // UNIT TESTING
    // =======================================================

    testImplementation(
        "junit:junit:4.13.2"
    )

    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2"
    )


    // =======================================================
    // ANDROID INSTRUMENTATION / COMPOSE UI TESTING
    // =======================================================

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )


    // =======================================================
    // DEBUG / COMPOSE PREVIEW
    // =======================================================

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}