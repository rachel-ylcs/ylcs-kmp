# 首轮验证记录

本文件保留各阶段的真实结果；当前维护/消费边界及版本规则以 README 和下方最新阶段为准。
本地日志统一存放在 `native-compose/vendor/build/logs/`；历史日志已迁入此目录。

日期：2026-10-24（Asia/Shanghai）。本轮使用 Windows x64、JDK 25、Gradle 9.8.0、
Kotlin 2.5.0-Beta2、Skiko 0.156.0、Skia m156-24300fbbcb、MSVC 14.50.35717、SDL 3.4.18。
构建通过用户提供的 IDEA MCP 终端执行。主工程的应用和基础库模块没有改动。

## 已实际通过

| 验证 | 结果与证据 |
| --- | --- |
| 源码准备 | 固定三个源码提交及 SHA-256；官方 0.156.0 与社区 Native 差异三方合并；独立 fork 构建逻辑编译通过 |
| Windows Skiko 核心 | C++ 桥接编译、静态归档、COFF 指令过滤、KLIB 编译和应用链接通过 |
| 离屏运行 | 像素颜色回读断言、系统字体绘制、PNG 编码通过；输出 640×360 PNG，5565 bytes |
| SDL 窗口运行 | 绘制第一帧，窗口从 800×480 缩放到 960×540 后再次绘制，释放资源并正常退出 |
| 交互窗口预览 | 独立启动实际 Native 窗口，读取窗口内容生成预览，再通过关闭消息正常退出 |
| Bridge | 8 项 Gradle TestKit 集成测试通过；项目插件和 Settings 插件的 `validatePlugins` 通过 |
| Maven 生产 | 根 KMP metadata、Windows 主 KLIB、CInterop KLIB、ICU classifier、源码包及插件 marker/实现 JAR 发布到本地验证仓库 |
| Maven 工件审计 | 主 KLIB 嵌入 27 个静态归档；manifest 链接参数不含本机源码路径；元数据文件大小/校验值匹配；源码包包含许可证、来源说明和 85 个 C++ 源文件 |
| 独立 Maven 消费 | `verification/maven-consumer` 不 include 源码 Build，从 Maven 加载已发布插件和库，编译、链接、运行并输出相同 PNG |
| 仓库外消费 | 在代理外部工作目录创建全新的消费者，只复制示例代码和版本配置，通过本地 Maven 解析插件/KLIB/ICU，重新编译、链接并运行成功 |
| 打包 | 两个 Debug ZIP 与暂存目录中的每个文件均核对 SHA-256 一致 |
| 静态检查 | Python 脚本语法、任务源码行尾空白检查、`git diff --check --ignore-submodules=all` 通过 |

Bridge 的 8 个用例覆盖：自定义目标名与按目标重定向、纯 JVM 不解析移植工件、未审核版本失败、
同 Build 项目替换、included build 直接选择、Native-only 公共 metadata、混合 JVM/Native 公共 metadata、
真正 Native 可执行文件的运行与 ZIP 一致性。

源码 Build、独立 Maven Build 和仓库外消费者的离屏 PNG SHA-256：

```text
45383e3e8339e25fb52a2471b0dbb94a333ddaa6c7bcb0b5f7b97c31ff91a71f
```

PE 导入表检查表明，离屏程序只导入 Windows 系统 DLL；窗口程序额外导入 `SDL3.dll`。
两者没有导入 Skiko DLL，也不包含 Java/JVM 运行时。Windows UCRT/系统库仍属于操作系统依赖。

## 主工程接入复核

- `native-compose` 已改为根工程下的模块组，桥插件移动到 `build-logic/native-bridge` 和
  `build-logic/native-bridge-settings`。两个示例通过根 Wrapper 和完整项目路径构建。
- Kotlin 编译器为根目录的 `2.5.0-Beta2`；Skiko 的语言/API 版本从它派生为 `2.5`。
  协程、AGP、Dokka 和 Skiko 与根版本目录一致，桥插件跟随 Rachel `3.7.1`；SDL `3.4.18`
  已纳入根版本目录，并与经过校验的下载锁核对。
- 显式清空 `SKIKO_VSBT_PATH` 后，使用 IDEA 进程已配置的 `VS_PATH=E:\VS2026`，重新编译
  C++ 桥接、KLIB、两个 Windows 可执行文件并运行成功。根任务还完成了
  `prepareKotlinIdeaImport`、Windows Maven publication 和两个 Debug ZIP。
  日志：`root-integration-final.log`，`BUILD SUCCESSFUL in 2m 51s`，90 项任务。
