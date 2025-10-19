-printmapping ../build/desktop-mapping.txt

# ----------------------------------------- JNA ----------------------------------------------- #

-keep class com.sun.** { *; }
-dontwarn com.sun.jna.**
-keep class * implements com.sun.jna.** { *; }

# ----------------------------------------- VLCJ ---------------------------------------------- #

# From https://github.com/caprica/vlcj/issues/1210
# Not always compiling on Mac
-dontwarn com.apple.eawt.**
# Keep DiscoveryDirectoryProviders loaded via SPI
-keep class * implements uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider { *; }
-keep interface uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider { *; }

# ----------------------------------------- Sketch ----------------------------------------------- #

-keep class com.github.panpf.sketch.**

# ----------------------------------------- App ----------------------------------------------- #