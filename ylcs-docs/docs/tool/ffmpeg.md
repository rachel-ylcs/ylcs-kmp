# FFmpeg 库编译指南

## Android

在 Windows 上编译 media3 软解码所需的 FFmpeg 库, 并开启 16K Page Size

> Linux/macOS 上编译请直接参考官方文档: <https://github.com/androidx/media/blob/release/libraries/decoder_ffmpeg/README.md>

1. 下载 mingw64-gcc (只需要使用里面的 make, 所以只下个 make 也行)

    将 mingw64 的 bin 目录添加到环境变量中, 并将 mingw32-make.exe 改名为 make.exe, 或者建个链接

2. 打开 git bash (我试过 powershell 和 wsl 但是都不行, 还是用 git bash 或者 cygwin 吧)

3. 拉 media3 源码

    ```bash
    git clone https://github.com/androidx/media.git
    ```

4. 在 libraries/decoder_ffmpeg/src/main/jni 目录下拉 ffmpeg 源码

    ```bash
    cd libraries/decoder_ffmpeg/src/main/jni
    git clone git://source.ffmpeg.org/ffmpeg
    cd ffmpeg
    git checkout release/6.0
    ```

5. 配置变量

    ```bash
    # 需要根据自己的环境修改
    FFMPEG_MODULE_PATH="/d/ylcs/media/libraries/decoder_ffmpeg/src/main"
    NDK_PATH="/d/SDK/Android/ndk/26.1.10909125"
    HOST_PLATFORM="windows-x86_64"
    ANDROID_ABI=26
    # 暂时只开 aac 和 flac 就够了
    ENABLED_DECODERS=$(aac flac)
    ```

6. 编译 ffmpeg

    先修改 libraries/decoder_ffmpeg/src/main/jni/build_ffmpeg.sh, 将编译各个架构的命令注释掉, 只留下 arm64-v8a

    然后执行 shell 脚本编译 ffmpeg 静态库 (TODO: 是否需要改为编译动态库)

    ```bash
    ./build_ffmpeg.sh "${FFMPEG_MODULE_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}" "${ANDROID_ABI}" "${ENABLED_DECODERS[@]}"
    ```

7. 编译 aar

    需要按如下补丁内容修改 common_library_config.gradle 和 constants.gradle, 只编译 arm64-v8a 架构的 aar 并将 API 版本改为 26, 否则无法编译

    ```diff
    diff --git a/common_library_config.gradle b/common_library_config.gradle
    index 9c357a6381..4ee05a6d4d 100644
    --- a/common_library_config.gradle
    +++ b/common_library_config.gradle
    @@ -27,6 +27,9 @@ android {
            aarMetadata {
                minCompileSdk = project.ext.compileSdkVersion
            }
    +        ndk {
    +            abiFilters = ["arm64-v8a"]
    +        }
        }
    
        lintOptions {
    diff --git a/constants.gradle b/constants.gradle
    index 452bc94154..328a8d2e96 100644
    --- a/constants.gradle
    +++ b/constants.gradle
    @@ -14,7 +14,7 @@
    project.ext {
        releaseVersion = '1.6.1'
        releaseVersionCode = 1_006_001_3_00
    -    minSdkVersion = 21
    +    minSdkVersion = 26
        // See https://developer.android.com/training/cars/media/automotive-os#automotive-module
        automotiveMinSdkVersion = 28
        appTargetSdkVersion = 34
    ```

    然后运行 gradle 命令编译 aar 库

    ```bash
    gradlew.bat :lib-decoder-ffmpeg:assembleRelease
    ```

    生成的 aar 位于 libraries/decoder_ffmpeg/buildout/outputs/aar 目录中

## iOS

TODO
