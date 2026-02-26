plugins {
    id("aiadvent.android.library")
}

android {
    namespace = "com.tishukoff.core.database.api"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
