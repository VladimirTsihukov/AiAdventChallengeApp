plugins {
    id("aiadvent.android.library")
    id("aiadvent.android.compose")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tishukoff.feature.rag.impl"
}

dependencies {
    implementation(project(":feature:rag:api"))
    implementation(libs.androidx.navigation3.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
