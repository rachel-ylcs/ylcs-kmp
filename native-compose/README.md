# Rachel Native Compose bootstrap

这是 `ylcs-kmp` 主工程中可选的 Native desktop 模块组。当前实施范围为 Skiko 核心、Native Bridge、
离屏绘图和 SDL 窗口示例。示例使用根 Wrapper、版本目录和 `install(...)`；Gradle 和编译工具使用 JDK 25。
桥插件位于现有 `build-logic` Build 内，上游 Skiko 则由根 Settings 以 included build 关联。

开启下述本机开关后，在 IDEA 中打开 `ylcs-kmp`，执行 Reload All Gradle Projects，
即可从同一工程访问示例和插件源码。
不需要单独链接 `native-compose`。示例任务以 `:native-compose:adaptation:samples:` 开头。
运行配置的 `Native Compose` 分组提供 `nativeSkikoOffscreen` 和 `nativeSkikoWindow`，均从主工程启动。

本阶段构建 Skiko Core 和独立的 Skottie 扩展，目标为 `mingwX64`、`linuxX64`、`macosArm64`。
窗口示例默认选择 Ganesh：Windows D3D12、Linux Vulkan、macOS Metal，并检查实际后端，禁止将回退当作通过。
通过 `-Pnative.renderer=software` 可运行独立的 CPU 光栅基线。Graphite 按当前项目路线排除，
Compose UI 和 Rachel 业务模块尚未接入。源码、编译和各宿主运行是不同的验收，详见
[渲染说明](docs/RENDERING.md) 和 [源码位置](docs/SOURCES.md)。

## 维护模式与消费模式

根 `settings.gradle.kts` 通过一句 `nativeCompose()` 接入。
维护开关读取、示例注册及源码关联集中在
`build-logic/repository-conventions/src/main/kotlin/love/yinlin/repository/NativeCompose.kt`，
由仓库约定复用通用 Settings 桥插件。

`native.compose` 是移植源码维护开关，默认 `false`，不表示是否启用 Native desktop。
`ylcs-module`、`ylcs-app` 的实际目标仍由各模块自己的 Kotlin/模板配置决定，既有 `projects.*` 依赖不改写。
需要重定向依赖的模块手动安装桥插件，再按每个依赖、每个实际 target 判断是否需要移植工件。
仅声明 Native target 不要求桥插件；纯 Kotlin 的 `ylcs-module:core` 等模块维持自身目标和依赖配置。
`compose:core` 等依赖移植 UI 库的模块，由维护者在对应 `build.gradle.kts` 显式安装桥插件。
桥插件的坐标来自根版本目录，构建插件本身不读取 `native-compose` 版本锁或 vendor。

只有维护 Skiko/Compose 等移植源码时，在根目录 `local.properties` 的原有配置中增加：

```properties
native.compose=true
```

此文件已被 Git 忽略，不会替其他使用者开启。删除该项或设为 `false` 后重新同步 Gradle，即回到普通消费模式。
命令行 `"-Pnative.compose=true"` 或 `"-Pnative.compose=false"` 优先于本机配置，便于临时构建和 CI 验证。

| 使用者 / 配置 | 依赖与维护行为 |
| --- | --- |
| 普通业务开发者，`false`，仅 JVM/Android/iOS/Web 等原有 target | 保持官方上游依赖，不解析 desktop Native 移植 KLIB |
| 普通业务开发者，`false`，模块显式安装桥插件并声明 desktop Native target | 按 target 使用已发布的移植 Maven 工件；不关联或准备 vendor 源码 |
| 外部 Maven 消费者 | 不需要这个开关或检出本仓库；应用已发布桥插件后按 target 解析 Maven 工件 |
| 移植维护者，`true` | 开启移植模块和示例；重现锁定源码、适配、编译 KLIB、提供验证发布任务；适用依赖必须使用本地 project |

普通模式不注册 Native Compose 子项目、不读取 vendor 状态，也不触发源码或 Skia/SDL 准备；IDE 根模块排除该目录。
显式安装了桥插件但没有 Native target 的消费者只使用构建期插件，不会因此下载移植 KLIB。
当前已接入桥接的移植库为 Skiko Core 和 Skottie，后续 Compose 模块逐项增加规则；开关不能替代尚未完成的模块移植。