- 独立 Maven 消费者使用 `--refresh-dependencies --rerun-tasks` 再次验证通过，
  6 项任务全部重新执行，输出 PNG 与源码构建一致。日志：`root-integration-maven.log`，35 秒。
- 最新两个 ZIP 与各自暂存目录中的全部 7 个文件逐项核对 SHA-256 一致，离屏 PNG 哈希保持上文值。
  最新 Maven 工件审计通过，主 KLIB 仍包含 27 个静态归档。
- IDEA 导入和命令行使用相同的显式平台开关，避免上游 `isInIdea` 重新开启本轮排除的
  AWT、Web、Apple 等模块。Linux target 保留，没有通过删除目标隐藏缺失的宿主支持。
- SDL 公共头文件从已审核源码准备，在 Windows 上分别生成 Windows/Linux CInterop，
  公共化 manifest 的目标为 `linux_x64 mingw_x64`。窗口代码使用真正的 `desktopNativeMain`。
  针对 IDE 的 `cnames` 不透明句柄解析问题加入 C inline 包装后，再次完成编译、窗口缩放运行和打包。
  日志：`root-integration-window-ide.log`，`BUILD SUCCESSFUL in 31s`，65 项任务。
  这验证了 Linux 绑定生成，不替代 Linux Skiko 库的构建、链接或运行。
- 构建逻辑的 `kotlin-dsl` 语言/API 级别与现有 `public-plugin` 一致，由根 Wrapper 管理；
  它在 IDEA 中显示 `2.2`，Native Skiko/test-utils 和两个示例则已确认显示 `2.5`。
- 日志仍有上游 Gradle API 弃用、Kotlin 源码提示和非当前宿主任务的警告；这些与 Skiko 已修正的
  旧语言/API 配置不同。

## 可选接入验证

开关默认关闭，复用仓库已有的 `localProperties` 读取机制；命令行 `-Pnative.compose` 优先。
关闭时不会注册 Native 子项目、关联 vendor 源码 Build 或添加 `NativeVerification` 仓库。
桥插件仍在现有 `build-logic` 中；原工程已有的 Native target 和依赖不受此开关影响。

- 本机未设置 `native.compose` 时，使用外部 Gradle init 脚本检查最终模型：Native 子项目为 0，
  included builds 仅有 `build-logic`，Native 验证仓库不存在。
  同一调用以离线模式完成原有 `:ylcs-module:core:compileKotlinDesktop`。
  日志：`root-integration-disabled.log`，`BUILD SUCCESSFUL in 26s`，23 项任务。
- 关闭后的 IDEA 模型中没有 Native Compose、Skiko 及其 buildSrc 源码模块，
  现存 vendor 源文件经 `ProjectFileIndex.isExcluded` 检查为 `true`。
- 本机 `local.properties` 设置 `native.compose=true` 后，不传同名命令行参数，模型检查确认
  4 个 Native 子项目、`skiko` included build 和 Native 验证仓库均已接入。
  原有 core Desktop 编译与两个 Native 示例的实际运行在同一离线调用中通过。
  日志：`root-integration-enabled.log`，`BUILD SUCCESSFUL in 15s`，51 项任务。
- 本机开关为 `true` 时，命令行 `-Pnative.compose=false` 再次得到完整的关闭模型。
  日志：`root-integration-override.log`，`BUILD SUCCESSFUL in 8s`，21 项任务。
- 开启后的 IDEA 中，窗口共享源码与 vendor 内 Skiko 源码均有自己的 Native 模块，
  `isExcluded=false`，语言/API 均为 `2.5`；窗口 `Main.kt` 的 IDE 错误检查为 0。
  根 `settings.gradle.kts` 与 `build.gradle.kts` 的 IDE 错误检查也均为 0。
  最终保留本机开关开启，其他检出仓库的使用者仍默认关闭。

## Settings 逻辑整理复核

根 `settings.gradle.kts` 的 Native 接入收敛为一句 `nativeCompose()`。
开关读取、模块注册、验证仓库和 vendor 源码校验/关联移到
`build-logic/repository-conventions/src/main/kotlin/love/yinlin/repository/NativeCompose.kt`，
复用原有仓库 DSL；通用 Settings 桥插件只在开启时应用。

