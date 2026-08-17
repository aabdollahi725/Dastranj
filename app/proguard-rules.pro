# PRD §12 / CLAUDE.md §10: android.util.Log must be fully stripped from release builds.
# assumenosideeffects lets R8 delete these calls outright rather than just shrinking them.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static java.lang.String getStackTraceString(...);
}

# SQLCipher ships native bindings that R8 cannot see through.
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }

# Room generates implementations reflectively referenced by the runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Kotlin coroutines internals.
-dontwarn kotlinx.coroutines.**
