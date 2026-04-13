# Keep Kotlin metadata for Compose
-keep class kotlin.Metadata { *; }

# Keep Compose runtime internals
-keep class androidx.compose.runtime.** { *; }

# Kotlinx coroutines
-dontwarn kotlinx.coroutines.debug.**
