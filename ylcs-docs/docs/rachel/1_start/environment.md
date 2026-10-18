# 编译环境与工程结构

Rachel 使用仓库根目录的 Gradle Wrapper，以及 `buildSrc` 中的统一构建模板。环境配置应与当前源码保持一致。

## 版本基线

| 项目 | 当前配置 | 定义位置 |
| --- | --- | --- |
| Gradle Wrapper | 9.8.0 | `gradle/wrapper/gradle-wrapper.properties` |
| Kotlin | 2.5.0-Beta1 | `libs.versions.toml` |
| Compose Multiplatform | 1.13.0-alpha02 | `libs.versions.toml` |
| Android Gradle Plugin | 9.3.3 | `libs.versions.toml` |
| Ktor | 3.6.0 | `libs.versions.toml` |
| Coroutines | 1.11.0 | `libs.versions.toml` |
| Lifecycle | 2.11.0 | `libs.versions.toml` |
| Navigation 3 | 1.2.0-rc01 | `libs.versions.toml` |
| JDK / JVM target | 25 | `Constants.kt` |
| Android | minSdk 29、compileSdk 37.1、targetSdk 37 | `Constants.kt` |
| Android NDK / ABI | 29.0.14206865 / `arm64-v8a` | `Constants.kt` |
| iOS deployment target | 26.0 | `Constants.kt` |
| 应用版本 | 3.7.0，versionCode 370 | `Constants.kt` |

IDE 的 Gradle JVM 与命令行构建均需使用 JDK 25。Gradle 版本由 Wrapper 管理。上述表格记录源码配置，不代表所有工具链组合均已在当前主机验证。

## 工程结构

```text
ylcs-kmp/
├─ buildSrc/                 # 构建模板与任务
├─ ylcs-module/              # 公共框架模块
├─ ylcs-app/
│  ├─ app/                   # 共享业务模块
│  ├─ android-app/           # Android 入口
│  ├─ desktop-app/           # JVM Desktop 入口
│  ├─ web-app/               # JS / WasmJS 入口
│  ├─ iosApp/                # Swift / Xcode 宿主
│  ├─ cs/                    # 业务共享契约
│  ├─ server/                # Kotlin/Native 服务端
│  ├─ gallery/               # UI 组件示例
│  ├─ modern-screen/         # 导航示例
│  ├─ mod/                   # MOD 格式与共享实现
│  └─ mod-manager/           # MOD 桌面工具
└─ ylcs-docs/                # MkDocs 指南与生成页面入口
```

新增 Gradle 模块时，需在 `settings.gradle.kts` 中注册，并按照相邻模块使用 `install`、`template` 与 `lib` DSL。依赖通过版本目录与 `projects.*` 引用声明。

## 目标平台与主机限制

`KotlinMultiplatformTemplate` 按模块选项配置客户端及桌面原生目标。iOS 默认配置设备 `iosArm64`，仅在 Apple Silicon macOS 主机上增加 `iosSimulatorArm64`。Intel iOS 模拟器目标未启用。

桌面原生目标分别为 `mingwX64("windows")`、`linuxX64("linux")` 和 `macosArm64("macos")`。模块可关闭不需要的目标；原生编译、链接和打包仍受宿主及依赖工具链限制。iOS 构建需要 macOS 与 Xcode。

涉及 C/C++、Rust 或预编译库的模块，应按 `.gitmodules` 初始化所需子模块，并参考 `.github/actions/setup-build/action.yml` 准备工具。预编译库可用不代表已完成对应原生源码的重新构建。

## 源集组织

仓库关闭 Kotlin 默认层级模板，显式建立源集依赖关系。源集名称相同不代表所有模块具有相同继承关系。

| 源集 | 典型用途 |
| --- | --- |
| `commonMain` | 全部已启用目标共享的代码 |
| `clientMain` | 模块内显式定义的客户端或非 Web 共享层，范围由依赖关系决定 |
| `jvmMain` | Android 与 JVM Desktop 共享实现 |
| `webMain` | JavaScript 与 WasmJS 共享实现 |
| `nativeMain`、`posixMain` | 原生或 POSIX 能力共享层 |
| `appleMain`、`iosMain` | Apple 平台或 iOS 共享实现 |
| `skikoMain` | 依赖 Skiko 的共享实现 |
| `androidMain`、`desktopMain` 等 | 平台专属实现 |

共享逻辑应放置在实际可用的最上层公共源集中。平台类型与系统调用通过平台源集或 `expect/actual` 隔离。

