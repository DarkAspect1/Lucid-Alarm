-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.lucid.alarm.data.** { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-dontwarn androidx.room.paging.**

-keepclassmembers class com.lucid.alarm.data.AlarmEntity { *; }
-keepclassmembers class com.lucid.alarm.data.TrashedZone { *; }
-keep class com.lucid.alarm.BackupManager** { *; }
-keep class com.lucid.alarm.data.prefs.** { *; }

-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

-keep class com.lucid.alarm.ui.components.** { *; }
-keep class com.lucid.alarm.ui.screens.** { *; }
-keep class com.lucid.alarm.ui.theme.** { *; }

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void println(...);
    public static void print(...);
}
