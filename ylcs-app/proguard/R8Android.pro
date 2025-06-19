# ----------------------------------------- Android ----------------------------------------------- #

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep class **.R$*{*;}

-keep public class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ----------------------------------------- Logger ----------------------------------------------- #

-assumenosideeffects class android.util.Log {
      public static boolean isLoggable(java.lang.String, int);
      public static int v(...);
      public static int i(...);
      public static int w(...);
      public static int d(...);
      public static int e(...);
}

# ----------------------------------------- IDEA ---------------------------------------------- #

-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ---------------------------------------- libpag --------------------------------------------- #

-keep class org.libpag.**{*;}
-keep class androidx.exifinterface.**{ *; }

# ----------------------------------------- App ----------------------------------------------- #
