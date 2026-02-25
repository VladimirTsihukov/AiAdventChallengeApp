plugins {
    id("aiadvent.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tishukoff.feature.setting.api"
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.navigation3.runtime)
}
