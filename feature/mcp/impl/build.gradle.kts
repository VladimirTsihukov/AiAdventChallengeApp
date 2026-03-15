plugins {
    id("aiadvent.android.library")
    id("aiadvent.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tishukoff.feature.mcp.impl"
}

dependencies {
    implementation(project(":feature:mcp:api"))
    implementation(project(":feature:agent:api"))
    implementation(libs.mcp.kotlin.sdk.client)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
