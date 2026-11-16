plugins {
    alias(libs.plugins.androidDynamicFeature)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "zouitel.neighborhood.speechtranscription"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.workmanager)
    implementation(libs.coroutines.core)
    implementation(project(":app"))
    implementation(projects.core.plugin)
}
