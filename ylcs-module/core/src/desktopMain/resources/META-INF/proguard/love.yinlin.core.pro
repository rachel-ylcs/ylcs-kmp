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

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.SignalHandler
-dontwarn sun.misc.Signal
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keepclassmembers class kotlinx.coroutines.flow.ReadonlySharedFlow {
    kotlinx.coroutines.Job job;
}

-keepclassmembers class kotlinx.coroutines.flow.ReadonlyStateFlow {
    kotlinx.coroutines.Job job;
}

# ----------------------------------------- Logger ----------------------------------------------- #

-dontwarn org.slf4j.**