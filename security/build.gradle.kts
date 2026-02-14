import kotlin.collections.plusAssign

plugins {
    alias(libs.plugins.zouitel.androidLibrary)
    alias(libs.plugins.zouitel.androidCompose)
    alias(libs.plugins.zouitel.composeVoyager)
}

android {
    namespace = "zouitel.neighborhood.security"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17 -fvisibility=hidden")
                cppFlags("-Wl,-z,max-page-size=16384")
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    packaging {
        jniLibs {
            // This ensures libs are extracted and aligned during installation
            useLegacyPackaging = true
        }
    }
}

dependencies {
    //Modules.
    implementation(projects.common.ui)
    implementation(projects.common.logic)
    implementation(projects.domain)

    //Biometric.
    implementation(libs.androidx.biometric)

    //Lifecycle.
    implementation(libs.lifecycle.compose.runtime)

    //Security.
    implementation (libs.security)
}