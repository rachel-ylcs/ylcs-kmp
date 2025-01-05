-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-renamesourcefileattribute SourceFile
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*

-dontnote **

-printmapping ../../build/compose/tmp/desktop-mapping.txt

# native

-keepclassmembers class * {
    public <init>(...);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

# Logger

-assumenosideeffects class * extends org.slf4j.Logger {
      public void trace(...);
      public void debug(...);
      public void info(...);
      public void warn(...);
      public void error(...);
}

# kotlinx.serialization

-keep class kotlinx.serialization.** {*;}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ktor

-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# coil

-keep class coil3.** {*;}