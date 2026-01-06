-printmapping ../build/desktop-mapping.txt

# ----------------------------------------- JNA ----------------------------------------------- #

-keep class com.sun.** { *; }
-dontwarn com.sun.jna.**
-keep class * implements com.sun.jna.** { *; }

# ----------------------------------------- FFI ----------------------------------------------- #

-keepclassmembers class * {
    @java.lang.invoke.MethodHandle$PolymorphicSignature <methods>;
}

# ----------------------------------- NativeMusicPlayer ----------------------------------------- #

-keep class love.yinlin.platform.WindowsNativeAudioPlayer** { *; }
-keep class love.yinlin.platform.WindowsNativeVideoPlayer** { *; }

# ----------------------------------------- Sketch ----------------------------------------------- #

-keep class com.github.panpf.sketch.**

# ----------------------------------------- App ----------------------------------------------- #

-keep class love.yinlin.platform.MacOSSingleInstance {
    private static java.lang.foreign.MemorySegment MessageCallback(java.lang.foreign.MemorySegment, int, java.lang.foreign.MemorySegment, java.lang.foreign.MemorySegment);
}