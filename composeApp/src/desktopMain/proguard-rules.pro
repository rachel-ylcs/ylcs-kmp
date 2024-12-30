-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-renamesourcefileattribute SourceFile
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*

# Logger

-assumenosideeffects class * extends org.slf4j.Logger {
      public void trace(...);
      public void debug(...);
      public void info(...);
      public void warn(...);
      public void error(...);
}
-keep class org.slf4j.**{ *; }

-keepclassmembers,includedescriptorclasses class * {
    public <init>(...);
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# kotlinx.serialization

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# coil

-keep,includedescriptorclasses public class coil3.** {*;}