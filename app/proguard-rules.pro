# Room persistence library
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.lucid.alarm.data.** { *; }
-dontwarn androidx.room.paging.**

# Data & serialization models
-keepclassmembers class com.lucid.alarm.data.AlarmEntity { *; }
-keepclassmembers class com.lucid.alarm.data.TrashedZone { *; }
-keep class com.lucid.alarm.BackupManager** { *; }
-keep class com.lucid.alarm.data.prefs.** { *; }

# Android components referenced via manifest / intent
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Custom Compose Views and UI components
-keep class com.lucid.alarm.ui.components.** { *; }
-keep class com.lucid.alarm.ui.screens.** { *; }
-keep class com.lucid.alarm.ui.theme.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin annotations and signatures
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
