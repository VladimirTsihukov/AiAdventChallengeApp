plugins {
    id("aiadvent.android.library")
}

android {
    namespace = "com.tishukoff.feature.agent.impl"
}

dependencies {
    implementation(project(":feature:agent:api"))
    implementation(libs.okhttp)
    implementation(libs.koin.android)
}
