# Rachel Desktop Native Compose

## 概况

本目录维护 Windows x64、Linux x64、macOS arm64 的 Kotlin/Native 桌面 Compose 移植，三个目标均使用 Rachel 移植工件。

共 26 个工件：Skiko Core/Skottie、UI 八工件、Foundation 两工件、Animation 三工件、Resources、Navigation3 UI、Material3 主族四工件、Material Ripple、Adaptive 四工件。Runtime 等已具备三个目标支持的依赖使用官方工件。

在根 [libs.versions.toml](../libs.versions.toml) 选择上游版本。Material3 主族跟随 `cmp`，四个 Adaptive 工件跟随 `material3-adaptive`，完整 ICU 数据由 `icu` 选择。Skia SDK 版本从所选 Skiko 发布源码读取。同步器计算下载内容的摘要并生成来源记录，保存在 Git 忽略的 `vendor/` 下。移植库的 Maven 版本仍采用 Rachel 的发布格式。

## 目标与源码边界

提供标准 Compose API、桌面 Native 窗口、资源读取与部署、Maven 发布/消费及应用目录分发。桥插件按 Native target 将官方请求重定向为 `love.yinlin.native.*` 工件；JVM、Android 等目标使用官方 Compose。KLIB 保留上游模块身份，以兼容官方 Runtime、AndroidX 等预编译依赖。

- `adaptation/<上游 group>_<artifact>/`：维护的 Gradle buildFile、Kotlin/C++ 实现、cinterop 和单元测试。`src` 统一使用 `nativeMain`、`windowsMain`、`linuxMain`、`macosMain` 及对应 `Test`；cinterop 位于 `nativeMain/cinterop`。
- `upstream/*.json`：上游仓库、工件和构建配置。`upstream/patches` 保存上游差异；同步器检查补丁能否应用，计算摘要并保留原始许可证和版权。
- `adaptation/provenance/`：记录维护中适配源码的原始出处。只有替换适配源码来源时才需要更新，随移植工件发布供追溯。
- `gradle/templates`：Compose included build 模板。上游 `commonMain`、`skikoMain` 等编译片段由生成配置组织，保留元数据和 expect/actual 边界，不要求适配目录增加相同层次。
- `vendor`：忽略的上游源码、归档、SDK 和构建输出，由 Kotlin 同步器管理。维护差异放入适配源码或补丁，不直接修改生成源码。
- `verification`：独立 Maven 发布/消费工程，验证脱离源码复合构建后的使用方式。

未修改的 macOS 实现直接从当前版本的上游源码目录加入 `macosMain`，与适配源码一起编译成 Rachel 工件。例如 `ui-util` 使用上游 `macosMain`，`ui-text` 使用上游 `nativeMain`、`darwinMain` 和 `macosMain`。适配目录没有同名文件，不代表使用官方 macOS 二进制工件。

源码同步位于 `build-logic/repository-conventions`，SDL 构建及验证工具位于 `build-logic/native-bridge`，Release 二进制与符号处理的共享实现位于 `build-logic/native-binaries`。后者由插件编译其源码，不是 `build-logic/settings.gradle.kts` 中单独 include 的项目。维护流程使用 Gradle/Kotlin，不依赖 Python。

SDL 静态 SDK 使用 [upstream/sdl.json](upstream/sdl.json) 中的窗口宿主构建配置：保留窗口、输入、剪贴板、拖放、托盘、原生对话框和 Skiko 所需的图形上下文接入，以及 SDL 软件渲染回退；关闭音频、摄像头、手柄、触觉、HIDAPI、传感器、电源查询、独立 SDL GPU 和 SDL 自身不使用的硬件 renderer。Compose 的 D3D12/Metal/OpenGL GPU 渲染继续由 Skia/Skiko 提供。源码同步会对 SDL 的动态 API 总开关应用可重复、带上游格式检查的调整，让静态链接可以裁剪未引用 API；该 SDK 不支持通过 `SDL3_DYNAMIC_API` 替换运行时实现。配置和同步逻辑均参与源码/SDK 缓存身份，修改后会自动重建。

