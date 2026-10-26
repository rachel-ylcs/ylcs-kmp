# Rachel Native desktop / Compose

可选的桌面 Native 移植模块组，当前覆盖 Compose UI 的八个工件、Foundation/Animation 的五个运行时工件、Resources 与 SDL ComposeScene 宿主，
基于已有 Skiko Core + Skottie、静态 SDL 窗口宿主及桥插件。目标：mingwX64、linuxX64、macosArm64。
默认 GPU：Windows Ganesh/D3D12，Linux Ganesh/Vulkan，macOS Ganesh/Metal。
Graphite 按已确定的路线排除。Compose Runtime 复用官方 Native 工件。
UI 阶段见 [COMPOSE_UI.md](docs/COMPOSE_UI.md)，当前移植与构建边界见 [FOUNDATION_ANIMATION.md](docs/FOUNDATION_ANIMATION.md)。
资源运行时、生成和部署见 [COMPOSE_RESOURCES.md](docs/COMPOSE_RESOURCES.md)。
全部十六个已移植工件在 macOS 也由本项目维护和重定向，覆盖官方主坐标及显式 macosarm64 坐标；核验与保留边界见 [MACOS_AUDIT.md](docs/MACOS_AUDIT.md)。

当前范围、已验收的平台及待完成项见 [STATUS.md](docs/STATUS.md)，实际检查见 [VALIDATION.md](docs/VALIDATION.md)。

## 维护模式与普通消费

根 Settings 以一句 `nativeCompose()` 接入，逻辑在 build-logic/repository-conventions。
`native.compose` 默认 false，只控制移植源码维护，不控制业务模块是否声明 Native target。

| 配置 | 行为 |
| --- | --- |
| false，JVM/Android 等原有 target | 保持官方依赖，不下载桌面 Native 移植 KLIB |
| false，显式安装桥插件并声明匹配的 Native target | 使用已发布 Maven 工件，不准备 vendor 或 SDL SDK |
| true，移植维护 | 重现源码、覆盖构建版本、适配、编译、测试、验证发布，依赖直接选择源码 project |

模块是否安装桥插件由维护者手动决定；不会给 ylcs-module:core 自动安装。
需要移植依赖的模块安装后按“库 + target”重定向。
普通消费者不需要检出 native-compose；SDL 已在生产者的 KLIB 内，消费不要求 CMake/MSVC/SDL SDK。
Windows 的 ICU 数据仍由插件从配套 Maven classifier 收集。

在根 local.properties 增加以下本机配置，再 Reload All Gradle Projects：

```properties
native.compose=true
```

命令行 -Pnative.compose 优先，PowerShell 请将带点号的 -P 参数整体加引号。
开启维护模式不等于上传公共 Maven；没有源码项目映射时会失败，禁止退回 Maven。

## 版本与源码边界

版本取自根 libs.versions.toml；Gradle Wrapper、JDK 配置复用根工程。
当前 Kotlin 2.5.0-Beta2、Skiko 0.156.0、SDL 3.4.18、JDK 25。
移植工件版本与官方库相同，通过 group 区分来源；没有独立 native 版本后缀。

共享依赖按根目录坐标/版本自动覆盖到准备后的 fork，所有 fork Wrapper 也复用根工程。
上游独有而根目录没有声明的工具依赖保留原版本。
实际源码 release 改变仍须审核提交、Skia/SDL 与校验锁，不能把旧源码换个版本号发布。

维护模式以根选择的 Skiko 版本为准：较旧的普通数值请求，例如 CMP 的 0.150.1，
可以统一到已选择的 0.156.0；更新请求不会静默降级。
strictly/reject 冲突、未支持的动态和范围请求明确失败。
这项解析策略不构成所有旧版 API/ABI 自动兼容的承诺；普通模式使用所安装插件的兼容版本。

- adaptation/：按依赖坐标组织提交的 Kotlin/C++、CInterop 定义、构建配置和库内回归测试，不维护 samples。
- vendor/：忽略的上游源码、准备后的 fork、静态 SDK、输出及 logs。
- 上游历史共享代码由脚本合并到 nativeMain/nativeTest，活动源集不使用 Native/JS 名称。
- 当前仅有 macosArm64，Darwin 共享层统一合并到 macosMain/macosTest；三个平台层为 windows/linux/macos。
- 两个库属于同一个 Skiko included build；正式 buildFile 指向 adaptation，继续复用上游 buildSrc。
- Compose UI、Foundation、Animation 共用一份 vendor/compose/workspace 稀疏 checkout 和 compose-ui included build。
- Resources 来自另一个官方仓库，保存在 vendor/resources/workspace，仍加入同一 included build。

详见 [SOURCES.md](docs/SOURCES.md)。不要手改 vendor。

## 独立 APP 的使用方式

ylcs-app/native-compose 只包含 commonMain 的场景与公共窗口 API 调用。
build.gradle.kts 复用 Rachel nativeExecutable DSL，安装 composeCompiler，当前 commonMain 只需声明：

```kotlin
lib(libs.compose.foundation, libs.compose.resources)
```

