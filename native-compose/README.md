# Rachel Desktop Native Compose

## 概况

本目录维护 Windows x64、Linux x64、macOS arm64 的 Kotlin/Native 桌面 Compose 移植，三个目标均使用 Rachel 移植工件。

共 26 个工件：Skiko Core/Skottie、UI 八工件、Foundation 两工件、Animation 三工件、Resources、Navigation3 UI、Material3 主族四工件、Material Ripple、Adaptive 四工件。Runtime 等已具备三个目标支持的依赖使用官方工件。

基线由根 [libs.versions.toml](../libs.versions.toml) 和 [upstream](upstream) 锁定：CMP/Material3 `1.13.0-beta01`、Adaptive `1.4.0-alpha01`、Navigation3 `1.2.0`、Skiko `0.156.0`、SDL `3.4.18`、ICU 数据 `78.2`。桥插件使用 Rachel 发布版本，移植库使用对应的上游版本。

## 目标与源码边界

提供标准 Compose API、桌面 Native 窗口、资源读取与部署、Maven 发布/消费及应用目录分发。桥插件按 Native target 将官方请求重定向为 `love.yinlin.native.*` 工件；JVM、Android 等目标使用官方 Compose。KLIB 保留上游模块身份，以兼容官方 Runtime、AndroidX 等预编译依赖。

- `adaptation/<上游 group>_<artifact>/`：维护的 Gradle buildFile、Kotlin/C++ 实现、cinterop 和单元测试。`src` 统一使用 `nativeMain`、`windowsMain`、`linuxMain`、`macosMain` 及对应 `Test`；cinterop 位于 `nativeMain/cinterop`。
- `upstream/*.json`：源版本、仓库、提交、归档 SHA-256、工件及来源清单。`upstream/patches` 保存受版本、提交和哈希约束的上游差异，保留原始许可证和版权。
- `gradle/templates`：Compose included build 模板。上游 `commonMain`、`skikoMain` 等编译片段由生成配置组织，保留元数据和 expect/actual 边界，不要求适配目录增加相同层次。
- `vendor`：忽略的上游源码、归档、SDK 和构建输出，由 Kotlin 同步器管理。维护差异放入适配源码或补丁，不直接修改生成源码。
- `verification`：独立 Maven 发布/消费工程，验证脱离源码复合构建后的使用方式。

未修改的 macOS 实现直接从锁定的上游源码目录加入 `macosMain`，与适配源码一起编译成 Rachel 工件。例如 `ui-util` 使用上游 `macosMain`，`ui-text` 使用上游 `nativeMain`、`darwinMain` 和 `macosMain`。适配目录没有同名文件，不代表使用官方 macOS 二进制工件。

源码同步位于 `build-logic/repository-conventions`，SDL 构建及验证工具位于 `build-logic/native-bridge`。维护流程使用 Gradle/Kotlin，不依赖 Python。

SDL 静态 SDK 使用 [upstream/sdl.json](upstream/sdl.json) 中的窗口宿主构建配置：保留窗口、输入、剪贴板、拖放、托盘、原生对话框和 Skiko 所需的图形上下文接入，以及 SDL 软件渲染回退；关闭音频、摄像头、手柄、触觉、HIDAPI、传感器、电源查询、独立 SDL GPU 和 SDL 自身不使用的硬件 renderer。Compose 的 D3D12/Metal/OpenGL GPU 渲染继续由 Skia/Skiko 提供。源码同步会对 SDL 的动态 API 总开关应用可重复、带上游格式检查的调整，让静态链接可以裁剪未引用 API；该 SDK 不支持通过 `SDL3_DYNAMIC_API` 替换运行时实现。配置和同步逻辑均参与源码/SDK 缓存身份，修改后会自动重建。

## 维护端使用

需要 JDK 25、根 Gradle Wrapper、Git。Windows 需要 MSVC/Windows SDK；Linux 需要桌面开发包；macOS arm64 需要 Xcode。IDEA 的 Gradle JVM 使用 JDK 25。Gradle Settings 自动准备和登记本地移植源码。

