plugins {
    id("aiadvent.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tishukoff.feature.memory.api"
}

dependencies {
    implementation(project(":core:database:api"))
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.kotlinx.coroutines.core)
}
