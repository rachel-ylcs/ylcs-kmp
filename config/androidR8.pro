-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-renamesourcefileattribute SourceFile
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*

# native

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class * {
    public <init>(...);
}

# 反射与序列化

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(...);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Android

-keep class **.R$*{*;}

-keep public class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Logger

-assumenosideeffects class android.util.Log {
      public static boolean isLoggable(java.lang.String, int);
      public static int v(...);
      public static int i(...);
      public static int w(...);
      public static int d(...);
      public static int e(...);
}

# kotlinx.serialization

-keep class kotlinx.serialization.** {*;}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# libpag

-keep class org.libpag.**{*;}
-keep class androidx.exifinterface.**{*;}