```text
./gradlew -p build-logic :repository-conventions:prepareNativeSources
./gradlew -p build-logic --offline :repository-conventions:prepareNativeSources
./gradlew -p build-logic :repository-conventions:checkNativeUpstreamUpdates
./gradlew -p build-logic :repository-conventions:test :native-bridge:test :native-bridge:validatePlugins
./gradlew :native-compose:testHost :native-compose:publishHost
./gradlew :native-compose:verifyHost
```

`prepareNativeSources` 获取锁定归档和稀疏 Git checkout、应用补丁并生成 included builds，不执行 CMake、库编译或应用运行。输入和受管理源码一致时直接复用，不重复解压 Maven 源码或还原、应用 Git 补丁，也不重写 Wrapper。离线模式要求所需归档、Git 对象及源码选择已存在。同步保留构建输出和未变化文件的时间戳，遇到本地修改、未知文件或上游漂移时保留现场并失败。

Material3、Adaptive、Navigation3 使用官方 macOS sources JAR 中的真实共享实现；缺失 actual 由适配源码提供。日期和数字本地化共用 Skia ICU，macOS 的 `CalendarLocale` 保留 `NSLocale` 类型身份。

应用默认分发 JetBrains Skia SDK 自带的约 3.72 MiB 裁剪版 ICU 数据，包含中英文文本边界规则、中日文词典及 Unicode 规范化等基础数据。`upstream/icu.json` 锁定源 SDK 和数据哈希；三个桌面目标共用同一份小端数据，以 `icudtl-text@dat` 随 Skiko publication 发布，桥插件在分发目录中将其命名为 `icudtl.dat`。应用分发目录不额外复制 ICU 许可证，适配源码也不单独维护许可证副本；上游归档及 Maven 许可来源保持原有管理方式。

裁剪版不包含完整的各语言日期、数字、货币等本地化资源。完整 ICU 数据仍以 `icudtl@dat` 发布，`nativeBridge { skikoTests() }` 和移植维护测试使用完整数据，以验证 Material3 日期及数字本地化能力；需要这些能力的应用必须自行选用完整数据。Linux/macOS 首次使用日期服务时注册只读内存映射，Windows 使用 Skia 的数据加载器。

`testHost` 执行当前宿主库测试；`publishHost` 输出到 `native-compose/vendor/build/repository`；`verifyHost` 校验 Maven 文件、校验和、目标、KLIB 身份、静态库、许可、依赖闭包及维护测试报告。缺失、失败或跳过的测试不能作为通过结果。

日常 Build/Test 通过共享的 `setup-build`、`setup-native` 动作恢复上游归档、准备好的源码、Kotlin/Native 工具链和 Gradle 编译缓存，不运行移植库的维护测试或本地发布。桌面 Native 作业另外恢复宿主 SDL/Skia SDK；修改 `skikoMain`、`skikoTest` 也会触发三个桌面 Native 平台的框架测试。源码快照仅使用精确缓存键，命中后恢复并由 Settings 检查、复用；未命中时自动准备并创建快照。

[native-compose.yml](../.github/workflows/native-compose.yml) 是独立的移植维护工作流，适配源码、上游锁及补丁、桥插件、同步器和相关构建配置变化时运行，普通 `ylcs-module` 或示例源码变化不触发它。仅修改版本目录时，先比较 Native 构建实际使用的版本、库和插件；框架自身版本号及无关业务依赖变化不会启动三个宿主的重型验证。维护流程保留移植回归测试、本地发布、独立 Maven 消费和三宿主工件汇总。

GitHub Actions 的 macOS Gradle 作业通过共享 `setup-native` 动作设置 `kotlin.incremental.native=false`、一个 Gradle worker、一个 Native 后端线程及 3 GiB Gradle 堆，Kotlin 编译在 Gradle 进程内执行。这些用户级属性覆盖仓库的本地开发配置，同时作用于框架 Build/Test、发布、included build 和独立 Maven 消费验证，避免冷构建重复建立按文件项目缓存及多个编译进程争用 runner 内存；Gradle build cache 和 Native 外部依赖缓存继续启用。框架 Apple Native 单元测试及移植验证程序使用链接器 `-S` 省去调试映射，测试断言和编译优化模式保持原有配置。本地构建保留原有增量编译及调试配置。维护工作流保存宿主库阶段的完整日志、Gradle 耗时报告及 Kotlin 编译报告。