桌面 Native 拖放使用 [SDL 补丁](upstream/patches/sdl.patch) 提供逐窗口的同步协商：Windows OLE、macOS Cocoa、Linux X11／Wayland 在悬停时将格式与位置交给 Compose 命中检查，匹配后才返回允许复制。未设置接收目标、禁用、类型不匹配或移出组件时返回禁止；实际文件和文本在松手后交付。补丁由同步器校验、应用并记录 SHA-256，参与 SDL SDK 缓存身份；更新 SDL 时须一并审核。

## 维护端使用

需要 JDK 25、根 Gradle Wrapper、Git。Windows 需要 MSVC/Windows SDK；Linux 需要桌面开发包；macOS arm64 需要 Xcode。IDEA 的 Gradle JVM 使用 JDK 25。Gradle Settings 自动准备和登记本地移植源码。

根版本目录指定 Gradle 和 Kotlin 版本。根 Wrapper 的 `distributionUrl` 必须与目录一致，生成的上游工程复用该 Wrapper。Kotlin 源码统一使用目录中的 KGP，语言及 API 级别取该 Kotlin 版本的主次版本，实验参数取根 `gradle.properties`。`.gradle.kts` 脚本自身由当前 Gradle 发行版内嵌的 Kotlin 编译，编写脚本时要使用它支持的语法。

桥插件为本地源码消费单独生成项目结构元数据：保留发布坐标，并为实际选中的源码项目补充其 Gradle group/name 身份，兼容 Kotlin 在同一 build 和跨 included build 时使用的不同匹配方式。该文件仅替换 KGP 的 `kotlinProjectStructureMetadata` secondary variant；Maven publication 和 metadata JAR 继续使用原始发布元数据。通过 `ExportLib`／`api` 传递的依赖可沿模块链导入共享 Native 源码，不需要消费模块重复声明 Compose 子工件。

```text
./gradlew -p build-logic :repository-conventions:prepareNativeSources
./gradlew -p build-logic --offline :repository-conventions:prepareNativeSources
./gradlew -p build-logic :repository-conventions:test :native-bridge:test :native-bridge:validatePlugins
./gradlew :native-compose:testHost :native-compose:publishHost
./gradlew :native-compose:verifyHost
```

`prepareNativeSources` 按版本目录解析 Git 标签和发布归档，计算 SHA-256，核对上游提供的摘要（如有），并校验本地缓存内容，再应用补丁、生成来源记录和 included builds。根 Gradle Sync 自动执行同一准备过程；准备源码本身不执行 CMake 或库编译。需要使用 Native 库的任务会按需编译。离线模式需要本地已有当前版本的完整、可校验的上游输入。

归档和 Git 源码缓存按版本和 SHA-256 存放在 `vendor/archives/` 和 `vendor/git/`。`vendor/g/<id>/` 只由实际选择上游源码的版本组合决定：`kotlin`、`cmp`、`skiko`、`sdl`、`icu`，以及 `upstream/compose.json` 中模块使用的 `versionRef`（当前为 `navigation3` 和 `material3-adaptive`）；Material3 主族跟随 `cmp`。其中包含 Skiko、Compose、ICU、SDL，以及此组合的 CMake、SDK、Gradle 输出和 Maven 验证仓库。修改 `adaptation` 中的 Kotlin/C++ 源码由 Gradle 按任务输入重新编译，不切换上游目录。框架版本、仅供编译的依赖、构建属性和 Wrapper 变化时，在原目录刷新生成的构建配置，保持未变源码的时间戳和已有构建输出。