Foundation 的 API 依赖会带入 Animation、UI 与 Skiko，无需逐个列出 UI 子工件或调用 asProvider()。
Resources 额外声明自身运行时并应用 composeMultiplatform 插件，沿用官方 Res 生成与资源变体；Native 桥自动同步和打包 composeResources。
只使用 UI 原语时可声明 lib(libs.compose.ui)；animation-graphics 属于可选工件，使用其 API 时再声明。
窗口和运行目录配置沿用：

```kotlin
nativeApplication {
    skikoWindow()
}
```

APP 没有 sdl.def、nativeInterop 或各平台窗口代码。
androidx.compose.ui.window.NativeWindow 提供 Composable 入口，基于 Skiko NativeWindow.open 的窗口、事件与平台 renderer。
这是本移植库新增的桌面 Native API；当前 Native-only APP 可直接从 commonMain 调用。
混合 JVM/Android 的工程应将该窗口入口放在桌面 Native 源集，公共绘图/UI 仍保持跨平台；
桥插件不改写其他平台来伪造这项新 API。

预览展示 Foundation 布局、clickable、BasicTextField、SelectionContainer、LazyColumn，以及状态动画、AnimatedVisibility、animateContentSize 和 Dialog；Escape 由弹层和 Compose 返回处理。
“资源”页展示字符串、数组、复数、PNG/SVG/XML 矢量和原始文件。本轮仅构建和检查资源产物，尚未运行资源预览。

## 构建与运行

从 ylcs-kmp 根目录，Gradle JVM 使用 JDK 25。Windows 生产者使用现有 VS_PATH，例如 E:\VS2026。
维护模式首次构建自动准备锁定源码与静态 SDL；SDL 使用 CMake，Windows 使用 MSVC/Ninja，
Linux 的 SDL 使用当前 Kotlin/Native 自带的 Clang/sysroot，系统提供桌面开发包及 pkg-config；
macOS 使用 Apple Silicon/Xcode。
可先手动执行：

```powershell
python native-compose/scripts/sync_upstream.py
python native-compose/scripts/prepare_sdl.py --build
python native-compose/scripts/doctor.py
```

Linux 的 prepareSdlStatic 会先依赖 downloadKotlinNativeDistribution，再读取该构建的 konan.txt；
手动执行 prepare_sdl.py --build 时也会按需准备同一发行包，避免 SDL 引用比 Kotlin/Native 更新的 glibc ABI。
CI 准备步骤只获取源码、检查宿主工具，SDK 编译由库的 Gradle 任务管理。

Skiko 的唯一工作源码位于 vendor/skiko/workspace，Gradle/IDE 始终引用其中的 skiko 子目录。
上游、参考实现和共同基线只缓存锁定归档；需要更新时临时合并并原地同步生成文件。
维护源码的内容变化不再产生新指纹目录；工作目录准备成功后会清理可识别的旧 prepared/sources 缓存。

```powershell
# 独立 APP
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:runWindowsDebugExecutable
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:runWindowsReleaseExecutable
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:packageWindowsReleaseExecutable

# 当前 UI 库内回归
.\gradlew.bat "-Pnative.compose=true" :compose-ui:ui:windowsTest :compose-ui:ui-backhandler:windowsTest

# 既有 Skiko 库内回归（当前 CI 阶段不执行）
.\gradlew.bat "-Pnative.compose=true" :skiko:mingwX64RegressionTest :skiko:skiko-skottie:mingwX64RegressionTest
```

Linux/macOS 在对应宿主将 Windows/windowsTest 替换为 Linux/linuxTest 或 Macos/macosTest，
使用 ./gradlew。-Pnative.renderer=software 选择 CPU 基线；默认 gpu。
加 -Pnative.smoke=true 会绘制、缩放、退出，便于自动化。

IDE 的 Native Compose 分组提供 nativeComposeShowcase（Debug）和 nativeComposeShowcaseRelease。
应用打包输出在 ylcs-app/native-compose/build/distributions，
UI 输出在 native-compose/vendor/build/compose-ui；临时日志统一在 vendor/build/logs。
分发不再含 SDL 动态库，Windows 保留 icudtl.dat；插件不再复制许可证到程序暂存目录或 ZIP。
来源与许可证仍保存在源码目录及 sources 工件中。详细边界见 [RUNTIME.md](docs/RUNTIME.md)。

根 gradle.properties 的 kotlin.native.ignoreDisabledTargets 会由同步脚本传给准备后的 included build。
独立 Maven 验证工程也配置此属性；仅在主工程设置不会自动影响独立构建。
Skiko 的生成构建另外通过 kotlin.suppressGradlePluginWarnings=CrossCompilationWithCinterops
定向抑制不匹配宿主的 CInterop 提示。三个 target 和 KLIB 交叉编译配置保留，
每个平台的 C++/CInterop 与最终链接仍由对应宿主 CI 验证；普通业务工程不注入此设置。

## 桥插件与重定向定位

实现统一放在 build-logic/native-bridge 一个模块，保留两个适用阶段不同的插件 ID：

