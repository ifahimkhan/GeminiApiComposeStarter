import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ==========================================
// LOAD LOCAL PROPERTIES
// ==========================================

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")

    if (file.exists()) {
        file.inputStream().use {
            load(it)
        }
    }
}

val geminiApiKey = localProperties
    .getProperty("GEMINI_API_KEY")
    ?.trim()
    .orEmpty()


// ==========================================
// ANDROID CONFIGURATION
// ==========================================

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

        // Gemini API key from local.properties
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


    // ==========================================
    // BUILD TYPES - R8 CONFIGURATION
    // ==========================================

    buildTypes {

        // DEBUG BUILD
        debug {

            // Disable shrinking during development
            isMinifyEnabled = false

            isShrinkResources = false
        }


        // RELEASE BUILD
        release {

            // Enable R8 code shrinking
            isMinifyEnabled = true

            // Enable resource shrinking
            isShrinkResources = true

            // ProGuard / R8 configuration
            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }


    // ==========================================
    // JAVA CONFIGURATION
    // ==========================================

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }


    // ==========================================
    // BUILD FEATURES
    // ==========================================

    buildFeatures {

        compose = true

        buildConfig = true
    }
}


// ==========================================
// DEPENDENCIES
// ==========================================

dependencies {

    // ------------------------------------------
    // ANDROID
    // ------------------------------------------

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.activity.compose
    )


    // ------------------------------------------
    // JETPACK COMPOSE
    // ------------------------------------------

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

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

    implementation(
        libs.androidx.compose.window.size
    )


    // ------------------------------------------
    // VIEWMODEL
    // ------------------------------------------

    implementation(
        libs.androidx.lifecycle.viewmodel.compose
    )


    // ------------------------------------------
    // GEMINI API
    // ------------------------------------------

    implementation(
        libs.google.generativeai
    )


    // ------------------------------------------
    // DATASTORE PREFERENCES
    // ------------------------------------------

    implementation(
        "androidx.datastore:datastore-preferences:1.2.1"
    )


    // ------------------------------------------
    // ROOM DATABASE
    // ------------------------------------------

    implementation(
        "androidx.room:room-runtime:2.8.5"
    )

    ksp(
        "androidx.room:room-compiler:2.8.5"
    )


    // ------------------------------------------
    // UNIT TESTING
    // ------------------------------------------

    testImplementation(
        libs.junit
    )

    testImplementation(
        libs.kotlinx.coroutines.test
    )


    // ------------------------------------------
    // ANDROID INSTRUMENTATION TESTING
    // ------------------------------------------

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


    // ------------------------------------------
    // DEBUG TOOLS
    // ------------------------------------------

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}