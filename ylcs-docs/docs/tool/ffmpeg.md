# Android FFmpeg 解码库

`compose:components:media` 在 Android 源集中提供 Media3 FFmpeg 音频解码适配与预编译 JNI 库。`FfmpegRenderersFactory` 优先加入 `FfmpegAudioRenderer`，并配置 `EXTENSION_RENDERER_MODE_PREFER`。

## 当前配置

| 项目 | 源码或产物值 |
| --- | --- |
| Media3 | 1.11.1 |
| Android NDK | 29.0.14206865 |
| minSdk | 29 |
| 打包 ABI | `arm64-v8a` |
| JNI 库名 | `ffmpegJNI` |
| 文件 | `ylcs-module/compose/components/media/src/androidMain/jniLibs/arm64-v8a/libffmpegJNI.so` |
| 文件大小 | 1,017,368 字节 |
| SHA-256 | `A11F6693B92AF70338A538666CBDA144CBAFCDE4FB29A73E88FC8077838870F9` |

Kotlin 适配代码位于 `androidx.media3.decoder.ffmpeg` 包下。项目依赖版本与预编译二进制来源是不同信息：当前仓库未提供该产物对应的完整 FFmpeg 提交、decoder 列表与重建命令，不能仅凭依赖版本精确复现该文件。

## 运行时检查

```kotlin
val available = FfmpegLibrary.isAvailable
val version = FfmpegLibrary.version
val supportsFlac = FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_FLAC)
```

该 API 使用 Media3 的 `UnstableApi` 标记，调用方需按项目规则处理 opt-in。`supportsFormat` 在库成功装载后检查实际 decoder；MIME 名称映射存在不代表二进制一定包含对应实现。

当前接入的是音频 renderer，不能根据 `getCodecName` 中存在 H.264/H.265 名称映射推断已经提供 FFmpeg 视频渲染能力。

装载失败时，应核对最终 APK 的 ABI、库文件路径与依赖符号。验证范围应包含实际解码、seek、切换曲目与异常输入。

## 重建入口

重建时使用与项目 Media3 版本匹配的源码，并固定 FFmpeg 的具体提交。Media3 1.11.1 的构建说明要求先构建 FFmpeg，再通过上游 Gradle/CMake 流程生成 JNI wrapper；其脚本中的 `ANDROID_ABI` 表示 Native API level。参见 [Media3 1.11.1 FFmpeg 构建说明](https://github.com/androidx/media/blob/1.11.1/libraries/decoder_ffmpeg/README.md)。

以下变量示例按本项目 NDK 与 minSdk 配置调整，具体 decoder 和脚本补丁需随重建记录固定：

```bash
FFMPEG_MODULE_PATH="/path/to/media/libraries/decoder_ffmpeg/src/main"
NDK_PATH="/path/to/android-ndk/29.0.14206865"
HOST_PLATFORM="linux-x86_64"
ANDROID_ABI=29
ENABLED_DECODERS=(aac mp3 flac opus vorbis)

cd "${FFMPEG_MODULE_PATH}/jni"
./build_ffmpeg.sh \
  "${FFMPEG_MODULE_PATH}" \
  "${NDK_PATH}" \
  "${HOST_PLATFORM}" \
  "${ANDROID_ABI}" \
  "${ENABLED_DECODERS[@]}"
```

上游脚本覆盖多种架构，本项目仅打包 arm64，重建脚本应与所需产物一致。上述命令属于上游重建流程示例，未证明现有 `.so` 使用这些 decoder 或参数生成。

完成后将目标 JNI 库放回现有 `jniLibs/arm64-v8a` 路径，并同步记录 Media3、FFmpeg、NDK、主机、decoder 配置、构建命令与输出摘要。JNI 方法签名需与仓库 Kotlin 适配代码一致。

## 16 KB 页大小验证

Android 官方说明 NDK r28 及以上默认采用 16 KB ELF 对齐；预编译依赖和最终 APK 仍需分别核对，详见 [Android 16 KB 页大小指南](https://developer.android.com/guide/practices/page-sizes?hl=zh-cn)。

Windows 下可检查 ELF LOAD 段：

```powershell
& "$env:ANDROID_SDK_ROOT\ndk\29.0.14206865\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-objdump.exe" -p .\libffmpegJNI.so |
    Select-String -Pattern "LOAD"
```

LOAD 段对齐至少应达到 `2**14`。使用本机已安装的 Android Build Tools 验证最终 APK；以下版本路径仅为示例：

```powershell
& "$env:ANDROID_SDK_ROOT\build-tools\35.0.0\zipalign.exe" -v -c -P 16 4 .\app-release.apk
```

设备运行前可通过 `adb shell getconf PAGE_SIZE` 确认测试环境。16 KB 环境预期返回 `16384`，随后还需执行实际播放验证。NDK 版本与单次 ELF 检查不能代替完整应用兼容性验证。

## 维护记录

每次替换预编译库应记录源码版本、启用功能、产物摘要、目标架构与验证结果。上游适配代码和 FFmpeg 依赖的许可信息应分别保存，并与实际构建配置一致。

## 源码入口

- `ylcs-module/compose/components/media/src/androidMain/kotlin/love/yinlin/media/FfmpegRenderersFactory.kt`
- `ylcs-module/compose/components/media/src/androidMain/kotlin/androidx/media3/decoder/ffmpeg/FfmpegLibrary.kt`
- `ylcs-module/compose/components/media/build.gradle.kts`
