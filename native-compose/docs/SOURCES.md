# 适配源码与构建归属

`adaptation/` 保存提交到 Git 的维护代码；`vendor/` 保存忽略的原始源码、准备后的 fork、SDK 和输出。

```text
native-compose/
  adaptation/
    org.jetbrains.skiko_skiko/
      build.gradle.kts
      src/
        commonMain/kotlin/org/jetbrains/skiko/window/
        nativeMain/kotlin/       # 共享 Native 实现、SDL 窗口循环
        nativeMain/cpp/          # Image、GL interface 等共享 bridge
        windowsMain/kotlin/      # Windows/D3D12，包括窗口 backend
        windowsMain/cpp/
        linuxMain/kotlin/        # Linux/Vulkan，包括窗口 backend
        linuxMain/cpp/           # Vulkan WSI、Linux C++ ABI 兼容
        macosMain/kotlin/        # macOS/Metal，包括窗口 backend
        darwinMain/kotlin/
        nativeInterop/cinterop/sdl.def
    org.jetbrains.skiko_skiko-skottie/
      build.gradle.kts
    samples/skiko/              # 当前阶段：公共示例、Native 回归
  vendor/
    skiko/sources/              # 校验过的原始上游/社区源码，不改名
    skiko/prepared/             # 重现并规范化的 Native-only fork
    sdl-source-*/               # 校验过的 SDL 源码
    sdl-static/<konan-target>/  # SDL 静态 SDK、系统链接依赖清单
    build/
      logs/
      samples/
      native-compose/          # 本地验证 Maven 仓库
      maven-consumer/
  scripts/
  upstream/
  docs/
  licenses/
ylcs-app/native-compose/
  build.gradle.kts
  src/commonMain/kotlin/       # 只有业务场景与公共窗口 API 调用
```

## 共享代码统一到 Native

维护代码与准备后的活动源集都使用 `nativeMain/nativeTest`。
同步脚本合并上游历史共享目录，删除多余的 Native/JS hierarchy 节点，并对重名文件检查冲突。
只读的原始上游缓存及来源映射保留原路径，便于升级比较；它们不构成本项目的活动源集。

维护的 Image/GL C++ 放在 nativeMain；Linux Vulkan WSI 和 ABI 兼容放在 linuxMain。
`upstream/adaptation-origins.json` 将维护文件映射回原始社区路径，同步时按映射移除 vendor 的遮蔽文件。
不将手写代码复制成另一份可编辑的 vendor 副本；编译直接读取 adaptation。

## 窗口与 SDL 是库的实现

公共 API 位于 `org.jetbrains.skiko.window`：NativeWindow、NativeWindowOptions、NativeWindowBackend、NativeWindowKey。
NativeWindow 在 Native 层拥有 SDL 事件循环，内部接 Windows D3D12、Linux Vulkan、macOS Metal；软件基线也由库提供。

SDL CInterop 的定义、平台 adapters 和静态链接全部属于 Skiko Core。
生产者从锁定源码构建 SDL，Windows 与 Skia 共用 MSVC 静态 CRT 兼容方案，并过滤不兼容的 COFF directives。
SDK 静态归档与系统 linkerOpts 进入发布的 SDL CInterop KLIB。
消费方不配置 CInterop、不编译 SDL、不需要 SDL SDK 或平台窗口源码。

APP 只在 commonMain 引用官方 Skiko/Skottie 坐标并调用公共 API。
桥插件做 target 重定向、ICU 收集和启动/打包，不再承担 SDL SDK 下载、链接或 DLL 收集。
许可证保留在源码目录与 sources 工件中，不复制到程序暂存目录或 ZIP。

## Gradle 和 IDE

两个适配库属于同一个 Skiko included build。Settings 的 buildFileName 将 adaptation 的 build.gradle.kts
指定为正式项目脚本，项目目录仍在 vendor，以复用上游 buildSrc 和相对路径。
上游基础配置自动生成为 vendor 内的 rachel-upstream.gradle.kts。
不需要将库重复 include 为 ylcs-kmp 的普通子项目。

窗口绑定只在 Core 中 commonize；APP 没有额外的 CInterop 源集需求。
改变准备规则后 Reload All Gradle Projects，让 IDE 导入新的指纹目录。
维护开关仍默认 false；普通模式不准备或关联这些源码构建。

桥插件的两个生命周期入口统一维护在 build-logic/native-bridge：src/main 是 Project 实现，
src/settings 是不依赖 KGP 的 Settings 实现。发布时 Settings marker 选择同一 GAV 的 settings classifier。
规则及 adaptation 路径可由消费模块的 nativeBridgeReport 查看；实际依赖选择使用 dependencyInsight。

## Skottie 为什么只有构建脚本

adaptation 只保存本项目维护的差异，不保存完整上游副本。
Skottie 的公共 API、共享 Native Kotlin 实现和 C++ bridge 已可复用，当前没有新增平台 Kotlin/C++ 文件。
adaptation/org.jetbrains.skiko_skiko-skottie/build.gradle.kts 注册 Windows target、
关联 Core 的 Native 符号输入并以 API 依赖 Core，继续应用生成的上游构建脚本。

实际源码位置以 vendor/skiko/state.json 的 build 字段为准：

```text
vendor/skiko/prepared/<fingerprint>/skiko/skiko-skottie/src/
  commonMain/kotlin/org/jetbrains/skia/skottie/  # Animation、AnimationBuilder 等 API
  nativeMain/kotlin/org/jetbrains/skia/skottie/  # Native 文件入口、静态加载实现
  nativeMain/cpp/skottie/                      # Animation/AnimationBuilder/Logger bridge
  nativeMain/cpp/sksg/                         # InvalidationController bridge
```

本项目的功能回归位于 adaptation/samples/skiko/src/nativeTest，涵盖实际时间轴像素变化及无效输入。
如果后续发现平台实现缺口，手写修复再加入此 adaptation 模块的对应源集；现有上游代码不重复复制。

Graphite 按确定的路线排除；AWT/JVM runtime 不迁移。
