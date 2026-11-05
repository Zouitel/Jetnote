package zouitel.nieborhood.source

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureAndroidApplication(common: CommonExtension) {
    common.apply {
        defaultConfig.apply {
            vectorDrawables {
                useSupportLibrary = true
            }

            ndk {
                // Match the ABIs shipped by the document module's native libraries.
                abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                debugSymbolLevel = "NONE"
            }
        }

        lint.abortOnError = true

        splits.apply {
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

        packagingOptions.resources {
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

        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17

        packaging.jniLibs {
            keepDebugSymbols.addAll(listOf("*/arm64-v8a/*.so", "*/x86_64/*.so"))
        }
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
