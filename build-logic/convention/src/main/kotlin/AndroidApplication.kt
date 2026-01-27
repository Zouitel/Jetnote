import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import zouitel.nieborhood.source.configureAndroidApplication
import zouitel.nieborhood.source.configureAndroidCompose
import zouitel.nieborhood.source.configureAndroidLibrary
import zouitel.nieborhood.source.configureComposeVoyager
import zouitel.nieborhood.source.libs

class AndroidApplication: Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<ApplicationExtension> {
                defaultConfig.targetSdk = libs.findVersion("targetSdk-version").get().requiredVersion.toInt()

                configureAndroidApplication(this)
                configureAndroidCompose(this)
                configureAndroidLibrary(this)
                configureComposeVoyager(this)
            }
        }
    }
}