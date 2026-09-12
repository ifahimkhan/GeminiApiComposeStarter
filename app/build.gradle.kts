import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

// Assignment requirement: read the secret from local.properties and fall back to
// an environment variable for CI. Neither source is committed to Git.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val geminiApiKey = (
    localProperties.getProperty("GEMINI_API_KEY")
        ?: System.getenv("GEMINI_API_KEY")
        ?: ""
).trim()

fun quotedBuildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.fahim.geminiApiComposeStarter"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.fahim.geminiApiComposeStarter"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            quotedBuildConfigString(geminiApiKey)
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Mandatory assignment requirement: R8 obfuscation for the release APK.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }

    packagingOptions {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}


// Flamingo + compileSdk 33 compatibility:
// Some transitive libraries request androidx.core 1.12.0, which requires compileSdk 34.
// Force AndroidX Core 1.10.1 so the project remains compatible with AGP 8.0.2 / SDK 33.
configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.10.1")
        force("androidx.core:core-ktx:1.10.1")

        force("androidx.emoji2:emoji2:1.3.0")
        force("androidx.emoji2:emoji2-views-helper:1.3.0")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2023.08.00")

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.activity:activity-compose:1.7.2")

    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Gemini Android SDK used by the instructor starter repository.
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Room chat persistence.
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")

    // Preferences DataStore for personalized user settings.
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // The encrypted preferences container stores only the already-encrypted API-key payload.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // XML launch theme used by the Activity.
    implementation("com.google.android.material:material:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
