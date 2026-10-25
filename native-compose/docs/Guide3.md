# Native Desktop / Native Compose 交接文档

更新时间：2026-10-25（Asia/Shanghai）。

工作区：D:\Rachel\ylcs\ylcs-kmp。

后续进展已更新于本文第 14 节：主 UI 阶段完成后，用户已授权继续 Foundation/Animation；本轮只构建，不测试或运行 CI。第 1、13 节的暂停要求和 WIP 状态属于更早阶段，请以第 14 节、STATUS.md 与 VALIDATION.md 为当前接续依据。

本文汇总本会话从最初 Skiko 讨论到 AndroidX 依赖整理的全部关键需求、决策、实现进度、故障经验和未完成事项，用于下一对话窗口接续。它是交接摘要，不是逐字聊天导出。历史验证、用户反馈、当前源码和待验证推断分别记录；未实际执行的检查不得写成通过。

## 1. 下一窗口先读什么、先做什么

1. 阅读根 AGENTS.md，再阅读本文，检查工作区状态与用户的新修改。
2. 本次最后获准执行的代码变更已经完成：六个 AndroidX 依赖只改 module，不改别名和版本；移除 adaptation 中两个 Compose internal 依赖，复用已有 Google annotation/collection；保留 JetBrains Navigation 3 UI。
3. 用户将先手动验证业务 APP。收到用户测试结果及继续指示前，不自动恢复主 UI 移植，也不自行启动构建、预览或测试。
4. 当前存在上一阶段留下的未提交主 UI / SDL 宿主代码，尚未完成配套单元测试、APP Composable 预览、CI 更新及编译验收。不得把它宣布为完成。
5. 不提交、不推送、不公开发布 Maven，除非用户后续明确安排。当前 Native CI 的 Maven 步骤仅用于本地 file: 验证仓库。

本次不是干净工作区上的独立完整移植提交。开始整理依赖前，工作区已经包含后文列出的主 UI WIP；这些修改予以保留。

## 2. 最终目标与项目边界

最终目标是让 Rachel / ylcs-kmp 支持无 JVM 的 Native Desktop，覆盖 mingwX64、linuxX64、macosArm64，逐步支持全部需要的框架模块、预览和业务 APP，最终发布 Maven 工件贡献社区。

项目口号：我们一定可以！

最初用户提供了 C:/Users/Administrator/Desktop/Task.md 和 Guide.md，分别作为既有任务总结与 Astra 向导。它们是背景资料，不能代替用户后续明确修正，也不能默认其中版本和当前源码一致。当前实现、根版本目录、AGENTS.md 和最新用户决定优先。

当前阶段顺序：

1. Skiko Core、Skottie、静态原生依赖和桥插件。
2. Compose UI 的基础模块、图形、文本、Skiko backend。
3. 主 ui、ui-backhandler 和 SDL ComposeScene 宿主。
4. 后续 Foundation、Animation、Rachel Compose 模块、业务 APP 等，依赖审计后逐层推进。
5. 完整发行、公开 Maven 发布和社区贡献另行安排。

不能因为库声明了 Native target 或 Maven 上有同名目标，就认定它可复用。必须检查实际目标工件、源码、actual、TODO、NotImplementedError、测试 stub 和真实调用路径。官方已经提供可用实现的工件直接使用；只移植缺失部分。

## 3. 用户已确定的长期约定

### 3.1 目录、源码和 IDE

- 自己维护、需要提交 Git 的 Kotlin/C++ 放在 native-compose/adaptation。
- 每个依赖一个目录，名称对应官方坐标，例如 org.jetbrains.skiko_skiko、org.jetbrains.skiko_skiko-skottie、org.jetbrains.compose.ui_ui-text。
- 上游下载源码、工作副本、生成文件、SDK、原生构建输出、缓存和日志放在忽略的 native-compose/vendor。
- 每个上游只保留一个固定展开工作目录。指纹仅用于输入状态和增量判断，不再用指纹目录保存多份完整源码。
- 维护者主要在 adaptation 修改代码；vendor 的准备、版本覆盖和来源文件遮蔽由脚本负责，不能要求手工修改 vendor 的 build.gradle。
- 源码跟踪采用显式锁定提交、校验与更新检查，不在普通构建时追踪远端 HEAD 并无审查升级。
- 根工程通过 Settings 约定接入，IDE 能发现 included builds 和 adaptation 源码；不要把所有原始 vendor 工程、模板和样例都作为 Gradle 同步目标。
- native-compose 根模块可以排除 vendor 缓存目录，但不能把 adaptation 排除掉。
- 不维护 nativeJsMain 这样的误导性活动源集。共享 Native 实现使用 nativeMain/nativeTest。
- 自己维护的 Apple 平台实现使用 macosMain/macosTest；当前只有 macosArm64，不保留等价的 darwinMain 层。vendor 来源映射中出现原始 darwinMain 路径不等于新增了活动平台。
- 不是所有 adaptation 模块都必须有手写平台文件。若实现来自锁定上游源码，build.gradle 接入即可，例如 Skottie、ui-backhandler 的有效共享实现。

### 3.2 维护模式与普通消费模式

native.compose 默认 false，仅控制是否维护和构建移植源码，不控制业务是否允许 Native。

| 模式 | 职责 |
| --- | --- |
| true | 维护者模式：准备锁定上游、覆盖根版本、建立源码 composite/project 替换、编译 KLIB、验证发布等 |
| false | 普通业务开发者和库消费者：不准备 vendor、不构建移植源码；需要 Native 时通过桥插件消费 Maven 工件 |

- 本地可在 local.properties 设置 native.compose=true，也支持 -Pnative.compose=true。不要读取或泄露真实 local.properties 内容。
- 维护模式直接引用源码项目，使用 included-build substitution，不要求先 publishToMavenLocal。
- 普通模式即使需要 Native Compose，也仍然是 false。
- JVM/Android 等非 Native 使用者不应因桥插件而下载 Native KLIB 或启动 vendor 准备。
- 没有 Native target 的普通消费者通常不需要此桥插件；有 Native target 且需要官方缺失工件时手动安装。
- 不自动给全部 ylcs-module 模块安装桥插件。用户会在需要 Native Compose 的模块手动安装。
- project 形式本地使用与 Maven 字符串消费必须兼容；发布元数据要输出真实可消费坐标，不能泄漏生产者路径。
- MavenLocal/file: 仓库是独立消费者测试工具，不是维护源码 APP 运行的必需条件。正式发布后普通用户使用配置的 Maven 仓库。

