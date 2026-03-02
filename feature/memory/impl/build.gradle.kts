plugins {
    id("aiadvent.android.library")
    id("aiadvent.android.compose")
}

android {
    namespace = "com.tishukoff.feature.memory.impl"
}

dependencies {
    implementation(project(":feature:memory:api"))
    implementation(project(":feature:agent:api"))
    implementation(project(":core:database:api"))
    implementation(libs.okhttp)
    implementation(libs.androidx.navigation3.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
