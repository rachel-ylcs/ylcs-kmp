-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepattributes Exceptions, InnerClasses, Deprecated, EnclosingMethod
-keepattributes SourceFile, LineNumberTable, Signature
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*
-dontnote **

# ----------------------------------------- Kotlin ----------------------------------------------- #

-keepclassmembers class * {
    public <init>(...);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# --------------------------------- kotlinx.serialization --------------------------------------- #

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

# ----------------------------------- kotlinx.io --------------------------------------- #

-keep class kotlinx.io.** { *; }
-keep class okio.** { *; }

# ----------------------------------- kotlinx.coroutines --------------------------------------- #

-keep class kotlinx.coroutines.** { *; }

-assumenosideeffects class kotlinx.coroutines.internal.MainDispatcherLoader {
    boolean FAST_SERVICE_LOADER_ENABLED;
}

-assumenosideeffects class kotlinx.coroutines.internal.FastServiceLoaderKt {
    boolean ANDROID_DETECTED;
}

-assumenosideeffects class kotlinx.coroutines.internal.MainDispatchersKt {
    boolean SUPPORT_MISSING;
}

-assumenosideeffects class kotlinx.coroutines.DebugKt {
    boolean getASSERTIONS_ENABLED();
    boolean getDEBUG();
    boolean getRECOVER_STACK_TRACES();
}

# ----------------------------------------- Logger ----------------------------------------------- #

-dontwarn org.slf4j.**