- `-Pnative.compose=false`：模型检查仍为 0 个 Native 子项目、仅有 `build-logic` included build、
  无 Native 验证仓库；仓库约定插件编译及 `validatePlugins` 通过。
  日志：`settings-extraction-disabled.log`，`BUILD SUCCESSFUL in 41s`。
- 本机 `native.compose=true`：模型检查确认 4 个 Native 子项目及 Skiko 源码关联，
  Windows 窗口重新编译、链接并完成两帧缩放冒烟检查。
  日志：`settings-extraction-enabled.log`，`BUILD SUCCESSFUL in 28s`。
- IDEA 同步后，根 `settings.gradle.kts` 和新的 `NativeCompose.kt` 错误检查均为 0。
- `git diff --check` 及新增/更新文件的行尾空白检查通过。本次没有改动 Skiko 或 SDL 的绘制实现。

## 复现入口

以下命令从仓库根目录执行，先在根 `local.properties` 中设置 `native.compose=true`，
并按 README 完成源码和 SDL 准备。

```powershell
$env:VS_PATH = 'E:\VS2026'
.\gradlew.bat :build-logic:native-bridge:test :build-logic:native-bridge:validatePlugins :build-logic:native-bridge-settings:validatePlugins
.\gradlew.bat "-Pnative.smoke=true" :native-compose:publishSkikoToVerificationRepository :native-compose:adaptation:samples:skiko-offscreen:runWindowsDebugExecutable :native-compose:adaptation:samples:skiko-window:runWindowsDebugExecutable :native-compose:adaptation:samples:skiko-offscreen:packageWindowsDebugExecutable :native-compose:adaptation:samples:skiko-window:packageWindowsDebugExecutable
.\gradlew.bat :build-logic:native-bridge:publishAllPublicationsToLocalVerificationRepository :build-logic:native-bridge-settings:publishAllPublicationsToLocalVerificationRepository
python native-compose/scripts/verify_repository.py --target mingw_x64
.\gradlew.bat -p native-compose/verification/maven-consumer runWindowsDebugExecutable
```

生成物（均被 Git 忽略）：

- `build/repository/`：Skiko 本地验证 Maven 仓库。
- `../build-logic/build/native-bridge-repository/`：插件实现与 plugin marker 的验证仓库。
- `samples/skiko-offscreen/build/nativeApplication/windows/debugExecutable/`：离屏程序和 PNG。
- `samples/skiko-window/build/nativeApplication/windows/debugExecutable/`：窗口程序、SDL3.dll 和 ICU 数据。
- 两个 sample 的 `build/distributions/`：Debug ZIP。
- `../build-logic/native-bridge/build/reports/tests/test/index.html`：集成测试报告。

## Core 三平台基线与 CI 接入

日期仍为 2026-10-24。本节是后续新增验证，不将前面的 Windows 结果扩展解释为三平台已通过。

- Core 与 test-utils 恢复 `macosArm64`，不增加 `macosX64`；保留 `mingwX64`、`linuxX64`。
  版本继续为根目录的 Kotlin 2.5.0-Beta2 / Skiko 0.156.0。
- macOS ARM64 Skia 归档已实际下载并校验 SHA-256；Mach-O 静态符号检查确认 Ganesh 归档包含
  `GrDirectContexts::MakeMetal` 和 `MakeVulkan`。这不是 macOS C++、KLIB 或运行验证。
- Windows 重新完成 Core 的 C++/KLIB 编译、两个样例链接和 SDL 缩放运行。
  新增 4 项真正的 Native 测试全部通过：光栅/回读、三种图片编解码及无效输入、SVG 绘图、字体/双向文本排版。
  字体排版测试暴露 ICU 必须位于测试二进制旁的问题；已为测试补充对应暂存任务，未跳过或降低断言。
- 桥插件新增 macOS 目标重定向测试，9 项 TestKit 集成测试全部通过；两个桥插件的 `validatePlugins` 通过。
  Windows Core 测试、插件测试、样例运行和 Debug ZIP 的合并调用：
  `core-three-targets-windows-final.log`，`BUILD SUCCESSFUL in 53s`，60 项任务。
- 两个 Windows Debug ZIP 解压至新的带空格路径后，离屏和 SDL 缩放再次运行通过。
- 新的三目标根元数据与 Windows publication、本地插件 publication 生成通过：
  `core-three-targets-publish.log`，`BUILD SUCCESSFUL in 24s`。Windows 工件审计通过，仍嵌入 27 个静态归档。
