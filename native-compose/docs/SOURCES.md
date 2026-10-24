# 适配源码与上游缓存

`adaptation/` 是主仓库维护并提交的代码，目录名使用官方 Maven 坐标的 `group_artifact`，不使用移植后的 group。`vendor/` 是 Git 忽略的下载源码、准备结果及构建输出。

```text
native-compose/
  adaptation/
    org.jetbrains.skiko_skiko/
      build.gradle.kts
      src/
        windowsMain/kotlin/     # Windows layer、D3D12、Win32 actual
        windowsMain/cpp/        # D3D12 C++ bridge
        linuxMain/kotlin/       # Linux layer、Vulkan、OpenGL、CPU actual
        macosMain/kotlin/       # macOS layer、Metal、AppKit actual
        nativeMain/kotlin/      # 共享 Native 适配
        nativeJsMain/cpp/       # 共享 C++ 适配、Linux Vulkan WSI
        darwinMain/kotlin/
    org.jetbrains.skiko_skiko-skottie/
      build.gradle.kts          # 缺失的 Windows target、Core API 项目依赖
  vendor/
    skiko/
      sources/                 # 经校验的原始官方/社区源码
      prepared/                # 可重现的完整上游构建
      skia/                    # 经校验的 Skia 下载缓存
      state.json
    build/
      native-compose/          # 验证 Maven 仓库等根项目输出
      samples/                 # 示例的编译、CInterop、运行暂存和 ZIP
      maven-consumer/           # 独立消费者输出
      logs/                    # 本地验证日志
    sdl-*/                     # SDL SDK 和源码缓存
  samples/                     # 本项目维护的验证 host
  scripts/                     # 同步、上游构建兼容与验证工具
  upstream/                    # 版本/提交/校验锁及来源映射
  docs/
  licenses/
```

## 直接维护和编译的平台源码

Core 的 37 份适配源文件直接在 `adaptation/org.jetbrains.skiko_skiko/src/` 中维护。其中 34 份是此前通过锁定社区差异复用的相关文件，已成为本仓库维护的适配源码；来源映射记录在 `upstream/adaptation-origins.json`，继续保留原有许可与第三方声明。新增的 3 份文件是 `LinuxVulkanWindow.kt`、`LinuxVulkanRenderer.kt` 和 `LinuxVulkanWindow.cc`。

`windowsMain`、`linuxMain`、`macosMain` 是实际中间源集，由 `mingwX64Main`、`linuxX64Main`、`macosArm64Main` 继承，无需给每个具体 target 重复一份 actual。

Windows 入口为 `src/windowsMain/kotlin/org/jetbrains/skiko/SkiaLayer.windows.kt`，Linux 对应 `src/linuxMain/kotlin/org/jetbrains/skiko/SkiaLayer.linux.kt`。D3D12 C++ bridge 位于 `src/windowsMain/cpp/WindowsDirect3DBridge.cc`；Linux Vulkan WSI 位于 `src/nativeJsMain/cpp/LinuxVulkanWindow.cc`，由平台宏保护。

每个适配目录的 `build.gradle.kts` 是实际执行的适配配置：准备后的上游模块应用它，由它注册缺失 target、绑定维护的 source roots、接入静态 C++ 编译和 sources 发布。继续复用上游 `buildSrc` 的编译和发布任务，避免另写一套 Skiko 构建系统。

Kotlin 和 C++ 编译直接读取 `adaptation/`；准备过程只移除 vendor 中同路径的遮蔽文件，不把维护源码复制成 vendor 中的编辑副本。原始下载源码始终保留。

Skottie 已有官方共享 Native Kotlin/C++ 实现，当前无需重复复制或生成空的 `windowsMain`。其适配模块负责新增 Windows target 和 Core 的 `api(project(":"))` 依赖；以后需要手写的实际差异再加入本模块的 `src/`。Graphite 未接入；JVM/AWT runtime 不在本轮 Native 迁移范围内。

## 主工程和 IDE

在 `ylcs-kmp` 开启 `native.compose=true` 后同步 Gradle，Settings 关联 vendor 中的完整 `skiko` included build；其适配 source roots 指向上述 `adaptation/`。仍从主工程执行示例任务，不需要单独打开适配目录或手动关联新 Gradle build。

官方共享 API、已有 Native 实现和构建工具可在完整 vendor checkout 中查看。当前构建位置由 `vendor/skiko/state.json` 的 `build` 字段记录，路径相对于 `native-compose/`。版本升级时审核固定提交、相关公共 API 差异和维护源码；同步脚本不会自动覆盖 `adaptation/`。维护源码和构建脚本的路径及内容参与源指纹，修改后由根 Gradle 重新准备。

普通消费模式 `native.compose=false` 保持早返回，不配置这些源码构建；手动安装桥插件的 Native 消费者按 target 选择 Maven 工件。迁移目录不会改变这一开关的职责。