维护模式在 Settings 阶段运行可重现的 `sync_upstream.py`，复用校验过的缓存；首次会下载锁定源码和宿主 Skia。
`--offline` 也传递给准备脚本。普通编译直接选择 included build 中的 project 并连带构建，不经过 Maven 发布。
业务脚本可以继续声明官方坐标，坐标在这里是映射入口，解析结果为真实 project；同一 build 的库仍可直接使用
`projects.*` 或桥规则的 `projectPath`。缺少本地项目映射会失败，禁止退回 Maven。
源码维护范围包括 fork/适配、KLIB 构建、测试和发布任务，但不会因打开开关自动上传公共 Maven。
SDL SDK 仍通过 `prepare_sdl.py` 按需显式准备；普通模式应用使用自己的 SDK/发布运行文件，不访问 vendor。

原先的 `native.source` 二级模式已移除。需要验证 Maven 消费时，使用独立的
`verification/maven-consumer`，或普通模式加 `-Pnative.repository=<测试仓库>`；不再开启维护模式后选择 Maven。
`native.repository` 只有显式设置时才注册临时/私有仓库，普通消费默认使用已配置的正式 Maven 仓库。

## 版本和目录

已有依赖版本取自根目录 `libs.versions.toml`：Kotlin `2.5.0-Beta2`、Skiko `0.156.0`、
CMP `1.13.0-beta01`、SDL `3.4.18`；协程、AGP 和 Dokka 也与根目录一致。
移植库与对应官方库使用完全相同的版本号。`libs.skikoNative` 只是 Native 工件坐标别名，
直接复用 `version.ref = "skiko"`，没有独立 Native 版本或移植后缀；来源通过 Maven group 区分。
Skiko 的语言版本和 API 版本从 Kotlin 版本派生为 `2.5`，不会保留上游的 `2.2`。
桥插件版本跟随 Rachel `3.7.1`，JDK 和 Gradle Wrapper 沿用主工程。
构建插件与现有 `public-plugin` 一样使用 `kotlin-dsl`，其编译器及默认语言/API 级别由根 Wrapper
提供；目前 IDEA 为它们显示的语言/API `2.2` 与现有构建逻辑一致，不影响 Native 库和示例的 `2.5`。
`upstream/lock.json` 记录已审核的源码提交、三方合并基线、Skia 版本和校验值；
`upstream/sdl.json` 记录与版本目录匹配的 SDL 下载地址和校验值。
版本目录改变后，构建会要求重新审核并同步，不能静默沿用旧版源码。

| 目录 | 用途 |
| --- | --- |
| `upstream/` | 固定提交、SHA-256、需要移植的文件清单 |
| `docs/` | 验证记录、渲染后端说明 |
| `licenses/` | 许可证和第三方来源说明 |
| `scripts/skiko_adapt.py` | 对官方基线及社区差异的可审查适配 |
| `adaptation/<官方 group>_<官方 artifact>/` | 需要提交的适配构建脚本及 Kotlin/C++ 源码 |
| `scripts/` | 源码同步、SDL 准备、工具链诊断 |
| `vendor/skiko/sources/` | 校验过的原始源码，Git 忽略 |
| `vendor/skiko/prepared/` | 按输入指纹生成的 Skiko fork，Git 忽略 |
| `../build-logic/native-bridge/` | 依赖重定向、运行和打包插件 |
| `../build-logic/native-bridge-settings/` | 本地源码 Build 关联插件，无 KGP 运行依赖 |
| `samples/skiko-offscreen/` | Core 与 Skottie 集成测试、离屏绘图及 PNG 验证 |
| `samples/skiko-window/` | SDL 窗口及 Windows D3D12 / Linux Vulkan / macOS Metal 接入 |
| `vendor/build/` | 示例输出、临时 Maven、打包验证及统一 `logs/`，Git 忽略 |

不直接修改 `vendor/`。修改适配脚本或清单后重新执行同步；新输入生成新的目录，已有目录不会被递归清理。
同步会校验并缓存当前宿主的 Skia Release x64/ARM64 归档，生成新源码目录时复用已经校验的缓存。
IDEA 导入不再为同一个不可变归档重复请求网络，解压前仍会检查 SHA-256。
上游完整源码树不会被提交到主仓库；仓库保留适配、来源记录及必要许可证，发布的源码工件包含真实编译输入。
首次检出默认是普通消费模式。要维护源码或运行本目录 Native 示例，开启本机开关、准备 SDL，再重新同步 Gradle。