### 3.3 版本与依赖重定向

- 依赖和插件版本以根 libs.versions.toml 为准，Gradle 以根 Wrapper 为准。
- 移植工件版本与对应官方库版本一致，不另造 skikoNative 等版本序列。
- CMP 自带的 Skiko 版本可以低于 Rachel 主动选择的 Skiko；通过脚本生成和根版本覆盖对齐，不因此降低整个 Rachel。
- 上游源码提交的选择仍需审查 API 差异；覆盖版本号不保证自动获得 ABI 兼容。
- 桥规则至少精确到某个库、某个 target，不统一替换整个 group。
- UI backend 相关依赖必须形成一致的目标图，避免部分官方 KLIB 混入已移植 backend。
- 共享 metadata 和 IDE 源集也需要正确处理重定向，不能只修最终目标编译。
- 自定义 Maven group 与 KLIB unique_name 是两个概念。UI 移植库使用自己的 Maven group，但保留官方 KLIB 身份以满足二进制依赖。
- Native 别名放在版本目录底部，使用 native-xxx；xxx 沿用官方依赖形式，如 native-skiko-skottie、native-compose-ui-geometry。
- 本次用户特别要求现有六个 AndroidX libs 别名保持不变，仅修改 module。
- 不全局排除或统一替换 JetBrains 转发包装；上游二进制可能仍引用其 KLIB 身份。

### 3.4 消费端、预览和测试

- APP 构建文件保持 Rachel 的 install / rachel / lib DSL，依赖尽量写 commonMain 的官方坐标，由桥插件选出缺失 Native 工件。
- 业务 APP 不自行维护 SDL cinterop、sdl.def、窗口平台源码、复制任务或原生 SDK 路径。
- 打包、运行数据、run/package 等能力由 native-bridge 的 nativeApplication DSL 提供。
- 不让 ylcs-app/native-compose 依赖 adaptation 中某个 samples 项目。
- 曾经允许 adaptation/samples 做阶段验证；后续用户明确取消：适配层保留库与单元测试，真正可视预览只放 ylcs-app/native-compose。
- 预览应展示当前最新阶段，不能长期堆积旧的一次性样例。
- CI 始终推进同一个 .github/workflows/native-core.yml，不为每个移植阶段新增工作流。
- 当前用户手动提交 CI、验证业务 APP；可以不等待 CI 结果继续后续工作，但必须遵守最新暂停/测试安排。
- 当前回合只做已确认的依赖调整和交接，不运行构建、测试或程序。

### 3.5 代码、许可和发布

- 遵守 AGENTS.md。Kotlin/C++/JSON 使用四空格缩进。
- Kotlin 2.5 使用集合字面量 []、范围 0 ..< n、else/finally 单独起行等仓库风格。
- 所有移植相关 Kotlin 编译开启 -Xexpect-actual-classes，相关构建还统一开启 -Xcollection-literals。
- Gradle 脚本的嵌入式 Kotlin 与业务 Kotlin 版本不同，不机械把所有 Gradle listOf 改成新语法。
- 不额外引入 .clang-format。用户明确删除了它及独立 native-compose/licenses 目录，相关脚本不再依赖它们。
- 保留上游源码的许可证、版权和 sources 工件来源信息；不把许可证复制进 APP 运行目录或 ZIP。
- icudtl.dat 的分发保留，由插件自动处理。
- 当前禁止远程公开 Maven 发布；发布任务只允许 file: 验证仓库。最终贡献社区是长期目标，不是当前公开发布授权。
- 用户有无关业务代码修改与提交，不得覆盖、重置、清理或重写历史。

## 4. 当前版本、提交和参考项目

以下为交接时的源码快照，下一窗口先重新核对根目录：

| 项目 | 当前值 |
| --- | --- |
| Rachel | 3.7.1 |
| Kotlin | 2.5.0-Beta2 |
| Gradle Wrapper | 9.8.0-all |
| JDK / JVM target | 25 |
| AGP | 9.3.3 |
| Compose Multiplatform | 1.13.0-beta01 |
| cmp-internal 版本键 | 1.12.1；目前只用于 Google runtime-retain，未重命名 |
| Skiko | 0.156.0 |
| SDL | 3.4.18 |
| Lifecycle | 2.11.0 |
| SavedState 直接声明 | 1.4.0 |
| NavigationEvent | 1.2.0，Google 坐标 |
| Navigation3 UI | 1.2.0，JetBrains 坐标 |
| Google annotation | 1.11.0 |
| Google collection | 1.7.0 |
| Coroutines | 1.11.0 |

已锁定来源：

- Skiko：JetBrains/skiko v0.156.0，commit 71d1a686fe49bac19ef1767b9bded4800a5a67c5。
- Skiko 参考：brahmkshatriya/skiko-native，commit 85a7cf78045555f885fa774ac6007c6258d2bfb7。
- Skiko 合并基线：c4b1940eaa73abfcbaa3f3f5d5972ab0ac7b9810。
- Skia：m156-24300fbbcb；各宿主 Release 归档 SHA-256 在 upstream/lock.json。
- Compose：JetBrains/compose-multiplatform-core v1.13.0-beta01，commit bb05011eb10784ada1ec00494f1a710ba6ab6252。
- SDL 版本与下载校验在 upstream/sdl.json。

参考仓库：

- https://github.com/bitsycore/compose-desktop-native
- https://github.com/brahmkshatriya/compose-native
- https://github.com/brahmkshatriya/skiko-native
- https://github.com/JetBrains/kotlin-desktop-toolkit

用户最终选择继续 SDL3。kotlin-desktop-toolkit 可参考窗口/API/平台设计，不把它替换成新的窗口技术路线，也不认为有此仓库就代表 Skiko 已有完整官方 Native Desktop。

