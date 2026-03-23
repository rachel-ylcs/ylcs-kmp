# ----------------------------------------- ktor ---------------------------------------------- #

-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }

# ----------------------------------------- IDEA ---------------------------------------------- #

-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean