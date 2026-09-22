# Android FFmpeg 解码库

`compose.components:media` 在 Android 上内置 Media3 FFmpeg 音频 renderer，用于系统 MediaCodec 不支持或需要软件优先解码的格式。日常构建已经包含预编译库，只有升级 Media3/NDK、增加 decoder 或修复原生兼容性时才需要重编译。

## 当前仓库状态

基线 `15d3c8ba` 的关键值：

| 项目 | 当前值 |
| --- | --- |
| Media3 | 1.11.1 |
| Android NDK | 29.0.14206865 |
| minSdk | 29 |
| ABI | 仅 `arm64-v8a` |
| JNI 库名 | `ffmpegJNI` |
| 预编译文件 | `ylcs-module/compose/components/media/src/androidMain/jniLibs/arm64-v8a/libffmpegJNI.so` |
| 文件大小 | 1,017,368 bytes |
| SHA-256 | `A11F6693B92AF70338A538666CBDA144CBAFCDE4FB29A73E88FC8077838870F9` |

Kotlin 侧同时保留了 `androidx.media3.decoder.ffmpeg` 下的 renderer/decoder/library glue。`FfmpegRenderersFactory` 把 `FfmpegAudioRenderer` 放在系统 renderer 前，并设置 `EXTENSION_RENDERER_MODE_PREFER`。

`FfmpegLibrary.getCodecName` 认识 AAC、MP3、AC3/EAC3、TrueHD、DTS、Vorbis、Opus、AMR、FLAC、ALAC、μ-law/A-law，以及 H.264/H.265 名称；真正是否可用仍取决于 `.so` 编译时启用的 decoder。名称映射不代表二进制一定包含该 codec。

## 运行时检查

```kotlin
val loaded = FfmpegLibrary.isAvailable
val version = FfmpegLibrary.version
val supportsFlac = FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_FLAC)
```

`isAvailable=false` 通常意味着 ABI 不匹配、库未打包、依赖符号缺失或装载失败。检查最终 APK 的 `lib/arm64-v8a/libffmpegJNI.so`，不要只检查源码目录。

## 可复现重建前提

当前仓库只保存产物，没有与该 `.so` 配套的 Media3 commit、FFmpeg commit、decoder 列表和完整命令清单。因此不能仅凭文件名精确复现现有二进制。下一次重建应把这些信息连同新 SHA-256 写入变更记录：

```text
Media3 source commit/tag
FFmpeg source commit/tag
NDK exact version
ANDROID_ABI / target ABI
enabled decoders
build host and command
ELF + APK 16 KiB alignment verification
output SHA-256
license review
```

上游 [AndroidX Media FFmpeg README](https://github.com/androidx/media/blob/release/libraries/decoder_ffmpeg/README.md) 目前仍要求手工构建 FFmpeg，再由 Gradle/CMake 生成 JNI wrapper；上游说明主要支持 Linux/macOS，Windows 只作为可能可行的路径。优先在干净的 Linux CI/容器中建立可重复任务。

## 重建流程

### 1. 固定匹配的源码

检出与项目 Media3 `1.11.1` 对应的 AndroidX Media 源码，不要直接长期跟随浮动 `release` 分支。进入：

```text
libraries/decoder_ffmpeg/src/main
```

在其 `jni` 下获取并固定 FFmpeg。上游当前推荐 FFmpeg `release/6.0`，但仍应记录精确 commit，因为 branch 会移动，且并非所有 FFmpeg 版本都保证兼容该 wrapper。

### 2. 使用项目约束

```bash
FFMPEG_MODULE_PATH="/path/to/media/libraries/decoder_ffmpeg/src/main"
NDK_PATH="/path/to/android-ndk/29.0.14206865"
HOST_PLATFORM="linux-x86_64"
ANDROID_ABI=29
ENABLED_DECODERS=(aac mp3 flac opus vorbis)
```

`ANDROID_ABI` 是 Native API level，通常与 minSdk 一致且不能高于应用 minSdk；它不是 `arm64-v8a` 字符串。decoder 列表只放产品实际需要且许可证允许的项，最终再用 `supportsFormat` 验证。

### 3. 只构建所需 ABI

上游脚本默认可能构建多种 ABI。项目当前只打包 `arm64-v8a`，应在固定的脚本补丁中保留该 ABI，并执行：

```bash
cd "${FFMPEG_MODULE_PATH}/jni"
./build_ffmpeg.sh \
  "${FFMPEG_MODULE_PATH}" \
  "${NDK_PATH}" \
  "${HOST_PLATFORM}" \
  "${ANDROID_ABI}" \
  "${ENABLED_DECODERS[@]}"
```

这一步生成 FFmpeg 静态库；随后使用匹配 Media3 源码的 Gradle/CMake 任务链接 `libffmpegJNI.so`。不要从另一 Media3 版本只拷贝 JNI wrapper，它与 Kotlin/Java glue 的 native 方法签名必须匹配。

### 4. 替换仓库产物

只将最终 arm64 产物放到：

```text
ylcs-module/compose/components/media/
└─ src/androidMain/jniLibs/arm64-v8a/libffmpegJNI.so
```

不要把完整上游构建目录、静态库和其他 ABI 一起提交。更新哈希、版本记录与许可证归档。

## 16 KiB 页大小

项目使用 NDK r29；按 [Android 官方 16 KiB 指南](https://developer.android.com/guide/practices/page-sizes?hl=zh-CN)，NDK r28+ 默认生成 16 KiB 对齐产物。但“使用新 NDK”不能代替验证：所有预编译 `.so` 和最终 APK 的 ZIP 对齐都必须检查。

Windows 上检查 ELF LOAD 段：

```powershell
& "$env:ANDROID_SDK_ROOT\ndk\29.0.14206865\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-objdump.exe" `
  -p .\libffmpegJNI.so |
  Select-String -Pattern "LOAD"
```

每个 LOAD 段的 align 不应低于 `2**14`。再验证最终 APK：

```powershell
& "$env:ANDROID_SDK_ROOT\build-tools\35.0.0\zipalign.exe" `
  -v -c -P 16 4 .\app-release.apk
```

还应在 16 KiB Android 15+ 设备/模拟器上确认：

```text
adb shell getconf PAGE_SIZE
```

输出应为 `16384`，然后实际播放每种启用格式。不能只验证库能加载；解码、seek、切歌、后台播放和异常文件都要覆盖。

## 验收清单

- APK 只有预期 ABI，且包含 `libffmpegJNI.so`。
- `FfmpegLibrary.isAvailable` 为 true，version 可读。
- 每个声明支持的 MIME 都通过 `supportsFormat` 和真实样本播放。
- 不支持格式能回退或给出明确错误。
- ELF LOAD、RELRO 与 APK ZIP 对齐通过 16 KiB 检查。
- Media3/FFmpeg/NDK/decoder/命令/哈希可追溯。
- 已审查 FFmpeg 配置对应的 LGPL/GPL 与 codec 专利义务。

FFmpeg 不是普通 Kotlin 依赖：产物兼容性、许可证和安全更新都需要独立维护。把重建过程放入可审计 CI，比继续手工覆盖 `.so` 更可靠。
