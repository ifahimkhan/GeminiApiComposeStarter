import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val geminiApiKey: String =
    localProperties.getProperty("GEMINI_API_KEY")?.trim()
        ?: System.getenv("GEMINI_API_KEY")?.trim()
        ?: ""

android {
    namespace = "com.fahim.geminiApiComposeStarter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fahim.geminiApiComposeStarter"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"" + geminiApiKey
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\""
        )
    }

    buildTypes {
        release {
            // Required for the assignment
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Compose
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Responsive layouts
    implementation(libs.androidx.material3.window.size)

            // Gemini
            implementation(libs.google.generativeai)

            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)
            ksp(libs.androidx.room.compiler)

            // Unit tests
            testImplementation(libs.junit)
            testImplementation(libs.kotlinx.coroutines.test)

            // Instrumented / Compose UI tests
            androidTestImplementation(libs.androidx.junit)
            androidTestImplementation(libs.androidx.espresso.core)
            androidTestImplementation(platform(libs.androidx.compose.bom))
            androidTestImplementation(libs.androidx.compose.ui.test.junit4)

            // Debug
            debugImplementation(libs.androidx.compose.ui.tooling)
            debugImplementation(libs.androidx.compose.ui.test.manifest)
}