上游补丁或源码布局配置变化时，在原目录按组件更新相关文件，复用其他组件及已下载归档。源码记录继续校验 SHA-256，并在覆盖前拒绝未记录的本地修改。`vendor/current.json` 只在整套源码准备成功后更新，供独立验证和 CI 定位产物。切换到新的版本组合键时会首次建立新目录，旧版目录会保留；回滚版本可复用仍完整的对应缓存，同步器只清理自己创建的临时目录。

升级时修改版本目录即可触发同步。若新发布改变了补丁上下文、源码布局或 SDK 结构，准备过程会在不兼容的位置报错，此时需要更新对应适配。Windows 构建 SDL 需要 x64 MSVC、Windows SDK 和 Ninja：脚本会检查 Gradle 进程实际可见的编译器，必要时通过 `VS_PATH`、`SKIKO_VSBT_PATH`、`VSINSTALLDIR` 或 `vswhere` 找到安装，再初始化 `vcvars64.bat`。

Kotlin 警告沿用仓库模块的默认行为，源码保留原有的局部 `@Suppress` 注解。同步器保留上游的版权声明。

Material3、Adaptive、Navigation3 使用官方 macOS sources JAR 中的真实共享实现；缺失 actual 由适配源码提供。日期和数字本地化共用 Skia ICU，macOS 的 `CalendarLocale` 保留 `NSLocale` 类型身份。

应用默认分发 JetBrains Skia SDK 自带的约 3.72 MiB 裁剪版 ICU 数据，包含中英文文本边界规则、中日文词典及 Unicode 规范化等基础数据。同步生成的 `metadata/icu.json` 记录源 SDK 和两份数据的 SHA-256；三个桌面目标共用同一份小端数据，以 `icudtl-text@dat` 随 Skiko publication 发布，桥插件在分发目录中将其命名为 `icudtl.dat`。应用分发目录不额外复制 ICU 许可证，适配源码也不单独维护许可证副本；上游归档及 Maven 许可来源保持原有管理方式。

裁剪版不包含完整的各语言日期、数字、货币等本地化资源。完整 ICU 数据仍以 `icudtl@dat` 发布，`nativeBridge { skikoTests() }` 和移植维护测试使用完整数据，以验证 Material3 日期及数字本地化能力；需要这些能力的应用必须自行选用完整数据。Linux/macOS 首次使用日期服务时注册只读内存映射，Windows 使用 Skia 的数据加载器。

`testHost` 执行当前宿主库测试；`publishHost` 输出到当前代的 `build/repository`；`verifyHost` 校验 Maven 文件、校验和、目标、KLIB 身份、静态库、许可、依赖闭包及维护测试报告。缺失、失败或跳过的测试不能作为通过结果。

日常 Build/Test 通过共享的 `setup-build`、`setup-native` 动作恢复上游下载与 Kotlin/Native 工具链，再验证并准备当前代源码。桌面 Native 作业按当前代与宿主镜像恢复 SDL SDK。修改 `skikoMain`、`skikoTest` 也会触发三个桌面 Native 平台的框架测试。

[native-compose.yml](../.github/workflows/native-compose.yml) 是独立的移植维护工作流，适配源码、上游配置及补丁、桥插件、同步器和相关构建配置变化时运行，普通 `ylcs-module` 或示例源码变化不触发它。仅修改版本目录时，先比较 Native 构建实际使用的版本、库和插件；框架自身版本号及无关业务依赖变化不会启动三个宿主的重型验证。维护流程保留移植回归测试、本地发布、独立 Maven 消费和三宿主工件汇总。

GitHub Actions 的 macOS Gradle 作业通过共享 `setup-native` 动作设置 `kotlin.incremental.native=false`、一个 Gradle worker、一个 Native 后端线程及 3 GiB Gradle 堆，Kotlin 编译在 Gradle 进程内执行。这些用户级属性覆盖仓库的本地开发配置，同时作用于框架 Build/Test、发布、included build 和独立 Maven 消费验证，避免冷构建重复建立按文件项目缓存及多个编译进程争用 runner 内存；Gradle build cache 和 Native 外部依赖缓存继续启用。框架 Apple Native 单元测试及移植验证程序使用链接器 `-S` 省去调试映射，测试断言和编译优化模式保持原有配置。本地构建保留原有增量编译及调试配置。维护工作流保存宿主库阶段的完整日志、Gradle 耗时报告及 Kotlin 编译报告。

