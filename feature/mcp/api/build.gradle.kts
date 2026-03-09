plugins {
    id("aiadvent.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tishukoff.feature.mcp.api"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.serialization.core)
}
