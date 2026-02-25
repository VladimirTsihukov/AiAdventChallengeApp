plugins {
    id("aiadvent.android.library")
}

android {
    namespace = "com.tishukoff.feature.agent.api"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
