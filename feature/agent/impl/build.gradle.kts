plugins {
    id("aiadvent.android.library")
}

android {
    namespace = "com.tishukoff.feature.agent.impl"
}

dependencies {
    implementation(project(":feature:agent:api"))
    implementation(project(":feature:memory:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:invariant:api"))
    implementation(project(":feature:mcp:api"))
    implementation(project(":core:database:api"))
    implementation(libs.okhttp)
    implementation(libs.koin.android)
}