当前 HEAD：6a12037d2，feat: 移植compose.ui:ui-skiko模块到native。

此前阶段提交：

| 提交 | 内容 |
| --- | --- |
| 6a12037d2 | ui-skiko |
| 46c2059c1 | ui-text |
| 3748ebcaf | ui-graphics |
| 0a171b03e | ui-util、ui-unit、ui-geometry |
| 03f4a3d24 | Native CI 修复 |

主 ui、ui-backhandler 与 SDL 新输入宿主处于 HEAD 之后的未提交修改，不能当作上述提交已包含的能力。

## 5. 当前目录与构建接入

    native-compose/
        adaptation/
            org.jetbrains.skiko_skiko/
                build.gradle.kts
                src/commonMain/kotlin/
                src/nativeMain/kotlin/
                src/nativeMain/cpp/
                src/windowsMain/kotlin/
                src/windowsMain/cpp/
                src/linuxMain/kotlin/
                src/linuxMain/cpp/
                src/macosMain/kotlin/
                src/nativeInterop/cinterop/sdl.def
            org.jetbrains.skiko_skiko-skottie/
            org.jetbrains.compose.ui_ui-util/
            org.jetbrains.compose.ui_ui-geometry/
            org.jetbrains.compose.ui_ui-unit/
            org.jetbrains.compose.ui_ui-graphics/
            org.jetbrains.compose.ui_ui-text/
            org.jetbrains.compose.ui_ui-skiko/
            org.jetbrains.compose.ui_ui-backhandler/    # WIP
            org.jetbrains.compose.ui_ui/                # WIP
        vendor/                                        # 整体忽略
            skiko/workspace/
            compose/workspace/
                native-desktop/                        # 生成的 Gradle included build
            sdl-source-*/
            sdl-static/<target>/
            build/
                logs/
                compose-ui/
        scripts/
        upstream/
        verification/
        docs/
    build-logic/native-bridge/
    ylcs-app/native-compose/

关键入口：

| 文件 | 职责 |
| --- | --- |
| settings.gradle.kts 的 nativeCompose() | 根 Settings 保持单条接入语句 |
| build-logic/repository-conventions/.../NativeCompose.kt | 解析维护开关、准备源码、校验输入、关联 included builds |
| build-logic/native-bridge/.../NativeBridgePlugin.kt | 按目标重定向、报告、运行与分发 |
| native-compose/upstream/lock.json | Skiko 来源与校验 |
| native-compose/upstream/compose.json | Compose 来源、模块、源集选择、重定向目标、ciModules |
| native-compose/upstream/adaptation-origins.json | Skiko 手写/派生文件来源及遮蔽 |
| native-compose/upstream/compose-origins.json | Compose 派生文件来源 |
| native-compose/scripts/sync_upstream.py | 准备固定 Skiko 工作区、版本覆盖 |
| native-compose/scripts/skiko_adapt.py | Skiko 可重现适配 |
| native-compose/scripts/sync_compose.py | Compose 稀疏 checkout、输入校验、生成 Gradle 工程 |
| native-compose/scripts/compose_adapt.py | Compose 源码适配，按匹配条件检测上游漂移 |
| native-compose/scripts/prepare_sdl.py | 构建静态 SDL，记录系统链接依赖 |
| native-compose/scripts/templates/compose-ui.build.gradle.kts.template | UI 三目标、源集、发布、sources 统一配置 |
| native-compose/scripts/verify_compose_ui.py | KLIB/Maven/源码/测试报告审计 |

included build 名称：

- skiko：目录 native-compose/vendor/skiko/workspace/skiko；Core 项目为 :，Skottie 为 :skiko-skottie。
- compose-ui：目录 native-compose/vendor/compose/workspace/native-desktop；模块为 :ui-util 等。

适配模块自己的 build.gradle.kts 与脚本选出的上游源码共同构成实际项目。不要因为源码在 vendor、构建脚本在 adaptation，就把它们误判为未绑定的孤立工程。

桥插件有两个 Gradle 生命周期入口：

- love.yinlin.native-bridge：Project 插件，负责依赖图和应用构建。
- love.yinlin.native-bridge.settings：Settings 插件，负责 included builds / 源码替换。

它们已集中在 build-logic 的同一实现模块中。两个入口对应不同生命周期，不能简单把 Settings 行为塞进 Project.apply。保留轻量 Settings 产物。

nativeBridgeReport 可以查看规则、target、原坐标、Maven/源码替换位置；实际解析结果还可以使用 Gradle dependencyInsight。不要仅凭版本目录推断最终选中版本。

## 6. Skiko 阶段结果及边界

用户在后续对话明确报告：此前 Skiko 的三平台 CI 已全部通过。部分旧 STATUS.md 仍写旧 CI 全失败，这是用户曾允许暂缓更新文档造成的历史快照，不应当作最新结论。

但本次主 UI WIP 又扩展了 Core 窗口和输入 API，因此旧的 Skiko CI 成功不能证明这些新修改通过。

已落地的 Skiko 基线：

- Core、独立 Skottie 的三目标 Kotlin/C++ 和 KLIB 发布配置。
- Core/Ganesh 采用 Windows D3D12、Linux Vulkan、macOS Metal。
- SDL3 为窗口与事件基础，默认 CI 软件光栅基线与真实 GPU 运行分开判定。
- 静态 Skia、C++ bridge 与 SDL 绑定进入 KLIB。
- Core 功能回归覆盖图像读写、编解码、SVG、字体/段落、Skottie seek/render 等代表场景。
- Windows 曾本机启动成功，D3D12 绘制和 resize 有历史验证记录。
- Linux Vulkan 窗口层由本项目补充，不能把社区默认 OpenGL 路线误记成 Vulkan。
- 不迁移 Graphite：它没有本项目所需的直接 D3D12 路径；选择 Ganesh 不等于降低 Windows 后端为 Vulkan。
- AWT/JVM runtime、上游 CI/docs/import-generator 等不是 Native Desktop 运行工件的移植目标。
- Skottie adaptation 主要是构建接入，上游实现来自 vendor，所以目录没有大量手写 Kotlin 不代表空实现。

仍应保留的上游 API 边界：

- reachabilityBarrier 的 Native 行为、GC/回调/FFI 资源寿命需要压力验证。
- InteropPointer.fromInteropNativePointerArray 曾保留上游 TODO，现有调用路径未见使用；后续以源码再次核实。
- 若干公共迭代器 remove() 的 TODO 是上游共有限制。
- 代表性回归不等于所有 Skiko API、全部驱动、设备丢失、长时间运行都完成验收。

这些边界不能被写成全部功能逐项验证通过，也不应仅为清除 TODO 字样无必要改动上游公共 API。

## 7. 静态链接、Debug/Release 与分发约定

| 内容 | 生产端处理 | 消费端 |
| --- | --- | --- |
| SDL3 | CMake Release、静态归档；Windows /MT | 使用 KLIB 中的同一份 Release 归档 |
| Skia、zlib、图像等配套库 | 锁定官方 Skia Release 包并校验 | 不按 APP Debug/Release 重新编译 |
| Skiko/Skottie C++ bridge | 默认优化构建，skiko.debug=false | 链接生产端已编译归档 |
| Kotlin KLIB | 保存 Kotlin IR、元数据、绑定 | 最终 Debug/Release binary 决定 Kotlin 机器码生成设置 |
| Windows ICU 数据 | 保留 icudtl.dat 配套数据工件 | 插件自动 stage/run/package |

- Native 不需要 JVM、AWT runtime JAR、skiko.dll，也不分发 SDL3.dll/libSDL3.so/SDL dylib。
- 仍依赖系统库和驱动；静态 SDL 不代表静态化整个操作系统。
- Windows 通常分发 EXE + icudtl.dat；Linux/macOS 基线为可执行文件加系统依赖。
- 许可证保留源码与 sources 工件，不复制到运行目录和 ZIP。
- Kotlin/Native Debug/Release 取决于 binary/任务，不取决于是否点 IDE Debug 或是否生成安装包。
- Debug APP 仍可以链接优化后的 Release Skia/SDL/C++。
- 静态裁剪以 object/section、初始化和函数表等规则决定，不能保证所有未调用函数一定被移除。
- 用户观察的 Windows SDL 程序约 80 MiB 工作集不能直接认定为泄漏或 Kotlin 堆大小，尚无可重复分项性能基准。
- 未来需要完整安装器、macOS app bundle/签名/公证时再扩充分发 DSL。

现有 DSL：

    nativeApplication {
        skikoWindow()
    }

现有 APP 任务命名（本轮未运行）：

    .\gradlew.bat -Pnative.compose=true :ylcs-app:native-compose:runWindowsDebugExecutable
    .\gradlew.bat -Pnative.compose=true :ylcs-app:native-compose:runWindowsReleaseExecutable
    .\gradlew.bat -Pnative.compose=true :ylcs-app:native-compose:packageWindowsDebugExecutable

Linux/macOS 对应替换 Windows 为 Linux/Macos，任务需以当时配置为准。

## 8. Compose UI 已提交阶段

ui-util、ui-geometry、ui-unit、ui-graphics、ui-text、ui-skiko 已提交到当前 HEAD。主 ui 还未完成。

| 模块 | 实现来源及本项目工作 |
| --- | --- |
| ui-util | 复用有效共享逻辑；补 Windows/Linux Trace actual，保持 block 执行一次 |
| ui-geometry | 主要复用 common 实现 |
| ui-unit | 非 Android / 非 JVM 源集和 Native actual 组织、原子等依赖 |
| ui-graphics | 图形接口、Skiko backend 对接、Native 依赖和行为回归 |
| ui-text | 字体、Paragraph、Locale、同步、expect/actual 分层 |
| ui-skiko | Skia 图形/文本实现、平台字体/图像、弱引用缓存、字符边界、区域选取和像素拷贝等 |

关键约束：

- Maven group：love.yinlin.native.compose.ui。
- Kotlin/Native unique_name 保留 org.jetbrains.compose.ui:<artifact>。
- 三个桌面 Native target 重定向到适配图；普通 JVM/Android 保持官方依赖。
- ui-text 的 nonAndroid expect 与 native actual 不能合并进同一编译 fragment。当前通过源集层级与平台叶子组织避免。
- 自己重写实际实现时，要从脚本准备的活动 vendor 源码中移除被遮蔽文件，不能只靠 Gradle 文件 exclude；否则 IDEA 仍可能把两份 actual 都索引进去。
- 单元测试保留在模块 commonTest/nativeTest，不恢复 adaptation/samples。
- sources.jar 必须保留上游许可和 provenance，不能混入 StubsMain。
- 非本机 cinterop 不可伪装为跨宿主可编译；需要在对应宿主 CI 构建。

当前 verify_compose_ui.py 的测试报告契约为 46 项：

| 测试类 | 数量 |
| --- | --- |
| UiUtilTest | 5 |
| NativeRuntimeTest | 2 |
| UiGeometryTest | 3 |
| UiUnitTest | 4 |
| UiGraphicsTest | 6 |
| UiTextTest | 8 |
| NativeLocaleTest | 3 |
| UiSkikoTest | 10 |
| NativeSkikoBackendTest | 5 |

这是脚本要求的数量，不是本回合执行结果。独立 Maven 消费者目前还验证 ComposeUiPublicationTest 的 4 项测试。

历史上用户已经报告 ui-text 相关 Windows 编译和运行成功，并多次自行提交 UI CI。最近 ui-skiko 提交的三宿主 CI 结果没有在本回合重新检查，不得猜测已通过或仍失败。

## 9. 当前主 UI / SDL 宿主 WIP：代码已写，阶段未完成

这些内容在用户要求暂停移植、先审计依赖前已开始编写。本次只保留，不继续实现或构建。

### 9.1 构建和源集

upstream/compose.json 当前已经加入 ui-backhandler、ui，ciModules 已列出八个模块。

