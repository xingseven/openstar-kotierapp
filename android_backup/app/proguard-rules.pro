# EasyTier JNI
-keep class com.easytier.jni.EasyTierJNI { *; }

# Keep Kotlin data classes for JSON serialization
-keep class com.easytier.data.** { *; }

# Backend protocol and adapter classes
-keep class com.easytier.backend.** { *; }
-keep class com.easytier.backend.protocol.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