## 准备和运行

以下命令从 `ylcs-kmp` 根目录执行，先在 `local.properties` 中设置 `native.compose=true`。
Python 需要 3.11+，Git 必须在 PATH 中；Gradle 使用根 Wrapper。

```powershell
python native-compose/scripts/sync_upstream.py
python native-compose/scripts/prepare_sdl.py

# Windows: VS 安装根目录，不是 VC/Tools/MSVC/<version> 子目录。
$env:VS_PATH = 'E:\VS2026'
python native-compose/scripts/doctor.py

.\gradlew.bat :native-compose:adaptation:samples:skiko-offscreen:runWindowsDebugExecutable
.\gradlew.bat :native-compose:adaptation:samples:skiko-window:runWindowsDebugExecutable
.\gradlew.bat :native-compose:adaptation:samples:skiko-window:packageWindowsReleaseExecutable
```

要直接查看 Windows 窗口效果，也可以临时开启模块后运行：

```powershell
.\gradlew.bat "-Pnative.compose=true" :native-compose:adaptation:samples:skiko-window:runWindowsDebugExecutable

# CPU 基线（不用于证明 D3D12 已运行）
.\gradlew.bat "-Pnative.compose=true" "-Pnative.renderer=software" :native-compose:adaptation:samples:skiko-window:runWindowsDebugExecutable

# D3D12 两帧缩放测试；成功后自动退出
.\gradlew.bat "-Pnative.compose=true" "-Pnative.smoke=true" :native-compose:adaptation:samples:skiko-window:runWindowsDebugExecutable
```

窗口支持调整大小，关闭窗口后 Gradle 任务结束。此命令不启用 `native.smoke`，不会在两帧绘制后自动退出。

Windows 桥接使用 MSVC `cl.exe`、`lib.exe`、静态 CRT 及 Windows SDK。正常 Windows 构建不要求 clang-cl。
工具链沿用 `build-logic/public-plugin` 中 `BuildDesktopNativeTask` 的 `VS_PATH` 环境变量约定。
IDEA 启动时需能读取该变量；已配置系统环境变量的机器不必在终端重复设置。
保留 `SKIKO_VSBT_PATH` 作为上游兼容的显式覆盖，不需要新增本机路径配置文件。
Skia、C++ 桥接和匹配的 CRT 归档通过 `-include-binary` 进入 KLIB；不引入 AWT 或 JVM Skiko runtime JAR。
最终程序仍可能需要运行数据和其他 DLL，例如 `icudtl.dat`、窗口示例的 `SDL3.dll`。