`:repository-conventions:writeNativeCiInputs` 计算 CI 的版本目录输入。CI 可以恢复含有其他版本的下载缓存，但同步器按当前版本定位文件并校验 SHA-256；静态 SDL SDK 的缓存键使用完整的当前代哈希。

升级维护流程：

1. 在根版本目录更新目标版本。同步器解析发布标签、Maven 源码和 Skia SDK，并生成对应的 SHA-256 记录。
2. 若补丁不再适用，在临时目录用原始源码更新补丁，执行 `git apply --check`，保留平台实现和许可证。只有补丁文件增删或改名时才需要调整清单。
3. 核查适配实现、源集、依赖导出、KLIB 身份、资源所有权及测试；Kotlin 位置解构等兼容性修改保留为适配补丁。
4. 准备源码并验证对应宿主和独立 Maven 消费，刷新 IDEA Gradle 模型。

## 消费端使用

应用与 CMP/Skiko 兼容的桥插件，在普通 `commonMain` 中声明官方 Compose 依赖。消费工程不需要检出本目录或构建 Skia/SDL。

```kotlin
plugins {
    install(libs.plugins.rachel, libs.plugins.kotlinMultiplatform, libs.plugins.composeCompiler, libs.plugins.nativeBridge)
}

rachel {
    multiplatform {
        windowsTarget = true
        linuxTarget = true
        macosTarget = true
        sources {
            commonMain { lib(libs.compose.material3) }
        }
    }
}
```

按需使用 `libs.compose.ui`、`libs.compose.foundation`、`libs.compose.material3`、`libs.compose.material3.windowSizeClass`、`libs.compose.material3.adaptive.navigation.suite`、`libs.compose.material3.adaptive.layout`、`libs.compose.material3.adaptive.navigation`、`libs.compose.material3.adaptive.navigation3`。Material3 的基础 UI、Foundation 和 Ripple 随依赖图引入。Resources 同时应用官方 Compose Multiplatform 插件，沿用 `src/commonMain/composeResources` 和生成的 `Res`。

桌面入口位于 Native 源集，调用 `androidx.compose.ui.window.NativeWindow`。

`NativeWindowOptions` 支持创建时的 `transparent` 和 `undecorated`，默认均为 `false`。透明模式贯通 SDL 窗口与 Windows D3D12、Linux Vulkan、macOS Metal 的 alpha 缓冲；Linux 需要支持透明合成的窗口管理器和 Vulkan surface。`NativeWindowScope` 提供图标、最小尺寸、窗口装饰及标题栏命中区域接口；图标像素由 SDL 复制，拖动回调在窗口关闭前解绑。

窗口可通过 `focusable`、`resizable` 和 `clickThrough` 配置输入焦点、用户缩放及整窗鼠标穿透，默认分别为 true、true、false。Rachel Native 应用可覆盖同名初始属性，并在运行时修改 `controller.focusable`、`controller.resizable`、`controller.clickThrough`。全屏期间的缩放开关在退出全屏后应用，不可缩放的普通窗口不提供最大化操作。焦点与鼠标穿透独立控制，不获取焦点时仍然正常绘制。

Windows、macOS 和 X11 支持禁用普通窗口焦点；Wayland 普通顶层窗口的 `focusable` 配置仅保存赋值，不调用 SDL 修改焦点能力，也不抛异常或回写配置值。鼠标穿透、用户缩放和标题栏拖动分别独立控制，Wayland 的鼠标穿透仍可正常使用。

