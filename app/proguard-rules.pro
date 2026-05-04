# ── ChamCong ProGuard Rules ──

# Keep Room entities (reflection-based)
-keep class com.bienhieu.chamcong.data.local.** { *; }

# Keep TFLite classes
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# Keep ML Kit face detection
-keep class com.google.mlkit.** { *; }

# Keep VectorTypeConverter (Room reflection)
-keep class com.bienhieu.chamcong.data.local.VectorTypeConverter { *; }

# ── Supabase / Ktor / Kotlinx Serialization ──

# Keep Supabase remote models (serialized via @SerialName)
-keep class com.bienhieu.chamcong.data.remote.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bienhieu.chamcong.**$$serializer { *; }
-keepclassmembers class com.bienhieu.chamcong.** {
    *** Companion;
}
-keepclasseswithmembers class com.bienhieu.chamcong.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Supabase
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**