`prepare_sdl.py` 默认同时准备 Windows SDK 和经过校验的 SDL 源码。
公共头文件可供 Windows 宿主为 Linux 生成 CInterop；每份绑定仍按对应 Konan target 的 ABI 生成，
不会将 Windows 的绑定或库作为其他平台产物。窗口代码放在 `desktopNativeMain`；Apple CInterop、
链接及运行需要 macOS 宿主，不能将 Windows 上的绑定生成结果视为 Apple 构建验证。
SDL 的窗口和渲染器继续作为不透明指针管理，`.def` 中的小型 C inline 包装负责调用原始 API，
避免依赖 IDE 对 `cnames` 前置声明的解析；相关上游问题见
[KTIJ-35493](https://youtrack.jetbrains.com/issue/KTIJ-35493) 和
[KTIJ-17810](https://youtrack.jetbrains.com/issue/KTIJ-17810)。资源仍按创建/销毁顺序在 `finally` 中释放。

`runWindowsDebugExecutable` 从 `vendor/build/samples/skiko-window/nativeApplication/windows/debugExecutable/` 启动；
离屏示例对应 `vendor/build/samples/skiko-offscreen/`。
离屏 PNG 默认也写入这里。`packageWindowsReleaseExecutable` 对对应暂存目录打 ZIP。
暂存与运行使用相同文件集合；缺少显式声明的运行文件或文件名冲突会失败。

Linux 使用 x64 Linux 宿主，要求 g++、ar、nm、fontconfig/GL 开发包。SDL 可从固定源码构建：

```bash
python3 native-compose/scripts/sync_upstream.py
python3 native-compose/scripts/prepare_sdl.py --build
python3 native-compose/scripts/doctor.py
./gradlew :native-compose:adaptation:samples:skiko-offscreen:linuxTest
./gradlew :native-compose:adaptation:samples:skiko-offscreen:runLinuxDebugExecutable
./gradlew :native-compose:adaptation:samples:skiko-window:runLinuxDebugExecutable
```

如果系统将 SDL 安装到其他前缀，传入 `-Psdl.root=/absolute/prefix`。需要的 `libSDL3.so*`
与可执行文件一同暂存，通过 `$ORIGIN` 查找。Linux 构建、X11/Wayland 和窗口行为必须在对应宿主验证。

macOS 使用 Apple Silicon 宿主、JDK 25、Xcode/command line tools、Python 3.11+ 和 CMake：

```bash
python3 native-compose/scripts/sync_upstream.py
python3 native-compose/scripts/prepare_sdl.py --build
python3 native-compose/scripts/doctor.py
./gradlew "-Pnative.compose=true" :native-compose:adaptation:samples:skiko-offscreen:macosTest
./gradlew "-Pnative.compose=true" :native-compose:adaptation:samples:skiko-offscreen:runMacosDebugExecutable
./gradlew "-Pnative.compose=true" :native-compose:adaptation:samples:skiko-window:runMacosDebugExecutable
```

脚本校验 macOS ARM64 Skia 归档，并从固定 SDL 源码构建 ARM64 dylib，默认安装至 `vendor/sdl-macos`。
`-Psdl.root=/absolute/prefix` 也适用于 macOS；指定前缀应包含对应架构的 SDL 头文件和动态库。
样例使用 `@loader_path` 查找同目录 dylib，SDL install name 使用 `@rpath`。
程序暂存及 ZIP 保留可执行权限。当前提供平铺目录和 ZIP；`.app`、签名、公证与安装器属于后续发行工作。

## GitHub 三平台基线

工作流 `.github/workflows/native-core.yml` 名称为 **Native Skiko Core and Skottie**，可通过 Actions → Run workflow 手动运行。
`main` 上相关代码变更及相关 PR 也会触发；不读取或生成 `local.properties`，通过 `-Pnative.compose=true`
显式开启模块组。普通工程仍默认不接入 Native Compose。

矩阵固定为 `windows-2025 / mingw_x64`、`ubuntu-24.04 / linux_x64`、`macos-26 / macos_arm64`。
`doctor.py` 核对实际宿主架构，macOS 不能使用 Rosetta/x64 JDK 冒充 ARM64 构建。
每个宿主执行：

1. 校验并重现源码及 Skia/SDL 输入，编译桥插件并运行集成测试。
2. 编译 Core 和 Skottie 的 C++/KLIB，执行 6 项 Native 测试：Core 的像素回读、图片编解码、
   SVG、字体/双向排版，以及 Skottie 的 seek/绘制像素和无效 JSON。
3. 运行离屏和 SDL 缩放示例，打 Debug ZIP，并在新的带空格目录中解压运行两个 ZIP。
4. 发布该宿主的 Core、Skottie 和插件到临时 Maven 仓库，审计元数据、静态归档、CInterop、许可证及本机链接路径。

汇总任务只选用 macOS ARM64 生产的一份根 KMP publication，并保留三个宿主分别生产的 target publication；
不会让三个宿主互相覆盖根元数据。根与各 target 完整性审核后，另起三台干净 runner，从汇总 Maven 仓库
加载插件和 KLIB，编译、链接、运行独立消费者；这些消费任务不准备 vendor、Skia 或 SDL SDK。

独立消费者只声明 Skottie，依靠其 API 元数据传递引入 Core，以检查实际的 Maven 项目依赖发布。
成功运行后可下载：`native-core-maven`（三目标 Core + Skottie 仓库）、`native-bridge-maven`（插件仓库）、
各平台 `native-core-evidence-*`（样例 ZIP、PNG、测试报告）和 `native-core-consumer-*`（Maven 消费 PNG）。
下载的 artifact ZIP 需先解压，再作为 Maven 仓库使用。消费者可通过 `-Pnative.repository` 和
`-Pnative.plugin.repository` 分别指定这两个仓库的绝对路径。

Linux 窗口基线使用 Xvfb/X11；三平台的默认 CI 步骤显式选择 `native.renderer=software`。
手动运行时可勾选 `gpu-smoke`，另外要求 D3D12 / Vulkan / Metal 窗口和 resize 通过；runner 必须具备对应驱动。
Vulkan 软件设备也能验证 Vulkan API，但不能证明独立 GPU 性能。显式 GPU 测试发生回退会失败。
工作流不覆盖 Linux Wayland，只上传 Actions 构建工件，不向 Maven Central 或 GitHub Release 自动发布。

## Bridge 的消费方式

项目插件 ID 为 `love.yinlin.native-bridge`。插件内包含与本次源码锁一致的 Skiko 映射：
`org.jetbrains.skiko:skiko:0.156.0` 在 `mingw_x64`、`linux_x64`、`macos_arm64` 上映射到
`love.yinlin.native.skiko:skiko:0.156.0`。自定义目标名 `windows`、`linux`、`macos` 不影响判断。
`org.jetbrains.skiko:skiko-skottie:0.156.0` 同样按这三个 target 映射到
`love.yinlin.native.skiko:skiko-skottie:0.156.0`；Skottie 的 Maven 元数据以 API 依赖引入对应 Core。

```kotlin
plugins {
    kotlin("multiplatform") version "2.5.0-Beta2"
    id("love.yinlin.native-bridge") version "3.7.1"
}
kotlin {
    mingwX64("windows")
    linuxX64("linux")
    macosArm64("macos")
    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.skiko:skiko:0.156.0")
    }
}
nativeApplication {
    enabled.set(true)
    baseName.set("example")
    entryPoint.set("example.main")
    runtime("mingw_x64") {
        artifacts.put(
            "love.yinlin.native.skiko:skiko-mingwx64:0.156.0:icudtl@dat",
            "icudtl.dat",
        )
        // files.from(...) 可接本地文件/任务输出；dependencies.add(...) 可接 Maven 工件。
    }
}
```

发布坐标为首轮开发坐标，目前不表示已经上传公共 Maven。运行数据通过应用配置显式声明；插件本身不会为
没有 Native target 的工程解析移植 KLIB。仅 JVM/Android 消费者使用官方 Skiko；插件 JAR 是构建期依赖。
普通库只需要依赖解析能力时，保持 `nativeApplication.enabled=false`（默认值）。

新增库使用 `nativeBridge.redirect("名称") { upstream; upstreamVersion; targets; module }`；
`targets` 填 Konan 标识。`projectPath` 可替代 `module`，直接引用同一 Build 中的项目。
共享 metadata 只有在所有参与目标均使用同一移植实现时才整体替换；混合平台公共图保留官方声明。
各个 Native 编译配置分别选择对应移植实现。完整的 IDE、多模块 Compose metadata 验收随 Compose 接入推进。

本地源码开发还使用 `love.yinlin.native-bridge.settings`：

```kotlin
pluginManagement { includeBuild("path/to/ylcs-kmp/build-logic") }
plugins { id("love.yinlin.native-bridge.settings") }
nativeBridgeSources.include(file("prepared/skiko"), "skiko", mapOf(
    "skiko-core" to ("love.yinlin.native.skiko:skiko" to ":"),
    "skiko-skottie" to ("love.yinlin.native.skiko:skiko-skottie" to ":skiko-skottie"),
))
```

Settings 插件保存 included build 的项目选择器，项目插件直接选择它。不要假定两次坐标替换会自动串联。
Maven 验证由独立的 `verification/maven-consumer` 完成，不包含 Skiko 源码 Build，不开启维护模式。

## 发布和检查

```powershell
.\gradlew.bat :build-logic:native-bridge:test :build-logic:native-bridge:validatePlugins :build-logic:native-bridge-settings:validatePlugins
.\gradlew.bat :native-compose:publishSkikoToVerificationRepository
.\gradlew.bat :build-logic:native-bridge:publishAllPublicationsToLocalVerificationRepository :build-logic:native-bridge-settings:publishAllPublicationsToLocalVerificationRepository
python native-compose/scripts/verify_repository.py --target mingw_x64
.\gradlew.bat -p native-compose/verification/maven-consumer runWindowsDebugExecutable
```

Skiko 验证仓库位于 `native-compose/vendor/build/native-compose/repository/`。独立消费者从这里解析根 metadata、
Windows KLIB、CInterop 与 ICU 数据，并从 `build-logic/build/native-bridge-repository/` 加载已发布的插件；
不包含源码 Build。桥插件的 ID 和版本来自根版本目录，版本跟随 Rachel。
如果使用其他仓库，通过 `"-Pnative.repository=<仓库的绝对路径>"` 指定。
PowerShell 中包含点号的 `-P` 参数请整体加引号。
Linux/macOS target publication 要在对应宿主构建并验证后汇总。根 KMP metadata、target KLIB、CInterop 和 ICU 数据
必须配套；只有裸 KLIB 无法提供完整的普通 KMP 消费体验。

依赖替换规则只作用于消费方解析，不会自动改写库发布出去的依赖坐标。发布自己的框架模块时，
应在对应 Native 源集中声明真实移植依赖，并审核生成的 POM/Gradle Module Metadata；同一 Build 的项目依赖
可以由 KGP 发布为生产者坐标，composite 替换不能替代发布验证。

```powershell
python native-compose/scripts/sync_upstream.py --check-updates
python native-compose/scripts/sync_upstream.py --offline
```

更新检查只报告远程引用，不会移动版本锁。源码提交和补丁审核后才更新锁；不要在普通构建中自动追踪 master。
第三方来源及许可见 [第三方说明](licenses/THIRD_PARTY_NOTICES.md)。

窗口自动冒烟检查：加 `"-Pnative.smoke=true"` 执行 `runWindowsDebugExecutable`，程序完成绘制、
窗口缩放后的第二次绘制，再销毁资源并退出。它不替代完整的桌面交互和 GPU 后端验收。
当前实际验证结果见 [验证记录](docs/VALIDATION.md)。日志统一存放在 `vendor/build/logs/`。

## 官方窗口层参考

截至 2026-10-24，[Kotlin Desktop Toolkit](https://github.com/JetBrains/kotlin-desktop-toolkit) 已提供
桌面窗口、事件循环、输入、剪贴板及渲染上下文等 OS 集成工作。它与 Skiko 的职责互补；
当前项目自身已有与 Skiko 配合的示例，不能将“单独的窗口层仓库”理解成尚未合并的 Skiko 核心移植。

当前 Kotlin 模块使用 `kotlin("jvm")` 和 JDK 25，Windows API 使用 `java.lang.foreign`；README 将 KMP
列为后续目标，因此现有 Kotlin 工件不能直接用于 Kotlin/Native。其 Rust 后端已经通过 C ABI 导出函数，
可以作为后续 Native 窗口层候选，但需要 cinterop 绑定、回调/线程/生命周期适配和各宿主验证。
Windows Cargo 当前输出 `rlib` 和 `cdylib`，没有现成的 C `staticlib`；静态集成还要单独适配构建与链接。

本轮离屏示例不涉及窗口，窗口示例由 SDL3 管理窗口和事件。尚未引入或验证 KDT；后续接入 KDT
不应改变已验证的 Skiko 核心绘图和依赖消费路径。

源码依据：[模块构建](https://github.com/JetBrains/kotlin-desktop-toolkit/blob/05dbfbdb01252cd39b227343635baa419165fa7d/kotlin-desktop-toolkit/build.gradle.kts)、
[Windows Kotlin 封装](https://github.com/JetBrains/kotlin-desktop-toolkit/blob/05dbfbdb01252cd39b227343635baa419165fa7d/kotlin-desktop-toolkit/src/main/kotlin/org/jetbrains/desktop/win32/Window.kt)、
[Rust 构建](https://github.com/JetBrains/kotlin-desktop-toolkit/blob/05dbfbdb01252cd39b227343635baa419165fa7d/native/desktop-win32/Cargo.toml)、
[C ABI 约定](https://github.com/JetBrains/kotlin-desktop-toolkit/blob/05dbfbdb01252cd39b227343635baa419165fa7d/native/desktop-win32/docs/FFI_CONVENTIONS.md)。