鼠标穿透通过由适配补丁提供的 SDL 窗口扩展实现，要求无系统标题栏；恢复系统标题栏时自动关闭穿透。Windows 使用 layered／transparent 扩展样式，并在关闭穿透时恢复原有相关样式位；窗口宿主预先注册不带 `CS_OWNDC` 的窗口类，以兼容分层输入穿透，保留 D3D12／软件渲染。macOS 使用 NSWindow 的忽略鼠标事件属性；X11 使用 SHAPE 1.1 输入区域；Wayland 使用 wl_surface 的空输入区域。切换不会清空绘制内容，缺少所需后端能力时明确报错。补丁哈希参与源码和静态 SDK 缓存身份。

Rachel 的 `compose:app` Native `PlatformApplication` 默认采用透明、无系统标题栏窗口，支持与 JVM 类似的 `title`、`icon: DrawableResource?`、`initSize`、`minSize`、`roundedCorner` 和 `TopBar(controller, onExit)`。`controller.icon`、`controller.undecorated` 和 `controller.roundedCorner` 可在运行时更新；`transparent` 必须在创建窗口前配置。自定义标题栏通过 `WindowScope.DragArea` 识别主鼠标键拖动，再调用底层窗口的 `startDrag()` 发起平台拖动；支持长按与双击回调。交互控件区域可用 `Modifier.windowDragArea(controller, draggable = false)` 禁止拖动，边缘保持系统缩放；切回系统装饰后隐藏 Compose 标题栏。位图、XML Vector 和 SVG 图标共用 Painter 栅格化，macOS 设置应用 Dock 图标；Wayland 不支持图标协议时保留系统图标，不中断应用。窗口实际透明还需要 Compose 内容使用透明背景。

JVM 和 Native 应用通过 `openWindow(ApplicationWindow)` 注册独立子窗口，由应用的窗口管理器挂载，当前没有 `MultipleWindow` 覆写入口。子窗口分别实现 `Window(content)` 和 `Content()`：JVM 的前者可直接调用官方 `Window(onCloseRequest = ::close)`，Native 的前者可调用 `love.yinlin.compose.NativeWindow` 扩展。框架自动为内容提供应用的主题、字体、资源及 `LocalPlatformContext`，平台宿主保留各窗口独立的 Lifecycle、ViewModelStore 和 Compose 作用域。透明子窗口可覆盖 `background = Color.Transparent`。

每次打开创建新的 `ApplicationWindow` 实例，可将需保留的业务状态放在外部。窗口激活时调用 `initialize()`，离开组合时取消其 `scope` 并调用 `uninitialize()`；`close()` 只关闭自身。主窗口关闭后停止接受新窗口，关闭所有子窗口及其原生资源，完成应用清理后结束进程。Native 的多个窗口共用一个 SDL 事件循环，按窗口 ID 分发输入、关闭及唤醒事件；独立的应用级 Compose 组合管理子窗口，即使主窗口隐藏或最小化也继续驱动其生命周期。系统主题和语言等全局变更广播到所有窗口。子窗口不建立原生父子关系或模态关系。

`initPosition` 使用 `WindowPosition.Absolute(x.dp, y.dp)`、`WindowPosition.Aligned(Alignment.Center)` 或 `WindowPosition.PlatformDefault`，`initPlacement` 使用 `WindowPlacement.Floating`、`Maximized` 或 `Fullscreen`。运行时可修改 `controller.position`、`controller.placement`，系统移动、缩放、最大化和全屏事件会回写状态。对齐以当前显示器工作区为准；绝对坐标按当前显示器的 dp／SDL 窗口坐标比例转换。最大化、全屏或最小化期间的位置请求在还原后应用，`PlatformDefault` 只用于初始位置。Wayland 不开放全局窗口定位，`controller.isPositionSupported` 为 false，位置由合成器决定。