- ui：commonMain 读取上游 common；skikoMain 读取上游 skiko；nativeMain 读取 nonJvmMain/nativeMain；三个叶子继续依赖 nativeMain。
- ui-backhandler：commonMain 读取上游 common；nativeMain 读取上游 jbMain 的有效共享实现。
- sync_compose.py 已允许 bare ui 模块、skikoMain、jbMain。
- UI Gradle 模板已支持可选 skikoMain 中间层，避免把 expect 和 actual 放在同一 fragment。
- 新增 native-compose/adaptation/org.jetbrains.compose.ui_ui/build.gradle.kts。
- 新增 native-compose/adaptation/org.jetbrains.compose.ui_ui-backhandler/build.gradle.kts。
- 版本目录底部已有 native-compose-ui、native-compose-ui-backhandler；并加入常规 compose-ui-backhandler 等依赖条目。
- 桥插件 fake dependency graph 的功能回归已扩为八个 UI 工件，但新增部分尚未跑验证。
- compose_adapt.py 已加入主 UI 的 Synchronization 遮蔽、ClipMetadata 占位移除、PostDelayedDispatcher 接入等准备变更。

### 9.2 Skiko Core 新增窗口/事件 API

原 NativeWindow.run 保留，委托新的 open 路径，保持旧示例兼容。

新增公共文件：

- NativeWindowEvent.kt：焦点、键盘、鼠标、滚轮、文本、IME 编辑、文件/文本 drop 等拥有自身数据的事件。
- NativeWindowScope.kt：关闭、失效重绘、尺寸、显示比例、光标、文本输入区域控制。
- NativeWindowCursor.kt。
- NativeDesktop.kt：剪贴板和 URI。

Native 实现：

- NativeDesktop.native.kt。
- SdlWindowScope.kt。
- SdlWindowEvents.kt。
- 修改 NativeWindow.native.kt 的事件循环、生命周期和帧节流。
- 修改 sdl.def，增加 SDL 窗口/IME/光标/唤醒等 C inline 包装。

设计意图：

- 主线程拥有窗口和 UI 操作；跨线程唤醒不直接执行 UI 工作。
- 复制 SDL 文本/drop 内容，避免持有 SDL 事件字符串生命周期之外的指针。
- clipboard 返回的 SDL 分配缓冲需释放，事件字符串不能不加区分地手动释放。
- displayScale 与 pixelDensity 分别处理 UI 密度和 SDL 坐标换算。
- UI 清理先于窗口/GPU 资源销毁。
- 连续绘制按剩余帧时间节流，避免额外固定等待叠加 GPU 阻塞，也避免软件动画忙循环。

上述意图尚须编译与行为测试证明。

### 9.3 主 UI 新增实现文件

目录：adaptation/org.jetbrains.compose.ui_ui/src。

commonMain：

- androidx/compose/ui/window/NativeWindow.kt：Composable 窗口入口 expect。

nativeMain：

- input/key/Key.native.kt：全部 common Key 声明，SDL scancode 映射与不支持实体键的独立编码。
- input/pointer/NativePointerIcon.kt。
- input/pointer/util/PlatformVelocityTracker.native.kt。
- focus/Focusability.native.kt。
- viewinterop/InteropView.native.kt。
- draganddrop/DragAndDropEvent.native.kt。
- draganddrop/DragAndDropTransferData.native.kt。
- platform/ClipEntry.native.kt、ClipMetadata.native.kt。
- platform/NativeClipboard.native.kt、SdlClipboard.kt。
- platform/PlatformUriHandler.native.kt。
- platform/Synchronization.native.kt。
- platform/NativeUiDispatcher.kt。
- platform/NativeInputEvents.kt。
- platform/NativeTextInputService.kt。
- platform/NativeSceneHost.kt。
- window/NativeWindow.native.kt。

宿主目前尝试复用 CanvasLayersComposeScene、FrameRecomposer、SingleComposeSceneRenderingScope、DefaultArchitectureComponentsOwner，接入生命周期、SavedStateHandles、NavigationEvent、焦点、文本和鼠标输入。

现代文本输入请求与旧 PlatformTextInputService 均已有接入草稿；SDL 的 codepoint 范围需转换为 Kotlin UTF-16 位置。输入捕获、取消传播、队列清理和重复关闭仍要验证。

拖放目前只是 SDL 可提供的接收文件/文本路径，不代表完成所有 OS outgoing drag、跨进程协商及可访问性。InteropView 泛型占位也不代表已实现所有本机控件嵌入能力。

### 9.4 尚未完成的配套工作

1. 主 UI、ui-backhandler 的有意义单元测试：真实 ComposeScene 布局/绘制/状态更新、指针和键盘、焦点、Dialog 返回、IME、Dispatcher、生命周期及清理。
2. 把 ylcs-app/native-compose 更新为独立的真实 Composable 展示。当前仍是旧 ShowcaseScene 类 + NativeWindow.run 的 Skia/Paragraph 示例。
3. APP 需要为真实 Composable 源码安装 composeCompiler 插件；保持构建 DSL 简洁，不放手工 task/interops。
4. 更新独立 Maven 消费者，覆盖主 UI 的 Composable 执行，而不只是已有基础 API。
5. 更新 verify_compose_ui.py 的导出依赖和新测试报告契约。
6. 更新同一个 native-core.yml 的测试任务、阶段描述和断言。
7. 给新增派生 actual 文件补齐必要的版权/来源映射，复查 @Stable、资源所有权和仓库代码风格。
8. 审查 Dispatcher 的退出/取消/重入和 Host 构造失败清理，不能只凭代码意图宣布正确。
9. 刷新生成源码后检查 IDE，再由获准的对应宿主构建和 CI 完成验证。
10. 后续再决定完整桌面窗口 API、多窗口、无障碍、本机 View 和正式发行的边界。

当前存在一项阶段不一致：compose.json 的 ciModules 已是八个模块，所以 publishHostToVerificationRepository 会尝试发布主 ui 和 ui-backhandler；但 workflow 的显式测试和报告仍是六模块/46 项，消费者也仍是六模块。不要误以为主 UI 的渐进 CI 已准备完成。

### 9.5 此前本地验证边界

