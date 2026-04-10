# Netty
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Proxyee
-keep class com.github.monkeywie.proxyee.** { *; }
-dontwarn com.github.monkeywie.proxyee.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# AndroidX
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.core.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Reflection / coroutines
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class kotlin.Metadata { public <methods>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
