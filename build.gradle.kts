// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // KSP runs the Room annotation processor. If Gradle sync complains, bump to the
    // latest release from https://github.com/google/ksp/releases
    id("com.google.devtools.ksp") version "2.3.6" apply false
}
