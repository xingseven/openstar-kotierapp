# EasyTier JNI
-keep class com.easytier.jni.EasyTierJNI { *; }

# Keep Kotlin data classes for JSON serialization
-keep class com.easytier.data.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
