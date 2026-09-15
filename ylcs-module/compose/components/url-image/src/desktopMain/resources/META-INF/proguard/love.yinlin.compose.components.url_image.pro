# ----------------------------------------- JNA ----------------------------------------------- #

-keep class com.sun.** { *; }
-dontwarn com.sun.jna.**
-keep class * implements com.sun.jna.** { *; }

# ----------------------------------------- Sketch ----------------------------------------------- #

-dontwarn org.jetbrains.skia.Image