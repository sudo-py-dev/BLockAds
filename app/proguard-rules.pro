# Proguard rules for AdsBlock VPN

# ---------------------------------------------------------------------------
# DNSJava (Critical: uses reflection to load record types)
# ---------------------------------------------------------------------------
-dontwarn sun.net.spi.nameservice.**
-dontwarn org.xbill.DNS.**
-dontwarn org.slf4j.**
-keep class org.xbill.DNS.** { *; }

# ---------------------------------------------------------------------------
# OkHttp3 / Okio
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep interface okio.** { *; }

# ---------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# ---------------------------------------------------------------------------
# General
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