- love.yinlin.native-bridge：Project 插件，提供 target 重定向和 nativeApplication；业务模块使用此入口。
- love.yinlin.native-bridge.settings：Settings 插件，登记 included build 源码项目；普通 Maven 消费不需要安装。

Settings 实现位于同一模块的 src/settings，生成轻量的 settings classifier。
两种入口共享同一发布坐标；Settings marker 选择轻量产物并排除 KGP 传递依赖，
避免在 Settings 类路径提前加载 Kotlin Gradle Plugin。原 native-bridge-settings 独立模块已合并。
主仓库仍由 nativeCompose() 自动管理 Settings 入口，无需在业务 build.gradle.kts 安装两遍。

查看配置好的规则和维护源码位置，报告任务本身不解析业务依赖：

```powershell
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:nativeBridgeReport
```

核对实际 Native compile 依赖图是否选中自有源码/Maven 工件，可执行 verifyNativePortDependencies；该任务不编译或运行目标程序。

| 上游坐标 | Native targets | 维护源码 / included project |
| --- | --- | --- |
| org.jetbrains.skiko:skiko | mingw_x64、linux_x64、macos_arm64 | adaptation/org.jetbrains.skiko_skiko，skiko build 的 : |
| org.jetbrains.skiko:skiko-skottie | 同上 | adaptation/org.jetbrains.skiko_skiko-skottie，skiko build 的 :skiko-skottie |

规则定义在 NativeBridgePlugin，源码映射定义在 repository-conventions/NativeCompose.kt。
正式脚本直接使用表中的 adaptation/build.gradle.kts，vendor 只是生成的项目目录。
nativeBridgeReport 展示配置规则；核对某个依赖实际被选中的结果使用 Gradle 自带报告：

```powershell
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:dependencyInsight --configuration windowsCompileKlibraries --dependency org.jetbrains.skiko
```

普通模式中相同报告会显示 Maven 坐标；混合 JVM/Android 工程中只有匹配的 Native target 应用这些规则。

## Maven 生产与独立消费

```powershell
.\gradlew.bat "-Pnative.compose=true" :native-compose:publishSkikoToVerificationRepository
.\gradlew.bat :build-logic:native-bridge:publishAllPublicationsToLocalVerificationRepository
python native-compose/scripts/verify_repository.py --target mingw_x64
.\gradlew.bat -p native-compose/verification/maven-consumer "--refresh-dependencies" "-Pnative.window=true" "-Pnative.legacyRequest=true" runWindowsDebugExecutable
```

Core/Skottie 验证仓库在 vendor/build/native-compose/repository，
插件验证仓库在 build-logic/build/native-bridge-repository。
这是为验证发布元数据和独立消费创建的文件 Maven 仓库，不是 mavenLocal()/~/.m2，
不是维护模式构建或最终程序运行的前置条件。维护模式直接依赖源码项目；
正式发布后普通消费者从配置的远程 Maven 仓库取包，运行程序不访问 Maven。
独立消费者不 include 源码 build；commonMain 的窗口探针从 Maven 使用封装后的窗口 API，
不配置 SDL SDK、绑定或平台 adapter，并继续检查 Skottie 传递 Core 与旧版请求统一。
可用 -Pnative.repository 与 -Pnative.plugin.repository 指向其他解压后的 Maven 仓库。

Skiko 官方坐标在三个桌面 Native target 映射到 love.yinlin.native.skiko 的相同版本。
projectPath 规则可直接引用同一 build 的项目；included build 由 Settings 桥保存真实项目 selector。
发布自己的框架时仍须审核 POM/Gradle Module Metadata；消费替换不自动改写发布坐标。

完整发布包含根 metadata、target 主 KLIB、配套 CInterop、sources、许可证及对应数据；
不能用一个裸主 KLIB 代替整个 Maven publication。
更新检查只报告远程引用，不自动移动锁：

```powershell
python native-compose/scripts/sync_upstream.py --check-updates
```

## 渐进式 CI

.github/workflows/native-core.yml 验证当前 UI 阶段：
源码/发布工具、桥插件测试、72 项 UI/Runtime 回归、真实 Composable APP 编译链接与 dummy/software smoke、
八模块三宿主 Maven 汇总与独立 Maven 消费者（官方请求和显式 Native 坐标各 6 项）。
本轮 CI 使用离屏测试及 SDL dummy/CPU smoke，不启动 GPU 窗口；IME、Wayland、多显示器和驱动行为仍需真机验收。

工作流只生成本地验证 Maven 文件并上传为 Actions 工件，不向公开 Maven 服务发布。
Skiko/Skottie 和桥插件发布任务只接受 file: 仓库，上游 ComposeRepo 发布配置由准备脚本移除。

第三方来源由 upstream/lock.json、upstream/sdl.json 和 upstream/adaptation-origins.json 记录。
源代码包所需声明从锁定的上游源码直接取得，来源说明由脚本生成在 vendor 中，不维护额外的 licenses 副本。
图形后端与社区参考见 [RENDERING.md](docs/RENDERING.md)。
