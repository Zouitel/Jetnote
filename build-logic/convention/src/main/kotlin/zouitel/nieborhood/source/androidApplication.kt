package zouitel.nieborhood.source

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureAndroidApplication(common: CommonExtension <*, *, *, *, *, *>) {
    common.apply {
        defaultConfig {
            vectorDrawables {
                useSupportLibrary = true
            }

            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
                debugSymbolLevel = "NONE"
            }
        }

        lint {
            abortOnError = false
        }

        splits {
            abi {
                // AppBundle tasks usually contain "bundle" in their name
                //noinspection WrongGradleMethod
                val isBuildingBundle =
                    gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }
                isEnable = !isBuildingBundle
                reset()
                include("arm64-v8a", "x86_64")
                isUniversalApk = true
            }
        }

        packagingOptions {
            resources {
                excludes.addAll(
                    listOf(
                        "META-INF/DEPENDENCIES",
                        "META-INF/LICENSE",
                        "META-INF/LICENSE.txt",
                        "META-INF/license.txt",
                        "META-INF/NOTICE",
                        "META-INF/NOTICE.txt",
                        "META-INF/notice.txt",
                        "META-INF/ASL2.0",
                        "META-INF/*.kotlin_module",
                        "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                        "DebugProbesKt.bin"
                    )
                )
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            jniLibs.keepDebugSymbols.addAll(listOf("*/armeabi-v7a/*.so", "*/arm64-v8a/*.so"))
        }

        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
        dependencies {
            androidUiTestDependencies(libs)
        }
    }
}