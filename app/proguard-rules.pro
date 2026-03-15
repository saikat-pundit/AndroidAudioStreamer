# Keep services alive
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.app.job.JobService

# Keep all classes in your package
-keep class com.example.audiostreamer.** { *; }

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep for reflection
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Don't obfuscate service names
-keepnames class * extends android.app.Service
-keepnames class * extends android.content.BroadcastReceiver
