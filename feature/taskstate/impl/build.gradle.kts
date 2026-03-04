plugins {
    id("aiadvent.android.library")
}

android {
    namespace = "com.tishukoff.feature.taskstate.impl"
}

dependencies {
    implementation(project(":feature:taskstate:api"))
    implementation(project(":feature:agent:api"))
    implementation(libs.koin.android)
}
