# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep @kotlinx.serialization.Serializable class * { *; }

# Kotlinx Datetime
-keep class kotlinx.datetime.** { *; }

# Ktor Client
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keep class io.ktor.client.** { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.util.** { *; }
-keep class io.ktor.utils.io.** { *; }

-dontwarn io.ktor.**

# Ktor specific
-keep class io.ktor.client.plugins.HttpTimeout** { *; }
-keep class io.ktor.client.plugins.contentnegotiation.** { *; }
-keep class io.ktor.client.plugins.observer.** { *; }
-keep class io.ktor.client.plugins.logging.** { *; }
-keep class io.ktor.utils.io.core.** { *; }

# Google AI / Generative AI
-keep class com.google.ai.** { *; }
-keepclassmembers class com.google.ai.** { *; }
-dontwarn com.google.ai.**

# Pebble Templates
-keep class io.pebbletemplates.** { *; }
-dontwarn io.pebbletemplates.**
-dontwarn com.github.benmanes.caffeine.**
-dontwarn jakarta.servlet.**
-dontwarn javax.servlet.**

# MetaProbe
-keep class com.devscion.metaprobekmp.** { *; }

# Caffeine (optional dependency)
-dontwarn com.github.benmanes.caffeine.**

# Servlet APIs (optional dependencies)
-dontwarn jakarta.servlet.**
-dontwarn javax.servlet.**

# Java Management (for debugging detection)
-dontwarn java.lang.management.**

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# OkHttp (Ktor engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

-repackageclasses
-allowaccessmodification
-overloadaggressively

# Prevent Voyager screens from being renamed to the same letter
-keep class * implements cafe.adriel.voyager.core.screen.Screen { *; }
-keepclassmembers class * extends cafe.adriel.voyager.core.model.ScreenModel { *; }