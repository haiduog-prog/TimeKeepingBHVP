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
