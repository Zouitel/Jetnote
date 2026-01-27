@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    "implementation"(libs.kotlin.gradle.plugin)
    "implementation"(libs.android.gradle.plugin)
}

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        register("android-library") {
            id = "zouitel.android.library"
            implementationClass = "AndroidLibrary"
        }
    }

    plugins {
        register("android-compose") {
            id = "zouitel.android.compose"
            implementationClass = "AndroidCompose"
        }
    }

    plugins {
        register("android-application") {
            id = "zouitel.android.application"
            implementationClass = "AndroidApplication"
        }
    }

    plugins {
        register("compose-voyager") {
            id = "zouitel.compose.voyager"
            implementationClass = "ComposeVoyager"
        }
    }
}