源码缓存任务为 `:repository-conventions:restoreNativeSourceCache`、`:repository-conventions:createNativeSourceCache`，缓存身份由 `:repository-conventions:writeNativeCiInputs` 计算；快照排除 SDK、原始归档和编译输出。静态 SDK 缓存按宿主镜像、Kotlin 版本及 SDK 输入区分。

升级维护流程：

1. 确定 CMP、Skiko 及独立版本库的目标版本，核对官方 tag/commit、Maven source archive、Skia 资产、SDK 的 ICU 版本和社区参考变化，更新根版本目录及锁定项。
2. 在临时目录取得对应原始源码，审核和重做 `upstream/patches`。先执行 `git apply --check`，保留真实功能、平台实现和许可证。更新补丁清单的 SHA-256、提交、版本及参考身份，不能只修改版本标签。
3. 核查适配实现、源集、依赖导出、KLIB 身份、资源所有权及测试；Kotlin 位置解构等兼容性修改保留为受版本约束的补丁。
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

按需使用 `libs.compose.ui`、`libs.compose.foundation`、`libs.compose.material3`、`libs.compose.material3.windowSizeClass`、`libs.compose.material3.adaptive.navigation.suite`、`libs.compose.adaptive.layout`、`libs.compose.adaptive.navigation`、`libs.compose.adaptive.navigation3`。Material3 的基础 UI、Foundation 和 Ripple 随依赖图引入。Resources 同时应用官方 Compose Multiplatform 插件，沿用 `src/commonMain/composeResources` 和生成的 `Res`。

桌面入口位于 Native 源集，调用 `androidx.compose.ui.window.NativeWindow`。应用安装桥插件后，可在 `rachel.multiplatform` 配置：

```kotlin
composeDesktopPackage {
    entryPoint = "example.main"
    packageName = "ExampleApp"
    displayName = "示例应用"
    packageVersion = "1.0.0"
    vendor = "Example"
    author = "Example Team"
    copyright = "Copyright 2026 Example"
    windows { iconFile = file("icons/app.ico") }
    macos { iconFile = file("icons/app.icns") }
}
```

该配置与 JVM 的 `desktopPackage {}` 独立，同一模块可同时配置。`composeRun` 运行宿主 Debug，`composeReleaseRun` 运行 Release，`composeDistribution` 输出 `build/nativeApplication/<target>/releaseExecutable` 普通目录，包含 Compose 资源和平台运行数据。Windows 图标/版本信息嵌入 EXE；macOS 使用 `.app`、Info.plist 和 ICNS。自定义 `composePublish` 可消费此目录进行复制或 ZIP 打包。

Release 链接后自动分离符号，桥插件与 Rachel 的 `nativeExecutable`、`nativeLibrary` 模板共用此处理；Debug 保持原样。链接输出和应用分发目录使用剥离后的 EXE、ELF 或动态库，符号归档在模块 `build/nativeSymbols/<target>/<binary>/`：Windows/Linux 为 `<binary>.debug`，macOS 为 `<binary>.unstripped` 及链接器已生成的 `.dSYM`。`symbols.properties` 记录剥离前后 SHA-256；旁边的 `<binary>-distribution.properties` 记录嵌入应用资源后最终分发文件的 SHA-256，用于匹配对应构建。macOS 剥离后重新进行 ad-hoc 签名。符号不会复制进分发目录。请在清理 `build` 前自行归档符号；普通 Release 未生成的 DWARF 行号信息不会凭空补齐，Windows 的 `.debug` 是 COFF 格式而非 PDB。Linux 使用 `objcopy`，Windows 使用 Kotlin/Native 已下载的 MinGW `objcopy`，特殊工具链可通过 `-Prachel.native.objcopy=/path/to/objcopy` 指定。

`NativeWindow` 在创建场景前自动调用 `registerSkikoComposeImplementation()`，注册 Skiko 图形和文本后端。若自行管理底层 Compose 场景，或在窗口创建前使用 `Path()`、`ImageBitmap()`、文本排版等后端 API，需要在首次使用前完成注册。

框架库沿用自身 Maven group/GAV，发布 KMP root 及启用的 Native target publication。JVM、Android 等用户按普通方式依赖；Native 用户应用匹配版本的桥插件。公开 Compose 类型按模块 API 导出规则声明，保持消费者可见的依赖完整。
