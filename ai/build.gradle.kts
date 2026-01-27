import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "zouitel.neighborhood.ai"

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.optIn.add("kotlin.uuid.ExperimentalUuidApi")
        compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(projects.common.logic)
    implementation(projects.common.ui)

    // apache commons
    implementation(libs.commons.text)

    // kotlinx
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    // okhttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.okhttp.logging)
}