plugins {
    id("aiadvent.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tishukoff.core.database.impl"
}

dependencies {
    implementation(project(":core:database:api"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.koin.android)
}
