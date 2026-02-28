# ----------------------------------------- ktor ---------------------------------------------- #

-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }

# ----------------------------------------- OkHttp ---------------------------------------------- #

-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.oracle.svm.**
-dontwarn org.graalvm.nativeimage.**