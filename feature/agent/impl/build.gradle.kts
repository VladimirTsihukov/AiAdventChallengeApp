plugins {
    id("aiadvent.android.library")
}

android {
    namespace = "com.tishukoff.feature.agent.impl"
}

dependencies {
    implementation(project(":feature:agent:api"))
    implementation(project(":core:database:api"))
    implementation(libs.okhttp)
    implementation(libs.koin.android)
}