- 曾完成八模块的源码准备；那时目录生成成功不代表 Kotlin 编译通过。
- 曾做配置和依赖图审计，因误用 org.jetbrains.androidx.navigationevent 的缺失 Windows 工件失败；后来已切换 Google navigationevent。
- 该配置审计尚未在本次最终 AndroidX 调整后重跑。
- 新主 UI 未完成 Native 编译、链接、测试或预览。
- 新 sdl.def C wrapper 需要重建 cinterop；IDE 旧 KLIB 可能临时不认识新符号，不能仅凭此认定实现不存在，也不能忽略真实 API 错误。
- 本回合只做静态校验，不刷新 vendor 生成产物。版本目录变化后，维护模式下一次准备/同步会重新生成状态。

## 10. AndroidX 审计结果与本次实际变更

### 10.1 用户最终选定并已完成的六项替换

只改 module。以下别名、版本/ref 保持不变：

| 别名 | 修改后的 module | 版本 |
| --- | --- | --- |
| androidx-lifecycle | androidx.lifecycle:lifecycle-runtime-compose | lifecycle = 2.11.0 |
| androidx-viewmodel | androidx.lifecycle:lifecycle-viewmodel-compose | lifecycle = 2.11.0 |
| androidx-navigation3-viewmodel | androidx.lifecycle:lifecycle-viewmodel-navigation3 | lifecycle = 2.11.0 |
| androidx-viewmodel-savedstate | androidx.lifecycle:lifecycle-viewmodel-savedstate | lifecycle = 2.11.0 |
| androidx-savedstate | androidx.savedstate:savedstate | 1.4.0 |
| androidx-savedstate-compose | androidx.savedstate:savedstate-compose | 1.4.0 |

原 libs.androidx.lifecycle、libs.androidx.viewmodel、libs.androidx.navigation3.viewmodel 等调用不改。

已经使用 Google，未再次改动：

- androidx.annotation:annotation:1.11.0。
- androidx.collection:collection:1.7.0。
- androidx.navigationevent:navigationevent-compose:1.2.0。
- androidx.compose.runtime:runtime-retain:1.12.1。

保持 JetBrains：

- org.jetbrains.androidx.navigation3:navigation3-ui:1.2.0。
- org.jetbrains.compose.runtime:runtime:1.13.0-beta01。
- org.jetbrains.compose.runtime:runtime-saveable:1.13.0-beta01。

最后两项在审计中也被识别为可选 Google 替换候选，但用户本次只批准六项 AndroidX，不扩展修改。

### 10.2 internal 的本次清理

移除了以下五个模块的 libs.compose.annotation.internal / libs.compose.collection.internal 直接依赖：

- ui-util：移除只包含 internal collection 的空 nativeMain 依赖块。
- ui-unit：保留 Native atomicfu，移除两项 internal。
- ui-graphics：保留 Skiko/atomicfu，移除 internal collection。
- ui-text：保留 Skiko/atomicfu，移除 internal collection。
- ui：保留其他 skikoMain 依赖，移除两项 internal。

既有 commonMain 或依赖模块已提供 Google annotation/collection，不重复添加同一依赖。

同时移除版本目录中两条已无调用的 compose-annotation-internal、compose-collection-internal 声明。保留 cmp-internal = 1.12.1，因为 runtime-retain 仍引用它；没有改版本或额外重命名。

### 10.3 审计依据和不能忽略的区别

- 六项候选的 Google 工件已经发布三个桌面 Native KLIB，并检查其 manifest、包元数据和平台源码；同时核对 JVM/iOS/JS/Wasm 等发布元数据与实际文件存在。
- 这些当前 JetBrains 版本主要是空转发包装，再依赖同版本 Google 实现；切换直接坐标不会凭空更换一套 ViewModel 基础实现。
- Google Navigation3 runtime 1.2.0 已有可用 Native 实现。
- Google Navigation3 UI 1.2.0 的非 Android 默认转场调用 implementedInJetBrainsFork()，会抛 NotImplementedError；jvmStubs/linuxx64Stubs 不是真正的桌面支持，不能替换 JetBrains UI。
- 当前 JetBrains navigation3-ui 也没有 Windows/Linux Native 工件，后续仍需移植，不能靠改 group 解决。
- Google lifecycle-viewmodel-navigation3 依赖有效的 navigation3-runtime，不要求使用 Google navigation3-ui，因此它可以独立替换。
- Google lifecycle-viewmodel-savedstate 2.11.0 的非 Android SavedStateViewModelFactory 仍有 TODO，默认 create 委托 Factory，不能依赖 Android 反射式自动构造能力。相同版本 JetBrains 也转发到它，所以不是此次新引入的限制。
- 当前 ScreenStore 和 ScreenManager 使用显式 viewModelFactory，未依赖该自动构造路径。
- Lifecycle 中捕获 Dispatchers.Main 缺失的 NotImplementedError 是既有 fallback，不等于整个 Native 库未实现；线程语义还需由宿主正确处理。
- Compose runtime 的 “Implemented as an intrinsic” 需要结合编译器 intrinsic 判断，不能把所有 NotImplementedError 字样机械当作平台缺失。
- collection 中的优化 TODO 与 Skiko Linux 的未实现 actual 性质不同。

两个 internal 工件已发布的 1.11.1 / 1.12.0-alpha02 本身不含独立注解或集合功能代码：Linux/macOS 直接转发 Google，Windows/JS 等有空兼容 KLIB 保留 unique_name。

它们没有发布 1.12.1；当时查询 POM 与 .module 均 404。Google runtime-retain 1.12.1 存在，不能共享版本数字就推断两个发布方的工件都存在。本次移除失效声明已消除此处直接引用。

### 10.4 不全局清空 JetBrains 包装的原因

实际检查过官方 ui-macosarm64:1.13.0-beta01 的 KLIB manifest，它依然登记了若干 JetBrains Lifecycle、NavigationEvent、SavedState、Runtime 包装身份。

因此：

- 可以修改我们自己的直接 module。
- 可以从自己重编译的 adaptation 删除不需要的 internal 直接依赖。
- 不全局 exclude 所有 org.jetbrains.androidx / internal / runtime 包装。
- 不承诺修改后整个解析图里不再出现 JetBrains 包装。
- Gradle group 不同，版本号相同也不自动视作同一个组件；最终要结合 Gradle metadata 与 KLIB identity 检查。

