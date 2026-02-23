plugins {
    id("aiadvent.android.library")
    id("aiadvent.android.compose")
}

android {
    namespace = "com.tishukoff.core.designsystem"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
}
