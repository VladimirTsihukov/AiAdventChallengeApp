package com.tishukoff.aiadvent

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")

    commonExtension.buildFeatures.compose = true
}
