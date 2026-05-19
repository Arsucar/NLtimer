# === NLtimer ProGuard/R8 Rules ===

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === Hilt / Dagger ===
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# === Room ===
-keep class com.nltimer.core.data.database.entity.** { *; }
-keep class com.nltimer.core.data.database.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# === DataStore ===
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# === Kotlin Serialization (if used) ===
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep data classes used by Compose and potential serialization
-keep class com.nltimer.core.data.model.** { *; }
-keep class com.nltimer.feature.**.model.** { *; }

# === App-specific keep rules ===
-keep class com.nltimer.app.NLtimerApplication { *; }
-keep class com.nltimer.app.MainActivity { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
