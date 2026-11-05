package zouitel.nieborhood.source

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureAndroidCompose(common: CommonExtension) {
    common.apply {
        buildFeatures.apply {
            compose = true
            buildConfig = true
        }

        dependencies {
            composeDependencies(libs)
            composeKoinDependencies(libs)
            composeBundlesDependencies(libs)
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.optIn.addAll(
                "androidx.compose.material3.ExperimentalMaterial3Api",
                "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "cafe.adriel.voyager.core.annotation.InternalVoyagerApi",
                "kotlin.uuid.ExperimentalUuidApi",
                "androidx.compose.foundation.layout.ExperimentalLayoutApi",
                "androidx.compose.foundation.ExperimentalFoundationApi",
                "androidx.compose.ui.ExperimentalComposeUiApi",
                "androidx.media3.common.util.UnstableApi",
                "kotlin.time.ExperimentalTime"
            )
        }

        testOptions.unitTests {
            isIncludeAndroidResources = true
        }
    }
}