- 独立 Maven 消费者使用 `--refresh-dependencies`，从新发布的插件和 Skiko 工件重新编译、链接、运行通过：
  `core-three-targets-consumer.log`，`BUILD SUCCESSFUL in 54s`，6 项任务全部执行。
- Maven 汇总及 ZIP 边界/权限工具的 4 项 Python 测试通过；actionlint 1.7.12 对
  `.github/workflows/native-core.yml` 的结构、表达式和 runner 检查通过（未配置 shellcheck）。

新增 `Native Skiko Core` 工作流提供三个宿主的构建/Native 测试/样例/ZIP 迁移运行，以及独立的
三宿主 Maven 消费矩阵。汇总时只取一份 Apple 根 publication，分别保留三个目标，避免覆盖元数据。
首次交付时尚未在远程 GitHub 执行；后续的首次 CI 结果及修复见下一节。
后端路线及两个社区实现的源码比较见 `RENDERING.md`。

## 维护模式、官方版本对齐与首次 CI 修复

日期：2026-10-24。当前版本统一为官方 Skiko `0.156.0`，移植工件通过
`love.yinlin.native.skiko` group 区分。版本目录只有 `skiko` 版本项，`skikoNative` 是复用其版本的工件别名；
源码锁不再维护独立的移植版本。

- `native.compose=false` 是普通消费模式；是否需要桥插件由模块显式决定，声明 Native target 本身不要求桥插件。
  普通 `ylcs-module:core` 不自动安装桥插件。需要移植依赖的 `compose:core` 等模块由维护者手动安装。
- `native.compose=true` 是移植源码维护模式，自动重现锁定源码并选择本地 project；没有项目映射时禁止退回 Maven。
  不再提供 `native.source` 二级模式。测试/私有 Maven 仓库仅由显式的 `native.repository` 注册。
- 实际解析模型断言通过：维护模式的 Skiko 是 `ProjectComponentIdentifier`（`project ':skiko'`），
  不是 Maven 模块，且没有默认 Native 验证仓库。原工程 `ylcs-module:core` 的普通模式 Desktop 编译通过，
  同时确认没有 Native Compose 子项目/源码 Build，也没有自动应用桥插件。
  日志：`maintainer-official-version.log`、`ordinary-mode-manual-bridge.log`。
- 当前版本的 Windows Core 4 项 Native 测试、桥插件 11 项 TestKit 测试、插件验证、SDL 缩放、
  本地 Core 与桥插件 Maven 发布通过：`maintainer-official-version.log`，`BUILD SUCCESSFUL in 1m 22s`。
- `0.156.0` 工件审计通过，仍嵌入 27 个静态归档。独立 Maven 消费刷新依赖后重新编译、链接、运行通过：
  `maven-official-version-consumer.log`，`BUILD SUCCESSFUL in 37s`。

