package zouitel.nieborhood.source

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.collections.addAll

fun Project.configureAndroidLibrary(common: CommonExtension<*, *, *, *, *, *>) {
    common.apply {
        compileSdk = libs.compileSdk

        defaultConfig {
            minSdk = libs.minSdk
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            getByName("release") {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }


        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                    "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                    "-Xcontext-parameters"
                )
            }
        }

        dependencies {
            androidDependencies(libs)
//            androidTestDependencies(libs)
            koinDependencies(libs)
        }
    }
}