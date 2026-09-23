import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()

val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

val geminiApiKey =
    localProperties.getProperty("GEMINI_API_KEY")?.trim()
        ?: System.getenv("GEMINI_API_KEY")?.trim()
        ?: ""

android {
    namespace = "com.example.n083harshitraiassignment1"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.n083harshitraiassignment1"

        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"$geminiApiKey\""
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {

    // Core Android
    implementation(libs.androidx.core.ktx)

    // Activity + Compose
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(
        platform(libs.androidx.compose.bom)
    )

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Material 3
    implementation(libs.androidx.compose.material3)

    // Material Icons
    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    // Lifecycle
    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.11.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0"
    )

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    // Unit Tests
    testImplementation(
        libs.junit
    )
    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1"
    )

    // Android Tests
    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    // Debug
    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}