实际解析版本还可能被传递依赖抬高。例如 Google runtime-saveable:1.13.0-beta01 要求 Google savedstate-compose:1.5.0；catalog 中 savedstate 的直接声明 1.4.0 不能单独证明最终解析仍是 1.4.0。本次未擅自升级直接声明或新增强制版本规则。

### 10.5 一手来源与本机审计证据

一手来源：

- https://developer.android.com/jetpack/androidx/releases/lifecycle
- https://developer.android.com/jetpack/androidx/releases/savedstate
- https://developer.android.com/jetpack/androidx/releases/navigation3
- https://developer.android.com/jetpack/androidx/releases/navigationevent
- https://dl.google.com/dl/android/maven2/androidx/navigation3/navigation3-ui/1.2.0/navigation3-ui-1.2.0-sources.jar
- https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate/2.11.0/lifecycle-viewmodel-savedstate-2.11.0-sources.jar
- https://repo.maven.apache.org/maven2/org/jetbrains/compose/annotation-internal/annotation/maven-metadata.xml
- https://repo.maven.apache.org/maven2/org/jetbrains/compose/collection-internal/collection/maven-metadata.xml

本机外部检查目录（不是构建输入，不提交仓库）：

C:/Users/Administrator/.codex/visualizations/2026/10/23/01a1694b-396e-78e2-a0f5-cc2f7ffdb840/androidx-audit/

其中保存：

- roots.json、leaves.json：发布元数据汇总。
- native-artifacts.json：实际 Native KLIB manifest 和文件检查。
- native-source-review.json：42 个对应目标源码包的审查记录。
- artifact-existence.json：109 个实际发布文件 URL 的存在检查。
- extra.json、compatibility.json：internal 与 Runtime 转发检查。
- runtime-native-confirmation.json：Google Runtime 同版本 Native 工件补充验证。
- official-ui-klib-identities.json：官方 UI 依赖身份示例。
- 下载的 .module、.klib、sources.jar。

这些是 2026-10-25 的检查快照，不是用户 APP 的编译或运行证据。若下个窗口升级版本，应重新检查。

## 11. CI、IDE 和历史故障经验

### 11.1 CI 当前状态与发布边界

文件始终是 .github/workflows/native-core.yml，当前名称 Native Compose UI。

- 三宿主：Windows x64、Ubuntu Linux x64、macOS ARM64。
- Python 工具回归、桥插件 TestKit/插件校验、宿主 Skiko 基础发布、UI 库测试和本地 Maven 文件。
- 汇总三个目标的 file: 仓库，再在独立消费者中验证官方 common 请求重定向与显式 Native Maven 请求。
- 消费者检查没有准备 vendor 源码，避免“看似 Maven 消费，实际偷偷依赖源码项目”。
- 不向公开 Maven 推送，不提供公开发布凭据。
- 现有 stage/run/package 对 Windows ICU、ZIP 可执行权限和搬移运行有专门处理。
- 当前 workflow 的显式 UI 行为测试仍停在 ui-skiko 阶段，主 UI WIP 尚未同步完成。

不因用户说“不必等待 CI”就省略测试设计或把未通过的状态写成通过。用户后续有 CI 问题时读取真实最新日志；本次没有检查新的 GitHub run。

### 11.2 IDE 同步与 expect/actual

历史上出现：