`foundation:context` 的 Windows、Linux、macOS 分别维护 `PlatformContextProvider actual`，不增加共享 Native Provider 声明或句柄类型包装。各自的 `windowHandle: Long?` 分别保存 HWND、NSWindow 指针、X11 Window ID 或 wl_surface 指针；Linux 另有 `displayHandle: Long?` 保存 X11 Display 或 wl_display 指针。应用通过内部的平台接线在连接窗口时填入句柄，关闭时清空。句柄由 SDL 持有，消费端不得释放、跨窗口缓存或在窗口关闭后使用；context 模块不依赖 Skiko。

Native 应用沿用 JVM 的 `protected open val commandArgs`，由入口显式传入，不自动读取或解析进程参数：

```kotlin
private val appReference = LazyReference<DesktopApplication>()

class DesktopApplication(override val commandArgs: Array<String>) : PlatformApplication<DesktopApplication>(appReference) {
    override val initPosition = WindowPosition.Aligned(Alignment.Center)
    override val initPlacement = WindowPlacement.Floating

    @Composable
    override fun Content() { /* 应用内容 */ }
}

fun main(args: Array<String>) = DesktopApplication(args).run()
```

Native 托盘沿用 SDL 的图标接口，通过 `controller.tray.visible` 显示或隐藏，`painter` 默认为窗口图标，`tooltip` 默认为窗口标题。`isActive` 表示图标是否已创建，`isAvailable` 记录最近一次创建结果（尚未尝试时为 null）。桌面未提供托盘服务时跳过创建，Linux 需要可用的 GTK/AppIndicator 和桌面托盘服务。显隐与提示文字等状态由窗口生命周期内的 Main 协程观察，窗口隐藏或最小化时仍可更新；关闭窗口时取消观察并释放托盘。当前只开放图标、提示文字及显隐，点击、菜单和通知待后续接入，不添加图标点击的 SDL 扩展补丁。

应用安装桥插件后，可在 `rachel.multiplatform` 配置打包信息：

```kotlin
composeDesktopPackage {
    entryPoint = "example.main"
    packageName = "ExampleApp"
    displayName = "示例应用"
    packageVersion = "1.0.0"
    vendor = "Example"
    author = "Example Team"
    copyright = "Copyright 2026 Example"
    workingDirectory = rootProject.layout.projectDirectory.dir("work/modManager").asFile
    arguments = listOf("--profile", "development")
    windows { iconFile = file("icons/app.ico") }
    macos { iconFile = file("icons/app.icns") }
}
```

该配置与 JVM 的 `desktopPackage {}` 独立，同一模块可同时配置。`composeRun` 运行宿主 Debug，`composeReleaseRun` 运行 Release，`composeDistribution` 输出 `build/nativeApplication/<target>/releaseExecutable` 普通目录，包含 Compose 资源和平台运行数据。Windows 图标/版本信息嵌入 EXE；macOS 使用 `.app`、Info.plist 和 ICNS。自定义 `composePublish` 可消费此目录进行复制或 ZIP 打包。

`workingDirectory` 只设置 Gradle 运行任务所启动进程的当前目录，不存在时自动创建；未配置时沿用应用分发目录。`arguments` 按独立参数传递，包含空格的参数无需手动添加引号。二进制仍从 `build/nativeApplication` 中启动，ICU 和 Compose 资源继续按可执行程序所在位置查找。两项配置均不会写入二进制、应用 Bundle 或启动脚本，直接运行打包后的程序时仍由启动方式决定当前目录和参数。

仅使用桥插件时，可以配置相同选项：

```kotlin
nativeApplication {
    workingDirectory.set(file("work"))
    arguments.set(listOf("--profile", "development"))
}
```

