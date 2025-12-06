-printmapping ../build/desktop-mapping.txt

# ----------------------------------------- JNA ----------------------------------------------- #

-keep class com.sun.** { *; }
-dontwarn com.sun.jna.**
-keep class * implements com.sun.jna.** { *; }

# ----------------------------------- NativeMusicPlayer ----------------------------------------- #

-keep class love.yinlin.platform.WindowsNativeAudioPlayer** { *; }
-keep class love.yinlin.platform.WindowsNativeVideoPlayer** { *; }

# ----------------------------------------- Sketch ----------------------------------------------- #

-keep class com.github.panpf.sketch.**

# ----------------------------------------- App ----------------------------------------------- #