- templates/*.gradle.kts 被错误加入同步目标。
- Missing dependsOn 警告。
- ui-graphics 的 linuxMain 无法解析官方 Skiko 0.156.0。
- ExperimentalUnitApi 未解析 opt-in。
- ui-text 的 expect/actual 被合并在同一 fragment。
- vendor 与 adaptation 同步实际实现文件同时被 IDEA 索引。
- 非宿主 cinterop 警告，ignoreDisabledTargets 无法消除所有诊断。
- JDK native access 提示。

这些问题已分阶段处理，用户曾报告同步无红色错误；但主 UI WIP 的新增源集与 cinterop 仍要重新检查。

经验：源集、metadata 解析、target 解析和 IDE 索引分别处理；不要只通过最终命令行编译就认定 IDE 修复完成，也不要为隐藏警告禁用需要支持的目标。

### 11.3 Windows 原生链接

ui-text 阶段曾出现：

- libcmt 与 MinGW CRT 的 atexit、TLS 等重复符号 warning。
- ICU 类型被编译为 dllimport，LocaleUtils.o 中 StringPiece、ByteSink、Locale::isBogus 等符号无法链接。

用户补全日志后，真正导致失败的是末尾 undefined symbol，不能仅凭前两条 warning 猜根因。

既有修复涉及静态 ICU 编译定义、归档 COFF 默认 CRT 指令过滤及 MSVC/MinGW 边界。用户后来报告 Windows 编译与运行成功。后续若有类似问题先检查完整末尾日志，不通过宽泛忽略或强制允许重复定义掩盖错误。

Linux 曾受 SDL 使用宿主新 glibc 头文件、sysroot ABI 和窗口系统链接依赖影响；SDL 必须使用 K/N 选择的 Clang/toolchain/sysroot，宿主开发头文件后置。macOS linker framework 参数及 sources Jar 的任务类型也有既有修复，修改生成器时避免回退。

### 11.4 其他源码警告

已处理过的类型包括：

- NativeWindow.run actual 参数缺 @ParameterName。
- 无意义 inline、未用返回值、冗余 cast。
- PaintFilterCanvas 对内部 Optional/SymbolName 注解的直接引用。
- Synchronization 的 PublishedApi 可见性和 EXACTLY_ONCE contract 不匹配。
- AnnotatedString 无必要 !!。
- 保留旧 Paragraph API 的局部 deprecated 桥接警告。
- WeakKeysCache / NativeSkikoBackendTest 对私有或不存在 Key 符号的错误引用。

这些不代表新版本可以全局 suppress。源码准备 patch 需检测匹配范围，升级上游后重新审查。

## 12. 环境与工具

- 系统：Windows PowerShell。
- JDK：D:/SDK/Java/25，IDE Gradle JVM 也应为 25。
- 用户 MSVC：E:/VS2026/VC/Tools/MSVC/14.50.35717。
- VS 根目录通过 VS_PATH 使用，沿用 build-logic/public-plugin 的 BuildDesktopNativeTask 约定，不另硬编码版本。
- IDEA MCP：http://127.0.0.1:64653/stream 或 /sse；后续窗口可先检查服务器是否仍可用。
- 之前可用 MCP 工具包括 execute_terminal_command、get_file_problems；当时没有 Reload Gradle 接口，需要用户手动 Reload All Gradle Projects。
- 外部助手脚本在前述 visualizations 工作目录；它们不是项目依赖，缺失时不能影响正式构建。
- ide_request.py 是当时的 MCP 调用辅助。终端执行使用足够 timeout，例如 45000 ms；过短 MCP timeout 曾导致不完整日志。
- 工程日志集中 native-compose/vendor/build/logs；PowerShell 重定向文件可能为 UTF-16，读取时注意 BOM。
- 不把真实 secret、local.properties、签名或服务配置写入交接/日志。

Git 只读检查可使用：

    git -c safe.directory=D:/Rachel/ylcs/ylcs-kmp -c core.fsmonitor=false status --short --ignore-submodules=all
    git -c safe.directory=D:/Rachel/ylcs/ylcs-kmp -c core.fsmonitor=false diff --ignore-submodules=all
    git -c safe.directory=D:/Rachel/ylcs/ylcs-kmp -c core.fsmonitor=false diff --check --ignore-submodules=all

--ignore-submodules=all 可避开无关 miniaudio 等子模块访问问题。不要因此修改 .gitmodules 或初始化所有无关子模块。

## 13. 本次交付、验证和下一步

本次修改文件：

- libs.versions.toml。
- adaptation/org.jetbrains.compose.ui_ui-util/build.gradle.kts。
- adaptation/org.jetbrains.compose.ui_ui-unit/build.gradle.kts。
- adaptation/org.jetbrains.compose.ui_ui-graphics/build.gradle.kts。
- adaptation/org.jetbrains.compose.ui_ui-text/build.gradle.kts。
- adaptation/org.jetbrains.compose.ui_ui/build.gradle.kts。
- 本交接文件 native-compose/docs/HANDOFF.md。

本次进行的是静态检查：

- TOML 可解析。
- 六个别名保留，仅 module 从 org.jetbrains.androidx 改为 androidx。
- 所有版本与其他保留条目不变。
- Navigation3 UI 仍为 JetBrains。
- Runtime / Runtime Saveable 未扩大替换。
- adaptation 不再引用两个 internal 别名，现有 Google 依赖仍可通过源集/导出图获得。
- 全部失效 internal 版本目录调用已清除。
- Git diff --check。
- 未执行构建、测试、Native 链接、IDE 同步、程序运行或新的 CI。

下一窗口建议顺序：

1. 先确认用户手动业务测试结果，处理此次坐标调整暴露的问题。
2. 用户确认继续后，重新准备固定 vendor 工作区，使生成版本目录、状态指纹和活动源码与最新输入一致。
3. 检查主 UI 新增 API、源集和依赖图；不要把本机 config-only 审计当 Native 编译通过。
4. 完成主 UI 的有意义单元测试与独立 Maven 消费验证。
5. 更新 ylcs-app/native-compose 为真正的 Composable 最新成果展示，保持 common 依赖和 Rachel DSL。
6. 同步推进既有 CI、审计脚本和报告契约，明确 CPU 与 GPU、宿主差异。
7. 用户提交并检查三个平台结果后，再进入下一依赖层。
8. 公共 Maven 发布、完整业务实装、多窗口/无障碍/发行等仍是后续阶段。

不要在接续时回到“先讨论目录/插件是否存在”的起点，也不要越过本次用户业务测试的暂停点。遇到交接文字与新源码不一致，优先核对当前源码并向用户清楚说明差异。

## 14. 后续进展：UI 完成与 Foundation / Animation

主 UI 后续已完成八模块 Windows 编译、库内 72 项测试、真实 Composable 预览与独立 Maven 消费；上一阶段详情见 COMPOSE_UI.md 与 VALIDATION.md 的历史记录。用户已自行开始三宿主 CI，本轮没有查询其远程结果。

用户随后授权不等待 CI，先简化消费者依赖，再迁移全部 Foundation/Animation 运行时工件，并明确只需简单构建，不需要测试或 CI。

- 已用只含 lib(libs.compose.ui) 的 APP 完成 Windows 编译，确认 UI 子工件与 Skiko 可传递获得；此前逐个列出依赖和 asProvider() 均无需保留。
- 当前 APP 使用 Foundation/Animation，因此最后只声明 lib(libs.compose.foundation)。animation-graphics 与 Skottie 为可选独立依赖，用到时再加。
- 已加入 foundation-layout、foundation、animation-core、animation、animation-graphics 五模块；三个 Native target 的配置、源码选择、桥插件和发布模板扩展到十三模块，官方 KLIB 身份保持不变。
- 官方 macOS/desktop 的通用实现进入维护层，补齐文本编辑、选择菜单、语言、滚轮、SDL 剪贴板与 ReceiveContent 拖入；Animation 使用稳定 Native 线程身份。源码选择和适配详情见 FOUNDATION_ANIMATION.md。
- 最新预览使用 BasicTextField、SelectionContainer、LazyColumn、clickable 与真实 Animation API，保留原公共 NativeWindow/Dialog 入口。
- Windows 五模块 KLIB、共享元数据与最新 APP 链接已通过；最终日志 foundation-animation-final.log 的 EXE 临时占用失败，经 foundation-animation-final-link.log 重试链接成功。
- IDEA MCP 已由用户重新打开；Gradle 构建可用，但新依赖仍需 Reload All Gradle Projects，尚未确认 IDE 导入无错误。
- 本轮没有执行测试、程序运行、CI 或 Maven 发布，没有修改 workflow、ciModules 和上一阶段测试契约；未提交或推送。

接下来由用户验证新增五模块及预览，特别是 Linux/macOS 编译、三平台 IME/菜单/滚动/动画、Maven 元数据与独立消费。基础层通过后再推进 Rachel Compose 与业务模块。保留本文原有的窗口/无障碍/拖出/正式发行等后续边界，不能从 Windows 构建推断全平台行为通过。