## 依赖导出与发布坐标

`lib(...)` 中，`ExportLib` 之前为实现依赖，之后为向消费者导出的 API 依赖。调整位置会改变消费者可见的类型范围。

Maven 发布坐标由模块路径和应用版本名生成，例如：

```text
:ylcs-module:compose:app         → love.yinlin.compose:app:3.7.0
:ylcs-module:foundation:network  → love.yinlin.foundation:network:3.7.0
:ylcs-module:cs:client-engine    → love.yinlin.cs:client-engine:3.7.0
```

坐标生成规则不证明某个版本已经发布。仓库内部开发优先使用项目依赖；仓库外使用前应确认目标仓库中的实际产物。

## 常用命令

以下命令在仓库根目录执行，任务名称已对应当前构建脚本：

```powershell
# UI 组件展厅
.\gradlew.bat :ylcs-app:gallery:galleryRun

# 导航示例
.\gradlew.bat :ylcs-app:modern-screen:modernScreenRun

# 完整桌面应用
.\gradlew.bat :ylcs-app:desktop-app:desktopRunDebug

# WasmJS / JavaScript 开发服务
.\gradlew.bat :ylcs-app:web-app:webRun
.\gradlew.bat :ylcs-app:web-app:webJsRun

# Android Debug 包
.\gradlew.bat :ylcs-app:android-app:androidPackage

# 查询原生服务端任务
.\gradlew.bat :ylcs-app:server:tasks --all
```

Linux 与 macOS 使用 `./gradlew`。具体任务是否能执行取决于当前宿主、SDK 和模块配置。

### iOS 宿主与归档

首次打开 Xcode 工程或修改 SwiftPM 依赖后，在 macOS 的仓库根目录执行：

```bash
XCODEPROJ_PATH="$PWD/ylcs-app/iosApp/iosApp.xcodeproj" \
  ./gradlew :ylcs-app:app:portal:integrateLinkagePackage
```

此任务为 `portal` 及其传递依赖生成 `ylcs-app/iosApp/KotlinMultiplatformLinkedPackage`，供 Xcode 链接 MMKV 等 SwiftPM 原生依赖。该目录属于生成内容，不提交到版本控制。

随后打开 `ylcs-app/iosApp/iosApp.xcodeproj`。工程会在 Swift 编译前调用 `:ylcs-app:app:portal:embedAndSignAppleFrameworkForXcode`，根据 Xcode 当前配置及架构构建 framework。Debug 和 Release 的搜索路径均指向 `portal/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`；Scheme 的前置脚本负责生成版本号配置。

CI 的 `.github/actions/package-ios` 会先生成 SwiftPM 链接包，再以 Release 配置归档 iOS 真机应用并打包未签名 IPA。仅执行 `linkDebugFrameworkIosArm64` 不会验证 Swift 宿主编译和 Xcode 归档。

### Linux Release 与 AOT

桌面 Release 保留 AOT 预构建，训练阶段会实际启动桌面应用，因此无显示服务的 Linux CI 需要提供虚拟 X11 显示。`.github/workflows/release.yml` 的 Linux 打包任务和 `.github/workflows/release_linux.yml` 均启用 `virtual-display`，由共用环境 action 安装 `xvfb`、`xauth`，再使用 `xvfb-run` 执行原有 Gradle 发布任务。

这两个构建步骤通过 `SKIKO_RENDER_API=SOFTWARE_FAST` 使用软件渲染，避免依赖 CI 的 GPU 环境。该环境变量仅作用于 CI 构建过程，不写入发行包的 JVM 参数。`virtual-display` 默认关闭，其他构建入口按需启用。

## 文档生成

`.github/workflows/docs.yml` 依次生成 Gallery、ModernScreen 与 Dokka 页面，再由 MkDocs 发布站点。对应的生成任务为 `:ylcs-app:gallery:galleryPublish`、`:ylcs-app:modern-screen:modernScreenPublish` 与 `dokkaGenerateHtml`。

`ylcs-docs/docs/overrides/` 中的相关页面属于生成内容。修改指南应编辑 Markdown；修改生成行为应编辑源代码或构建模板。

## 源码入口

- `buildSrc/src/main/kotlin/KotlinTemplate.kt`
- `buildSrc/src/main/kotlin/KotlinMultiplatformTemplate.kt`
- `buildSrc/src/main/kotlin/KotlinNativeTemplate.kt`
- `buildSrc/src/main/kotlin/love/yinlin/project/Constants.kt`