首次 [GitHub CI 40048326466](https://github.com/rachel-ylcs/ylcs-kmp/actions/runs/40048326466) 的 Windows job 通过；
Linux 与 macOS job 均完成 C++ 桥编译和静态归档，在 `mergeSkiaArchives*` 失败，错误相同：
`Cannot convert 'null' to File`。这是 `MergeStaticArchivesTask.run` 将可选的空 `cwd` 显式传给 Gradle 9 导致的，
不是这两个平台的 C++ 编译失败。修复由 `scripts/skiko_adapt.py` 重现，仅在 `cwd != null` 时设置工作目录。

本地使用真正的 Gradle ExecSpec 复现原错误，并调用当前生成的归档任务 helper 验证空默认目录和显式目录均成功：
`archive-working-dir-regression.log`，`BUILD SUCCESSFUL in 2s`。
CI 失败片段集中在 `ci-40048326466-linux.log` 和 `ci-40048326466-macos.log`。
工作流已增加 `--stacktrace`。修复后的 Linux/macOS 完整流水线仍需由新的 GitHub 提交运行确认。

目录整理：文档集中在 `docs/`，来源及许可在 `licenses/`；根部 25 份日志与代理验证日志集中在 `vendor/build/logs/`。
清理了 17 份带指纹标记的旧生成源码缓存、3 份经逐文件比对确认相同的旧源码副本、旧插件构建缓存、
旧独立工程缓存和临时解压运行目录，共 29 个生成目录；另删除了 2 个过时的本地 Maven 版本目录。
保留当前准备的源码、按 hash 缓存的原始源码、下载归档及 SDL SDK。清理明细在 `cleanup.log`。

## Ganesh、Skottie 与可提交的适配源码

日期：2026-10-24。Core 与 Skottie 继续使用根目录选定的 Skiko `0.156.0` 和 Kotlin `2.5.0-Beta2`。
本阶段排除 Graphite；窗口默认明确要求 Windows D3D12、Linux Vulkan、macOS Metal。

- 按官方坐标建立 `adaptation/org.jetbrains.skiko_skiko/` 与 `adaptation/org.jetbrains.skiko_skiko-skottie/`。
  Core 的 34 份已采用的社区平台源文件和 3 份本项目新增文件成为可提交的 Kotlin/C++ 源码。
  每个模块的 `build.gradle.kts` 实际注册缺失 target、source roots 或 Core API 项目依赖；
  继续复用 vendor 中上游构建工具。编译直接读取 adaptation，生成目录不保留遮蔽的同路径副本。
  原始社区文件路径、提交及许可继续可追踪，详见 `SOURCES.md`。
- 构建输出集中到 `vendor/build/`；完整上游与准备结果位于 `vendor/skiko/`。
  `adaptation-layout-windows.log`：`BUILD SUCCESSFUL in 1m 49s`，126 项任务。
  包含 Core/Skottie 的 C++、KLIB 和 Windows Maven publication、6 项 Native 测试、
  Linux Core/窗口 Kotlin 编译，以及桥插件 12 项 TestKit 测试；没有跳过失败断言。
- Windows GPU 窗口实际报告 `Skia Ganesh/Direct3D 12 (NVIDIA GeForce RTX 4060)`，
  绘制与 `800x480 -> 960x540` resize 通过。示例会检查实际后端，回退不能当作 GPU 测试成功。
- 新增 Linux Ganesh/Vulkan WSI：loader、instance、graphics/present queue、device、surface、
  swapchain、按图像复用呈现 semaphore、有界图像取得 fence、resize 和资源释放。
  新 C++ 文件用 Kotlin/Native 附带的 Clang 和 Linux sysroot 完成 Linux ABI 语法检查：
  `linux-vulkan-syntax.log`。这不等于 Linux 的完整 C++ 链接或驱动运行。
- 工件审计通过：Core KLIB 包含 27 个静态归档，Skottie KLIB 包含独立的 4 个归档；
  Skottie 的发布元数据以 API 依赖引入 Core `0.156.0`，保留许可证并检查可移植 linker options。
- 独立 Maven 消费者只声明 Skottie，刷新依赖后重新编译、链接和运行成功，
  同时验证 Core 传递依赖与实际 Skottie 绘图：`skottie-maven-transitive.log`，31 秒、6 项任务全部执行。
  该消费者未关联 Skiko 源码 build。
- 显式 CPU 模式的窗口/离屏运行及 Debug ZIP 打包通过：`adaptation-layout-packages.log`，5 秒。
  两个 ZIP 解压至新的带空格路径后运行通过：`adaptation-layout-relocated.log`。
  Maven 汇总/ZIP 边界工具 5 项 Python 测试通过，更新路径后的工作流通过 actionlint。

上述是 Windows 本机结果及 Linux 交叉源码检查。更新后的完整三宿主 CI 尚未远程运行。
桥插件目前仍对原始依赖请求版本做精确检查；普通旧版本传递请求与最终选定版本的区分，
正在讨论版本策略，本阶段没有改变此行为，也未将 CMP 源码接入构建。

## 尚未验证或尚未实施

- Linux/macOS 的 C++ 桥编译、归档已由首次 CI 验证；修复后的 Skia 静态库合并、KLIB、链接及运行仍需新 CI 验证。
- Linux X11/Wayland 行为、macOS SDL dylib 加载及可执行权限仍需对应宿主运行验证。
- Linux Vulkan 和 macOS Metal 窗口运行；透明、多窗口、复杂缩放、设备丢失等完整 GPU 场景。
- Release 可执行文件、安装器、无开发工具的干净机器验收。
- Compose UI/Foundation、IDE 预览、Gallery、业务应用和其余框架模块；Graphite 按当前范围不适配。
- 正式 Maven Central 上传、签名与三宿主产物汇总的远程验收，以及完整 Skia 第三方二进制许可清单的发行审核。
- 本轮没有提交、推送或创建远程 GitHub fork；当前 fork 由固定官方源码和可追踪适配在本地重现。
