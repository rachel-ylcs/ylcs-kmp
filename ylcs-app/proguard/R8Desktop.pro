-printmapping desktop-mapping.txt

# ----------------------------------------- JNA ----------------------------------------------- #

-keep class com.sun.** { *; }
-dontwarn com.sun.jna.**
-keep class * implements com.sun.jna.** { *; }

# ----------------------------------------- App ----------------------------------------------- #
