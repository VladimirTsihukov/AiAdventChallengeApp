plugins {
    id("aiadvent.android.library")
}

android {
    namespace = "com.tishukoff.feature.taskstate.api"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
