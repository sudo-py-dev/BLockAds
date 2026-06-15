
# Keep Hilt entry points
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.blockads.app.**$$serializer { *; }
-keepclassmembers class com.blockads.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.blockads.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep DataStore
-keep class androidx.datastore.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin reflection (for StringsTest — not needed in release)
-keep class kotlin.reflect.** { *; }

# Keep crash info classes
-keep class com.blockads.app.ui.crash.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
