# GENERAL OPTIONS

# turn on all optimizations except those that are known to cause problems on Android
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 7
-allowaccessmodification
-dontpreverify

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-keepattributes *Annotation*

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# TUSKY SPECIFIC OPTIONS

# keep members of our model classes, they are used in json de/serialization
-keepclassmembers class com.keylesspalace.tusky.entity.* { *; }

-keep public enum com.keylesspalace.tusky.entity.*$** {
    **[] $VALUES;
    public *;
}

-keep enum com.keylesspalace.tusky.db.DraftAttachment$Type {
    public *;
}

# preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# remove all logging from production apk
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int w(...);
    public static int v(...);
    public static int i(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber* {
    public static *** d(...);
    public static *** w(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** plant(...);
}

-assumenosideeffects class java.lang.Exception* {
public void printStackTrace();
}

-assumenosideeffects class java.lang.String {
    public static java.lang.String format(...);
}

# remove some kotlin overhead
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void throwUninitializedPropertyAccessException(java.lang.String);
}

# Gson
# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Retain abstract classes and interfaces from Gson
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Because new AGP and Gradle
# From: https://github.com/google/gson/issues/2379#issuecomment-1564517629
# Thanks to SekthDroid for pointing me to the path for fixing this
-if class *
-keepclasseswithmembers class <1> {
    <init>(...);
    @com.google.gson.annotations.SerializedName <fields>;
}