Release 链接后自动分离符号，桥插件与 Rachel 的 `nativeExecutable`、`nativeLibrary` 模板共用此处理；Debug 保持原样。链接输出和应用分发目录使用剥离后的 EXE、ELF 或动态库，符号归档在模块 `build/nativeSymbols/<target>/<binary>/`：Windows/Linux 为 `<binary>.debug`，macOS 为 `<binary>.unstripped` 及链接器已生成的 `.dSYM`。`symbols.properties` 记录剥离前后 SHA-256；旁边的 `<binary>-distribution.properties` 记录嵌入应用资源后最终分发文件的 SHA-256，用于匹配对应构建。macOS 剥离后重新进行 ad-hoc 签名。符号不会复制进分发目录。请在清理 `build` 前自行归档符号；普通 Release 未生成的 DWARF 行号信息不会凭空补齐，Windows 的 `.debug` 是 COFF 格式而非 PDB。Linux 使用 `objcopy`，Windows 使用 Kotlin/Native 已下载的 MinGW `objcopy`，特殊工具链可通过 `-Prachel.native.objcopy=/path/to/objcopy` 指定。

`NativeWindow` 在创建场景前自动调用 `registerSkikoComposeImplementation()`，注册 Skiko 图形和文本后端。若自行管理底层 Compose 场景，或在窗口创建前使用 `Path()`、`ImageBitmap()`、文本排版等后端 API，需要在首次使用前完成注册。

Windows/Linux 的 UI 工件在应用启动时自动注册 `Dispatchers.Main`，窗口创建后将任务投递到同一个 Native UI 事件队列；`MainScope()`、`Dispatchers.Main.immediate` 和 Rachel 的 `Coroutines.main` 无需额外初始化。窗口启动前可以创建作用域、提交任务，任务会等待窗口事件循环开始；后台切回 Main、延迟和超时恢复均返回 UI 线程。关闭窗口后新的 Main 任务会被取消，重新打开窗口会接入新的 UI 队列；应用自行创建的作用域仍由应用负责取消。macOS 保留 coroutines 的系统主队列。此接入使用 coroutines `1.11.0` 的 Native 内部注入接口，上游升级时需保留调度器回归检查。

框架库沿用自身 Maven group/GAV，发布 KMP root 及启用的 Native target publication。JVM、Android 等用户按普通方式依赖；Native 用户应用匹配版本的桥插件。公开 Compose 类型按模块 API 导出规则声明，保持消费者可见的依赖完整。

## 窗口作用域与源码示例

`compose:ui` 的 Native `WindowScope` 是 `org.jetbrains.skiko.window.NativeWindowScope` 的类型别名。`DragArea` 是作用域扩展，创建跟随组合生命周期的拖动区域；区域离开组合时注销注册，窗口或拖动配置改变时更新绑定。应用不再通过 `WindowController.draggable` 配置全局拖动开关。

实际主窗口在 `PlatformApplication.native.kt` 的 `ComposedLayout` 内使用：

```kotlin
DragArea(enabled = !controller.maximize, onDoubleClick = { controller.toggleMaximize() }, modifier = Modifier.fillMaxWidth()) {
    TopBar(controller, onMainWindowClose)
}
```

`controller`、`TopBar` 与 `onMainWindowClose` 属于应用宿主。需要额外拖动区域的组件应传入当前窗口作用域，避免持有其他窗口的 scope。`addDragArea` 的返回值具有注销责任；直接使用底层接口时必须在所属生命周期结束时关闭注册。

- [DragArea 公共签名](../ylcs-module/compose/ui/src/pcMain/kotlin/love/yinlin/compose/ui/window/DragArea.kt)
- [Native 拖动实现](../ylcs-module/compose/ui/src/nativeMain/kotlin/love/yinlin/compose/ui/window/DragArea.native.kt)
- [Native 应用宿主](../ylcs-module/compose/app/src/nativeMain/kotlin/love/yinlin/compose/PlatformApplication.native.kt)
- [独立 Native 窗口扩展](../ylcs-module/compose/app/src/nativeMain/kotlin/love/yinlin/compose/ApplicationWindow.native.kt)
- [桌面打包回归用例](../build-logic/native-bridge/src/test/kotlin/love/yinlin/nativebridge/NativeDesktopPackagingTest.kt)
