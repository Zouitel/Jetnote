package zouitel.nieborhood.source

import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.androidDependencies(libs: VersionCatalog) {
    add("implementation", libs.findLibrary("core-ktx").get())
    add("implementation", libs.findLibrary("lifecycle-runtime-ktx").get())
}

fun DependencyHandlerScope.androidTestDependencies(libs: VersionCatalog) {
}

fun DependencyHandlerScope.composeDependencies(libs: VersionCatalog) {
    add("implementation", platform(libs.findLibrary("compose-bom").get()))
    add("implementation", libs.findLibrary("activity-compose").get())
    add("implementation", libs.findLibrary("ui-graphics").get())
    add("debugImplementation", libs.findLibrary("ui-tooling").get())
    add("implementation", libs.findLibrary("compose-constraintlayout").get())
    add("implementation", libs.findLibrary("compose-foundation").get())
}

fun DependencyHandlerScope.composeBundlesDependencies(libs: VersionCatalog) {
    add("implementation", libs.findBundle("compose").get())
    add("androidTestImplementation", platform(libs.findLibrary("compose-bom").get()))
    add("androidTestImplementation", libs.findLibrary("ui-test-junit4").get())
    add("debugImplementation", libs.findLibrary("ui-test-manifest").get())
}

fun DependencyHandlerScope.koinDependencies(libs: VersionCatalog) {
    add("implementation", libs.findLibrary("koin-core").get())
    add("implementation", libs.findLibrary("koin-android").get())
    add("implementation", libs.findLibrary("koin-coroutines").get())
}

fun DependencyHandlerScope.composeKoinDependencies(libs: VersionCatalog) {
    add("implementation", libs.findLibrary("koin-compose").get())
}

fun DependencyHandlerScope.voyagerDependencies(libs: VersionCatalog) {
    add("implementation", libs.findLibrary("voyager-navigator").get())
    add("implementation", libs.findLibrary("voyager-screenModel").get())
    add("implementation", libs.findLibrary("voyager-transitions").get())
    add("implementation", libs.findLibrary("voyager-koin").get())
    add("implementation", libs.findLibrary("voyager-bottom-sheet").get())
}
