-dontobfuscate
-keepattributes Signature,LineNumberTable,*Annotation*,InnerClasses,EnclosingMethod

# App code: keep the public surface (DI, serialization and Compose entry points), let R8 shrink the rest.
-keep,allowoptimization class dev.bunko.** { public protected *; }

# Fix crash with reflection access to methods
-keepclassmembers class androidx.lifecycle.SavedStateHandleSupport { *; }
-keepclassmembers class androidx.lifecycle.SavedStateHandlesVM { *; }
-keepclassmembers class androidx.lifecycle.SavedStateHandlesProvider { *; }

# Kotlin
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------------------------
# Apollo GraphQL: the generated operations, models and adapters are plain classes reached through
# static registries and name-based lookups (`__typename` -> model), so keep them whole.
# ---------------------------------------------------------------------------------------------
-keep class dev.bunko.data.graphql.** { *; }
-keepclassmembers class * implements com.apollographql.apollo.api.Operation { *; }
-keepclassmembers class * implements com.apollographql.apollo.api.Operation$Data { *; }
-keepclassmembers class * implements com.apollographql.apollo.api.Fragment$Data { *; }
-dontwarn com.apollographql.apollo.**

# ---------------------------------------------------------------------------------------------
# Ktor: engines are discovered through ServiceLoader and the plugins use some reflection.
# ---------------------------------------------------------------------------------------------
-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }
-keep class io.ktor.client.engine.okhttp.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.network.sockets.DatagramSendChannel
-dontwarn io.ktor.util.debug.**
-dontwarn java.lang.management.**

# OkHttp / Okio
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okio.**

# ---------------------------------------------------------------------------------------------
# kotlinx.serialization
# ---------------------------------------------------------------------------------------------
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
    static ** INSTANCE;
}
-keep,includedescriptorclasses class **$$serializer { *; }
-keep,includedescriptorclasses class dev.bunko.**$$serializer { *; }
-keepclassmembers class dev.bunko.** {
    *** Companion;
}
-keepclasseswithmembers class dev.bunko.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# offline chapter metadata (OfflineChapterMeta) is a plain line format, not kotlinx.serialization,
# so the offline reader needs nothing beyond the app keep rule above.

# ---------------------------------------------------------------------------------------------
# AndroidX / Compose ship their own consumer rules; only the Compose runtime's reflective
# composer lookups are worth pinning.
# ---------------------------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**
