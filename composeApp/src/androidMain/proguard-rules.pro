-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-renamesourcefileattribute SourceFile
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, EnclosingMethod

# 日志

-assumenosideeffects class android.util.Log {
      public static boolean isLoggable(java.lang.String, int);
      public static int v(...);
      public static int i(...);
      public static int w(...);
      public static int d(...);
      public static int e(...);
}

# 注解

-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*

# 反射

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# 序列化

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

# 枚举

-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(...);
}

# 原生方法

-keepclasseswithmembernames class * {
    native <methods>;
}

# 构造函数

-keepclassmembers class * {
    public <init>(...);
}

# 资源
-keep class **.R$*{*;}

# View

-keep public class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# libpag

-keep class org.libpag.**{*;}
-keep class androidx.exifinterface.**{*;}

# gson

-keep class com.google.gson.**{*;}

# GsyVideoPlayer

-keep class com.shuyu.gsyvideoplayer.video.** {*;}
-dontwarn com.shuyu.gsyvideoplayer.video.**
-keep class com.shuyu.gsyvideoplayer.video.base.** {*;}
-dontwarn com.shuyu.gsyvideoplayer.video.base.**
-keep class com.shuyu.gsyvideoplayer.utils.** {*;}
-dontwarn com.shuyu.gsyvideoplayer.utils.**
-keep class com.shuyu.gsyvideoplayer.player.** {*;}
-dontwarn com.shuyu.gsyvideoplayer.player.**
-keep class tv.danmaku.ijk.** { *; }
-dontwarn tv.danmaku.ijk.**
-keep class androidx.media3.** {*;}
-keep interface androidx.media3.**

# 反射用到的字段

# APP

-keep,allowobfuscation @com.yinlin.rachel.annotation.** class *
-keepclassmembers class com.yinlin.rachel.data.**{*;}