# YLCS Native Compose：技术讨论总结与实施提案

> **用途**：作为新 ChatGPT 对话或 Work 任务的完整交接文档。阅读本文后即可理解项目背景、已达成的判断、源代码审计结果、待验证问题、实施顺序和验收要求，无须重新讨论前期基础概念。  
> **整理日期**：2026-10-22  
> **项目仓库**：[rachel-ylcs/ylcs-kmp](https://github.com/rachel-ylcs/ylcs-kmp)（主分支 `main`）  
> **参考项目**：[brahmkshatriya/compose-native](https://github.com/brahmkshatriya/compose-native)、[bitsycore/compose-desktop-native](https://github.com/bitsycore/compose-desktop-native)  
> **文档性质**：架构提案与工作交接；尚非最终设计决议或已经通过 CI 的实现记录。

---

## 0. 供新对话直接执行的任务说明

**项目目标**：在 `rachel-ylcs/ylcs-kmp` 中建设自主维护的 **Compose Multiplatform for Kotlin/Native Desktop**，使 Windows `mingwX64` 与 Linux `linuxX64` 可以直接编译、运行不依赖 JVM 的 Compose 桌面应用。后续逐步支持仓库中现有的 Rachel UI、Gallery 和完整银临茶舍 Desktop App，同时发布可被其他 KMP 工程消费的 Maven/KLIB 工件以及易用的 Gradle 插件。

**首要设计原则**：

1. **以官方 Compose 为基准，优先复用官方工件**。对于官方已经提供所需 Kotlin/Native Target 的模块，如可确认兼容的 Compose Runtime、Kotlinx/Ktor 等，不 Fork、不重复发布。
2. **仅为官方缺少的 Native 变体移植源码并补齐平台实现**。避免完整 Fork 官方巨型仓库；通过固定版本/Commit 的源码同步脚本，在构建期间生成 Git 忽略的 `src/vendor/`，与自有 Native 代码共同编译 KLIB。
3. **保持上层调用习惯不变**。普通消费者只应用 YLCS Gradle 插件，在 `commonMain` 声明官方 Compose 依赖；插件负责在 Windows/Linux Native 目标进行必要的坐标替换、Native 宿主及资源配置，用户原则上无须在 `linuxX64Main` / `mingwX64Main` 重复引入 YLCS Compose UI。
4. **在同一代码仓库内独立维护 Native Compose 工程**，初步路径为 `ylcs-kmp/native-compose/`，建议独立 Gradle Build，以降低现有 79 个应用/框架子模块的构建耦合。
5. **跨平台窗口/输入层优先 SDL3**；图形渲染继续采用 Skia/Skiko：Windows 优先 D3D12，失败时回退 OpenGL/CPU；Linux 优先 OpenGL，回退 CPU。SDL3 与 D3D12不是互斥选项。
6. **先让 `ylcs-app/gallery` 跑通，再迁移完整 `ylcs-app/desktop-app`**；第一阶段不能被 Sketch、Haze、MMKV、媒体播放器等较大的依赖闭包阻断。
7. **固定兼容版本并自动核验**。用户倾向紧跟最新官方 CMP，但“最新上游”不等于“已经验证适配的 Native 发布版”；每次更新必须经过编译、ABI、功能和独立消费者验证。

**新任务执行时**：先重新检查三个 GitHub 仓库当前源码及构建配置，避免沿用过期版本；然后基于本文给出可执行的分阶段实施方案。若新对话明确授权直接开发，则从最小 Native Compose Demo 和模块骨架开始实际修改、编译、验收，记录每一个未能完成的阻碍，不能把静态代码审计当作通过测试。

---

## 1. 背景、范围与非目标

### 1.1 用户当前项目

- `ylcs-kmp` 是大型 Kotlin Multiplatform / Compose Multiplatform 单仓库，包含大量自有 Framework 子模块、Rachel Compose UI、自定义应用生命周期/导航、原生 FFI、网络与文件系统、音视频等。
- 当前 Windows/Linux 桌面应用主要走 **Compose Desktop JVM**；目标是**新增**真正的 **Kotlin/Native Desktop** 目标，而不是立即移除既有 JVM Desktop。
- 在 2026-10-22 前的 GitHub 审计中，仓库约有 **79 个 `ylcs-app`/`ylcs-module` Gradle 子项目**，其中 **26 个 Compose 框架模块**、**14 个 App 功能模块**。这是静态仓库数量，不等于 79 个模块需要重写。
- 版本目录 `libs.versions.toml` 当时记录：Kotlin `2.5.0-Beta1`、CMP `1.13.0-alpha02`、Skiko `0.153.0`、Ktor `3.6.0`、Coroutines `1.11.0`、Sketch `4.7.0`、Haze `2.0.1`、Lifecycle `2.11.0`。新任务务必重新读取，不可假定仍是这些版本。
- 主要桌面入口：`ylcs-app/desktop-app` → `ylcs-app/app/portal` → `app/global` 和 account、activity、community、game、music、photo、radio、viewer 等功能模块。
- 另有更轻量的 `ylcs-app/gallery`，适合作为第一阶段真实 GUI 验证项目。

### 1.2 项目目标

- `linuxX64`、`mingwX64` 产生无需 JVM 运行时的原生可执行文件。
- 自研 Rachel Compose 组件可在上述目标运行；尽量保留现有 `commonMain` API 和页面代码。
- 有可靠的 Windows/Linux Native 渲染、文字输入与 IME、资源加载、窗口生命周期、多窗口、鼠标、键盘、高 DPI、剪贴板等基础能力。
- 可按需渐进恢复现有 App 的 Sketch 图片加载、Haze 模糊、Skottie/Lottie、MMKV、文件对话框、Miniaudio/视频等能力。
- 向 Maven 发布带有正确 Gradle Module Metadata 的 KMP/KLIB 工件，提供简洁的官方坐标声明体验。
- 跟进官方 CMP/Kotlin，但有版本锁定、升级审计、测试基准和可复现发布。

### 1.3 非目标或可推迟项目

- 不是一次性重写所有 79 个子项目。
- 不强迫 Android、iOS、JS/Wasm、JVM Desktop 改用私有 Compose Fork。
- 不先行 Fork/适配当前 App 未依赖的 Material3 全生态；Material3 可在有确切需求时补充。
- 不因为社区示例依赖 Coil 就迁移 Coil。**当前 App 的图片生态使用 Sketch 4，而不是 Coil**。
- 现有 Desktop JVM 上已经是占位实现的 WebView/二维码扫描，可以在首阶段继续占位，先满足“与当前 Desktop 功能持平”。
- 暂不预设 Vulkan、Windows ARM64、完整无障碍、所有第三方扩展均在首版交付。

---

## 2. 已确认的关键技术认识

### 2.1 为什么不能只依赖官方 Common Metadata，再编译几个 `actual`

KMP 公共 Metadata 主要供公共 API/声明解析，**不能视作已包含完整 Linux/Windows 可链接实现的 Native KLIB**。假设官方 `compose-ui` 公开了 `commonMain` 代码和 iOS/macOS/JVM 变体，却没有 `linuxX64`/`mingwX64` 变体，那么：

1. 在 `commonMain` 声明 `org.jetbrains.compose.ui:ui`，会将该模块纳入 Native 目标依赖解析。
2. 原模块没有对应 Native 变体时，Gradle/KGP 不能自动从其他平台构建出缺失的实现。
3. 单独增加一个只含若干 `actual` 的 YLCS 依赖，也不会自动与官方 Common Metadata 拼装成完整 Native UI 库。
4. **需要针对缺失 Target 重新编译必需的官方公共实现源码，加上 Native-specific 源码与平台适配**，并正式发布完整可链接的 Native KLIB。

这**不要求 Fork 官方整个 Git 仓库**。可以只在构建时获取选定的源码文件，生成临时/忽略的 Vendor 目录，与自有实现一起编译。

### 2.2 Vendor 是什么

Vendor 实际上就是“取得并复制一份上游源码作为编译输入”。以 bitsycore 为例：

- `scripts/compose-fork/compose.properties` 固定上游 Commit/Ref；
- 每模块的 `compose-fork.txt` 列出所需源码及复制位置；
- `sync.sh` 下载/同步到 `<module>/src/vendor/`；
- `src/vendor/` 被 Git 忽略；只提交源码清单、自动化脚本、自有适配与修改说明。

优点是 **Git 仓库小、升级变更可追踪、上游文件可原样同步**；但本地/CI 仍需要存储上游源码和编译产物，并承担公共代码的 Native 编译成本。保留第三方许可和来源标注。

### 2.3 如何让消费者只在 `commonMain` 声明官方依赖

目标用户写法（**概念示例，插件 ID 尚未正式确定**）：

```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("dev.ylcs.compose-native") version "<compatible-version>"
}

kotlin {
    linuxX64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.animation)
        }
    }
}
```

插件在 **仅属于 Windows/Linux Kotlin/Native 的依赖解析配置**上，将官方没有对应 Native 变体的 `org.jetbrains.compose.*` 坐标替换为 `dev.ylcs.*` 私有发布坐标；已有兼容原生变体的 Runtime 等继续使用官方工件。插件还可负责 Native 宿主、资源打包、运行任务及版本检查。

关键限制：

- **Maven 依赖本身不能把 Gradle 的 `dependencySubstitution` 规则传播到消费者构建**。插件需要用户明确应用，或者用户显式声明 YLCS Fork 依赖。
- 插件必须处理实际目标配置、KGP 的公共 Metadata/IDE 解析及传递依赖；只匹配 `linuxX64` 字符串的粗糙替换规则不一定覆盖所有场景。
- Native-only 库如果本来就显式依赖 YLCS Native 工件，且正确发布 Gradle Metadata，可按正常依赖关系消费；消费者的“官方坐标自动替换”才需要 Bridge 规则。
- 必须避免官方/Fork 重复类、跨版本 KLIB 不兼容，以及 `api`/`implementation` 传递依赖错误。
- 发布**根 KMP Module Metadata** 和 `linuxX64`/`mingwX64` 各 Target 工件，不能只丢一个裸 `.klib` 到 Maven。

### 2.4 未确定的坐标和名称

`dev.ylcs.compose-native`、`dev.ylcs.compose.ui:ui`、`dev.ylcs.compose.foundation:foundation` 等只是此前讨论提出的**占位名称**，尚未确定，也未确认已发布。正式工作需要结合现有 `love.yinlin.*` Maven 命名规范设计，避免与已有构建脚本冲突。

---

## 3. 两个社区方案的对比结论

### 3.1 bitsycore/compose-desktop-native

链接：[仓库](https://github.com/bitsycore/compose-desktop-native) · [Vendor 机制](https://github.com/bitsycore/compose-desktop-native/blob/main/scripts/compose-fork/README.md) · [Bridge 插件](https://github.com/bitsycore/compose-desktop-native/blob/main/gradle-plugin/compose-desktop-native-bridge/README.md) · [路线图和欠账](https://github.com/bitsycore/compose-desktop-native/blob/main/PLAN.md) · [构建发布手册](https://github.com/bitsycore/compose-desktop-native/blob/main/TOOLING.md)

- **总体路径**：精细选择上游源码并 Vendor 到忽略目录，自行补上 SDL3 / Native Platform actual；尽量保留官方 Runtime 和已有可用底层库。
- **易用性优势**：Bridge 插件允许用户在 `commonMain` 继续使用官方坐标，通过 Native-only 依赖替换使用其工件，不用显式在 Native source set 配齐所有 Fork 模块。
- **生态优势**：仓库包含 Coil、Koin、Navigation3 等生态模块的 Native 适配示例和发布链；与本项目依赖不完全重合，尤其 Sketch 必须单独处理。
- **质量保证优势**：Vendor drift 检查、Native/JVM 视觉对比、输入 Probe、文字排版测试、性能/内存检查、跨 Host 发版脚本。
- **实现上的限制**：其项目计划记录 RTL、字体加载器、复杂文本渐变/描边、部分 GraphicsLayer 及无障碍等行为缺口；宣称的 API 覆盖率不能当作功能行为全部完成。
- **渲染架构**：SDL3 窗口；Windows/Linux 使用 Skia + **OpenGL**，macOS Metal，CPU Raster 作为回退。Windows 走自行维护的 Skiko/Skia DLL 桥接路径。
- **源码仓库较轻量**：只提交同步工具和自有实现，原样上游源码不长期纳入 Git 树。
- **审计时公开发布记录**：`v1.12.1-2`（2026-10-11）；更新必须在新会话复查。

### 3.2 brahmkshatriya/compose-native

链接：[仓库](https://github.com/brahmkshatriya/compose-native) · [Linux Demo/特性](https://github.com/brahmkshatriya/compose-native/blob/main/demo/README.md) · [Gradle 插件](https://github.com/brahmkshatriya/compose-native/blob/main/gradle-plugin/README.md) · [发布结构](https://github.com/brahmkshatriya/compose-native/blob/main/PUBLISHING.md) · [Skiko Native Fork](https://github.com/brahmkshatriya/skiko-native)

- **总体路径**：基于较完整的上游源码工程添加/扩展 Linux、Windows、macOS Native Source Set、Skiko 接口、SDL3 宿主、Gradle 插件与多平台发布；Git 源码体量显著大于 bitsycore。
- **核心功能优势**：公开 Demo 和 Skiko Native 文档覆盖更丰富的文本排版、BiDi、Brush/Shader、GraphicsLayer、Skia 特效、Linux AT-SPI 无障碍、原生 View/GPU 互操作。
- **Linux Demo**：展示 WPE WebKit、libmpv 等 Native 集成；这些是 Demo/平台集成案例，**不应误判为全平台已有开箱即用的通用 WebView/Media 库**。
- **Windows 图形后端**：SDL3 创建窗口并提供原生 `HWND`；Skiko Native `SkiaLayer.attachTo(hwnd)`，默认 Skia + **Direct3D 12/Ganesh**，可回退 WGL/OpenGL，再回退软件渲染。文档亦列出 D3D12、透明窗口与设备丢失恢复等支持，具体行为仍须真机实测。
- **消费者 API 区别**：其 README 的 `Native only` 示例，用户仍需在 `desktopNativeMain` 显式声明 Fork `ui`、`foundation`、`material3`、`desktop-native` 等依赖；插件不会无条件自动添加它们。
- **发行能力**：AppImage、Windows ZIP/MSI/安装器、macOS App/DMG，以及 Maven Central 的多 Host 发布及 Metadata 修复。
- **局限**：Windows ARM64 不支持、Windows UI Automation 未完成、源码体量/合并上游复杂度更高。
- **审计时跟进 CMP 1.13 预览版**；其 README、发布文档及提交历史提到不同的 alpha 版本，**不能直接假设当前所有源码和工件版本完全相同**，必须核验。

### 3.3 到目前为止的选型判断

**核心 Compose/Skiko 功能的广度及深度**更倾向参考 **brahmkshatriya**；**轻量源码维护、消费端接入、第三方生态 Bridge、质量验证**更倾向参考 **bitsycore**。

最终**推荐混合架构**：

| 能力 | 首选参考 | 原因 |
|---|---|---|
| Compose UI / text / Skia 高级功能 | brahmkshatriya | 更全面的 Native 图形与文本实现 |
| Windows Skiko + D3D12 | brahmkshatriya/skiko-native | 现成 Windows Native 后端与 HWND 附着路径 |
| SDL3 跨平台 Host | 两者 | 都实际落地窗口/事件循环 |
| 上游源码管理与更新 | bitsycore | 轻量 Vendor、固定 Ref、漂移审计 |
| 下游无感依赖替换 | bitsycore | 官方坐标 + Bridge Plugin |
| KGP Metadata / IDE 边界处理 | 借鉴 brahmkshatriya | 更复杂的 Native 源集和元数据处理经验 |
| Native/JVM UI 行为对照 | bitsycore | 已有 parity/probe 工作流 |
| App 多平台打包 | brahmkshatriya + 自建 YLCS 插件 | 具备各平台打包完整路径 |

**注意**：以上是设计偏好，不意味着两个项目源码能未经适配地直接混合编译。首先要审计它们的 Kotlin/CMP/Skiko/SDL 版本、`org.jetbrains.skiko` API、Source Set 层级、Skia 链接方式及许可证，并用最小编译验证验证组合可行性。

---

## 4. SDL3、D3D12、Skia、Skiko 的关系

### 4.1 分层认识

```text
Rachel App / Compose UI
  ├─ Compose Runtime、UI、Foundation、Animation、Resources
  └─ Skiko Kotlin API / Skia 2D 绘图引擎
        ├─ Windows: Direct3D 12 → OpenGL → CPU Raster
        └─ Linux:   OpenGL → CPU Raster

SDL3 Host
  ├─ 创建、销毁窗口
  ├─ 事件循环、键盘、鼠标、触摸、滚轮
  ├─ 文本输入、IME、剪贴板、系统事件
  ├─ 窗口尺寸、位置、高 DPI、显示器适配
  └─ 提供 Windows HWND 等原生句柄供渲染器附着
```

- SDL3 **不是** D3D12 的替代品。SDL3 主要抽象窗口和输入/系统集成；D3D12 是 Windows GPU 渲染 API。
- 通过 SDL3 创建 Win32 窗口后，可提取 HWND 并让 Skiko/Skia 使用 D3D12 直接绘制到窗口。
- SDL3 自身的 SDL_GPU 也可以在特定平台选用 D3D12/Vulkan/Metal，但**SDL_GPU 支持 D3D12 不代表 Skia 能自动复用 SDL_GPU Device**。现有方案是 Skiko/Skia 自己管理渲染后端。
- 直接 Win32 + D3D12 可行，但 Linux 又需要 Wayland/X11/输入法等另一套 Host；跨平台使用 SDL3 减少大量重复维护。
- Windows D3D12 不必假定总是快于 OpenGL；应针对帧调度、图片上传、图层、字体与媒体纹理实际跑基准。

### 4.2 YLCS 拟采用方案

- `SDL3 Host` 与 `Skia Renderer` 分离，抽象窗口/事件与图形后端职责。
- Windows 默认 D3D12；不支持或设备失败时回退 OpenGL/CPU。
- Linux 默认 OpenGL；失败时回退 CPU。Vulkan 可作为远期研究，不是第一阶段目标。
- 特别关注 Rachel 的透明无边框窗口、多窗口、置顶、托盘、窗口拖动，以及 Native GPU 视频互操作。

---

## 5. 建议的仓库结构与构建隔离

建议的结构（**提案，未实际建成**）：

```text
ylcs-kmp/
├── ylcs-app/                          # 既有应用，先维持原工作流
├── ylcs-module/                       # 既有 Rachel/YLCS 框架模块
└── native-compose/                    # 同 Git 仓库、独立 Gradle Build
    ├── settings.gradle.kts
    ├── build.gradle.kts
    ├── gradle/
    ├── upstream/
    │   ├── upstream-versions.toml     # 固定 Kotlin/CMP/Skiko/SDL Ref
    │   ├── manifests/                 # 每模块上游源码清单
    │   ├── patches/                   # 必需的可追踪上游差异
    │   └── sync/                      # Vendor 同步与漂移检查
    ├── compose/
    │   ├── ui/                         # 必需 Native UI/UI Text/Graphics/Utils
    │   ├── foundation/
    │   ├── animation/
    │   └── resources/
    ├── skiko/                          # 如选用上游/Fork 成品，可为依赖与构建集成
    ├── platform/
    │   ├── sdl3/
    │   └── desktop-host/
    ├── ecosystem/
    │   ├── navigation3/
    │   ├── sketch/
    │   ├── zoomimage/
    │   └── haze/
    ├── gradle-plugin/                # 插件本体运行在构建 JVM 中
    ├── tests/
    └── samples/
        └── native-hello/
```

**不要在首阶段照单建立所有子目录**：应以实际必要的模块构建闭包为准；上表用于表达分层，而不是要求所有模块一开始全部 Fork。已有官方 Native KLIB 的模块直接通过依赖引入。

现有 `ylcs-kmp` 的 `buildSrc` 已采用统一的 `KotlinMultiplatformTemplate`，且有 `windowsTarget`、`linuxTarget` 开关。当前这些开关默认 `false`，绝大多数 UI 模块尚未为 Windows/Linux Native 打开 Target。不要将当前 `desktopMain`（JVM）自动等同于未来 `desktopNativeMain`。需要新增独立 Native Source Set 层级，并明确已存在的 `skikoMain`、`nativeMain`、`windowsMain`/`linuxMain` 复用范围。

---

## 6. 当前 `ylcs-kmp` 依赖审计：最重要的迁移清单

> 以下结论来自 2026-10-22 前读取 GitHub 主分支的 `settings.gradle.kts`、`libs.versions.toml`、相关 `build.gradle.kts`、部分 Native/JVM 源码及部分上游 README。**没有执行所有 Artifact `.module` Metadata 比对，也没有实际运行完整 Windows/Linux Native 依赖解析、编译和运行测试**。下方“需要移植”表示初步判断与工作计划，不可冒充已经验证缺失目标变体。

### 6.1 Compose / Skiko / AndroidX 层

| 项目 | 当前判断 | 需要的工作 |
|---|---|---|
| Compose Runtime | 优先官方 Native KLIB | 版本兼容验证，不重复 Fork |
| Compose UI、UI Graphics、UI Text 等完整依赖闭包 | 官方缺 Windows/Linux Native 目标时需移植 | Native KLIB、文字/输入、绘制、事件宿主、窗口关联 |
| Foundation / Foundation Layout | 需要 Native 工件 | 复用官方源码并补齐 actual |
| Animation / Animation Core | 视上游每模块 Target 而定 | 仅补实际缺失的 Native KLIB |
| Compose Resources | 需要确认并补资源加载路径 | 读取资源、字体、图像、打包与路径解析 |
| Material3 | 当前 Rachel 主框架并非直接依赖 | 非首期必做；有传递依赖再移植 |
| Skiko / Skia | Linux 可评估官方 KLIB；Windows 优先研究 Fork | ABI、设备/交换链、打包、回退、GPU 资源 |
| Skottie | Linux 上游已有 Native 构建目标；Windows 特别验证 | 原生库发布、字体/资源、动画渲染 |
| Navigation3 UI、ViewModel Navigation3 | 现有 `compose/screen` 依赖 | 核实 Linux/Windows 变体，必要时补 Native UI 绑定 |
| Lifecycle / ViewModel / SavedState | 可能已有部分官方 Native 支持 | 精确核对每个所用模块和版本的变体 |
| NavigationEvent Compose | `compose/ui` 已依赖 | 核实 Native Compose 版本与上游变体 |

**技术重点**：不要只移植名为 `ui` 的一个坐标。实际完成 Compose UI 往往需要 `ui-util`、`ui-geometry`、`ui-graphics`、`ui-text`、`ui-unit`、`foundation-layout` 等相关模块。模块列表必须根据版本对应的上游依赖与 Module Metadata 自动展开。

### 6.2 图像与视觉效果生态

当前版本目录明确使用：

- `io.github.panpf.sketch4:sketch-compose:4.7.0`
- `sketch-http-ktor3`、`sketch-compose-resources`
- `sketch-animated-gif`、`sketch-animated-webp`
- `sketch-extensions-compose`
- `io.github.panpf.zoomimage:zoomimage-compose-sketch4:1.7.0`
- `dev.chrisbanes.haze:haze:2.0.1`、`haze-blur:2.0.1`

静态判断：Sketch 官方主要列 Android、iOS、JVM、macOS、Web，尚须审计 Linux/Windows Native KLIB；ZoomImage Compose Sketch4 受 Sketch 依赖闭包约束；Haze/Haze Blur 受 Compose GraphicsLayer、图形效果与 Native Renderer 兼容性约束。

**迁移原则**：先用最小静态图片能力复现 `url-image` 必要功能，再推进网络缓存、GIF/WebP、ZoomImage，最后核实 Haze Blur 的真实渲染效果。可先通过临时自研 Skia 图片加载适配层验证 App，不必第一天就把 Sketch 全家桶完全移植完成；但正式兼容性目标需要保留 Sketch 的 API 契约或明确记录差异。

### 6.3 官方 Native 支持基础比较好的依赖

- Kotlinx Coroutines、Serialization、IO、DateTime、AtomicFU：优先直接使用官方目标工件。
- Ktor Client Core、Content Negotiation、WebSockets、Serialization：优先官方。
- **Ktor WinHTTP + CIO**：`ylcs-module/foundation/network` 已分别为 `windowsMain` 使用 WinHTTP、为 `linuxMain` 使用 CIO。
- AndroidX Collection 等基础组件：已有 Native 代码使用路径；以实际所用 Maven 工件变体为准。

Android-only 的 Media3、Android Activity Compose、Android ZXing/Camera、Android Lottie 与服务端 SQLX4K、Redis、Ktor Server，不应仅因为存在于版本目录就算入 Desktop Native 必移植项。

### 6.4 自有 YLCS 模块：已有 Native 支持的基础层

已在 Gradle 配置中启用 Windows/Linux Native Target 的代表性模块：

- `ylcs-module/core`
- `ylcs-module/cs/core`
- `ylcs-module/cs/client-engine`
- `ylcs-module/foundation/context`
- `ylcs-module/foundation/filesystem`
- `ylcs-module/foundation/cryptography`
- `ylcs-module/foundation/network`
- `ylcs-module/foundation/compression`
- `ylcs-module/foundation/startup`
- `ylcs-module/foundation/fts`
- 多个 `ylcs-module/foundation/parser/*`
- `ylcs-module/platform/native-lib-loader`
- `ylcs-module/compose/startup/exception`
- `ylcs-module/native/win32/core` 已有 Win32 目标和实现。

这些不是全部“已通过测试”的意思，而是**优先复用、验收现有 Native 构建**的候选模块。

### 6.5 自有 YLCS 模块：需要重点平台化的部分

| 模块 | 现有代码/问题 | 建议 |
|---|---|---|
| `ylcs-module/compose/app` | Desktop JVM 入口用 AWT、Compose Window v2、托盘、多窗口、剪贴板 | 新建 Native Host / `actual`，适配 SDL3 窗口与系统服务 |
| `ylcs-module/compose/core` | Skiko、窗口状态以及 AWT 特有 Fixup | 为 Native 拆出独立实现 |
| `ylcs-module/compose/ui` | Animation、NavigationEvent、Desktop DragArea | 依赖补齐后迁移 Native DragArea |
| `ylcs-module/compose/platform-view` | `SwingPanel` + `java.awt.Component` | 设计 Native View Host，不能复用 SwingPanel |
| `ylcs-module/compose/components/media` | Miniaudio JNI、Windows 视频 JNI、SwingUtilities、Skia Bitmap | 复用 C/C++ 内核，JNI 改为 C ABI + Kotlin/Native cinterop |
| `ylcs-module/compose/components/url-image` | 多个 Sketch / ZoomImage 工件 | 先补上游 Native；或者阶段性替代实现 |
| `ylcs-module/compose/components/lottie` | `skikoMain` 直接使用 Skottie `Animation`/`InvalidationController` | 先确认 Skottie Native 工件、再复用业务绘制代码 |
| `ylcs-module/compose/startup/mmkv-kmp` | JVM JNI 包装的 MMKV | 复用 C++ 库，增加 Native C ABI/桥接，验证内存与线程 |
| `ylcs-module/compose/startup/picker` | Native File Dialog 的 JVM JNI | 复用 C/C++ 功能，重做 Native 接口；可考虑 SDL3 Dialog |
| `ylcs-module/compose/components/drag-drop` | Desktop 使用 AWT Transferable / DataFlavor | 使用 SDL3 Drop 事件及目标侧平台适配 |
| `ylcs-module/platform/os/window` | JVM-only | 独立 Native 平台实现 |
| `ylcs-module/platform/os/single-instance` | JVM-only | Win32 Mutex / POSIX 锁等，保持 API 兼容 |
| `ylcs-module/platform/os/auto-update` | JVM NIO、ZipFile、ProcessBuilder | Native 文件系统、解压、进程拉起与安全校验 |
| `ylcs-module/compose/components/webview` | 现 Desktop 本身是 `UnsupportedPlatformComponent` | 首期继续占位，后续按需求实现 |
| `ylcs-module/compose/components/qrcode` 摄像头扫描 | 现 Desktop 本身是占位实现 | 首期继续占位 |

特别注意：

- `media` 的 `MiniaudioAudioController.kt` 使用 `external` JNI 方法；`WindowsVideoController.kt` 使用 `javax.swing.SwingUtilities` 和 `Bitmap` 逐帧更新。Native 迁移需要重做线程、回调、像素缓存与视频帧生命周期。`LinuxVideoController` 当前是空操作，不能假装 Linux 视频已经具备完整行为。
- `compose/app/src/desktopMain/.../PlatformApplication.desktop.kt` 使用 `registerSkikoComposeImplementation`、Desktop `Window`/`Tray`、`java.awt.Desktop`/`Toolkit` 等。应在独立 `desktopNativeMain` 实现，不要整体复制到 Native 源集。
- `compose/ui` 的 AWT DragArea 自行使用 `MouseInfo`、`Window` 和 Swing/AWT 事件，Native 需用 SDL3 或平台原生窗口接口。
- `webview` 和二维码扫描当前 Desktop 占位，不应阻碍 Native 首次启动。
- `ylcs-app/desktop-app/src/desktopMain/kotlin/Main.kt` 直接使用 `java.awt.Desktop`、`System::gc`、JVM 入口逻辑。新入口必须剥离 JVM 特有行为。

### 6.6 导致 App 全链受阻的根节点

```text
desktop-app [JVM 当前入口]
  └─ app/portal
      ├─ app/global
      │   ├─ Haze / Haze Blur
      │   ├─ compose/app / screen / ui / cache / icons
      │   ├─ components/lottie / rich-text / url-image
      │   ├─ MMKV / Picker / 配置 / 异常启动
      │   └─ Network / Cryptography / CS Client Engine
      ├─ account / activity / community / information / photo / world
      ├─ music / radio / game-rhyme
      │   └─ components/media / drag-drop / reordering
      ├─ viewer
      │   └─ webview / media
      └─ 其他 App 功能模块
```

由此可见：**屏幕数量不是首要工作量指标**。最关键的阻碍在 `Compose/Skiko` 基础、`compose/app` 宿主、`Sketch` 图片链、`Haze`、`MMKV`、`media`、`Navigation3` 等根依赖。

---

## 7. 建议的实施路线与验收关口

### 阶段 A：验证技术组合和最小 Native UI（MVP 基础）

**任务**

- 固定 Kotlin/CMP/Skiko/SDL 版本与 Git Commit；确认两参考仓库 API、ABI、许可可复用性。
- 检查 official/Fork Maven Metadata，列出真正缺失 `linuxX64`、`mingwX64` 的具体坐标与传递闭包。
- 建立隔离的 `native-compose/` Gradle 项目；只获取所需上游源码。
- 成功构建 `mingwX64` 与 `linuxX64` 的必要 Compose UI 工件及 SDL3/Skiko Host。
- 运行一个包含 Text、TextField、Button、Canvas、动画、滚轮与输入法的最小原生 Demo。
- Windows 验证 D3D12 渲染及 OpenGL/CPU 回退；Linux 验证 OpenGL/CPU 回退。

**验收**：两平台至少完成真实原生窗口展示、基础绘制、CJK 文本显示与输入、鼠标点击/滚轮、关闭窗口；保存构建命令、运行日志与平台截图；明确尚缺功能。

### 阶段 B：Bridge Plugin + 真实外部消费者 + Gallery

**任务**

- 发布内部 Maven/KLIB 快照，包括 Root Gradle Metadata 和实际 Native Target Publication。
- 实现 YLCS Gradle Bridge：用户只在 `commonMain` 声明官方 Compose 坐标；Native 配置解析为 YLCS，其他平台继续保持官方。
- 独立空仓库消费者测试，不允许只在本仓库以 `project(...)` 通过。
- 使现有 `ylcs-app/gallery` 新增 Native Desktop 目标并实际启动。
- 确认公共资源、字体/图片、多窗口基础能力与常规 Rachel UI 组件。

**验收**：外部示例工程的普通 Compose 源码不出现显式 Native Fork 坐标；`linuxX64`/`mingwX64` 真正链接并运行；Gallery 关键组件有可视化证据。

### 阶段 C：Rachel 应用宿主、导航与基础系统能力

**任务**

- 完成 `compose/app` Native PlatformApplication、WindowController、窗口装饰/拖动、置顶、最小化、Tray、剪贴板、URI 打开等。
- 将 Navigation3 UI、Lifecycle/ViewModel、SavedState、NavigationEvent 的 Native 依赖全部打通。
- 验证 `app:portal` 能编译、打开首页并切换基础 Screen；允许尚未实现的媒体/WebView 功能有清晰的禁用/占位提示。

**验收**：可进入主要应用界面，不发生 Host 生命周期和导航崩溃；窗口设置、状态恢复及应用退出流程正常。

### 阶段 D：Sketch、ZoomImage、Haze 和高级图形

**任务**

- 先处理静态图像解码、远程 Fetch、缓存，再逐步实现 GIF/WebP、ZoomImage。
- 分别验证 Haze/Blur 在 OpenGL、D3D12、CPU 回退模式下的行为；对 Native GraphicsLayer 特性编写对照测试。
- 完成 Skottie/Lottie Windows 相关可用工件与 Native 接口验证。

**验收**：图片、缩放和关键特效可用；视觉基准和内存表现有可重现测试。

### 阶段 E：Native 多媒体与平台服务

**任务**

- 将 MMKV、Picker、Miniaudio/Windows 视频现有 JNI Bridge 替换为 C ABI/cinterop。
- 按优先级实现 Linux 原生视频播放，避免把当前占位行为当作已实现。
- 单实例、自动更新、系统集成、动态库部署及文件操作补齐 Native。
- 验证音视频线程、异常处理、GPU 帧数据路径和退出资源释放。

**验收**：核心音乐/音频功能、持久化、文件打开与基本发行流程通过真实设备测试；记录视频平台差异。

### 阶段 F：持续发行、升级和质量门禁

- 构建矩阵覆盖 Windows x64 与 Linux x64；未来按需加 Linux arm64/macOS。
- 对 Native 与既有 Desktop JVM 关键界面进行截图/行为对比。
- Vendor 清单、上游 Commit、手动补丁有漂移审计和 License 检查。
- ABI/API 快照，消除版本不匹配和重复类风险。
- Maven 发布同时验证根 Metadata、每目标 KLIB、传递依赖以及真实消费者解析。
- 支持 Native exe+资源+SDL/Skiko 原生库的一体化打包；Windows 检查 DLL/ICU，Linux 检查系统动态库要求。
- 每次更新上游 CMP/Kotlin 时先在 CI 中验证，保持“最新可兼容 Native 组合”的明确版本表。

---

## 8. 工程风险与必须注意的边界

| 风险 | 必须采取的措施 |
|---|---|
| 上游 Common Metadata 不等于 Native 平台实现 | 对缺失 Target 编译真实公共源码与 Native actual，不幻想插件能补字节码 |
| 官方 Runtime 与 Fork UI 版本/ABI 不一致 | 版本锁定 + `linuxX64`/`mingwX64` 编译链接验收 |
| KMP 依赖替换在 IDE、Metadata 或传递闭包失效 | 在多模块项目和独立消费者中验证，必要时细化 KGP 模型/解析处理 |
| Maven 根元数据未列齐目标变体 | 多 Host 发布后核对 `.module`，不可只看各平台上传成功 |
| Skiko Windows D3D12 ABI、原生库或驱动问题 | 使用实际 Windows 主机验证 D3D12 + 回退 + 设备丢失；核对 DLL/ICU |
| Native Compose 的高级绘制未达到 JVM 行为 | 视觉对比/Probe；先列出 RTL、字体、TextField、IME、Shader 等覆盖表 |
| Haze 与 GraphicsLayer 对渲染后端的依赖 | 真实 GPU 和 CPU 模式均验证，不以编译通过代替功能完成 |
| JNI 与 Kotlin/Native C Interop 不是同一机制 | Native Bridge 通过 C ABI、平台线程与资源生命周期重做 |
| SDL Host 与图形后端耦合 | SDL3 只负责窗口/事件，Renderer 可独立选 D3D/OpenGL/CPU |
| 开发阶段 Vendor 大量上游源码产生维护成本 | 固定 Ref、只同步必须文件、Git 忽略原样副本、补丁按模块管理 |
| Fork 版权与许可证 | 保留上游 Copyright/License，发布时遵守第三方许可，不把 Vendor 当成自有源码 |
| 示例 Demo 成功≠完整 App 可用 | 使用阶段式验收、Gallery 与 Portal 两级真实工程验证 |
| Kotlin 2.5 Beta / CMP 1.13 Alpha 快速变化 | 当前版本仅是被审计快照，新任务开始必须重新审计 |

---

## 9. 尚未最终确定的设计决策

以下各项不能因为本文记录了“建议”就视为用户已正式批准：

1. **代码基础**：直接继承 brahmkshatriya 部分 Compose/Skiko 实现、选择性移植其实现，还是以 bitsycore 为母架构再引入更完善的文字/图形实现？实际结果取决于版本兼容、源码可隔离性及许可。
2. **Native Compose 是否必须与当前 CMP `1.13.0-alpha02` 完全一致**，或允许优先固定已经验证的上游版本作为首个 PoC？原则上尊重“紧跟最新版”，但工程质量优先需要可复现版本。
3. **独立 Gradle Build 的接入方式**：`native-compose/` 独立构建 + 发布到 Maven，或主仓库用 `includeBuild` 支持联调，取决于调试体验与发布需求。
4. **公开坐标/插件 ID**：需要正式设计，当前只是示意。
5. **版本策略**：上游版本与 YLCS Native Port 版本如何配对、是否增加兼容矩阵/平台限定。
6. **Skiko Windows 来源**：优先采用 brahmkshatriya Fork、同步源码自建，还是在已有实现之上演进；需编译确认。
7. **Material3**：Gallery/Portal 是否真正通过某个传递依赖需要它，不能凭印象判断。
8. **Sketch 方案**：Fork Sketch 增加 Native Target，或先抽象 YLCS Image 接口，阶段性使用简化的 Native Skia Image 加载器。
9. **完整 Linux 视频和 WebView**：是首版功能目标还是以当前 Desktop JVM 已有行为为基准。
10. **SDL3 静态/动态链接、第三方库分发方式及许可证**：应结合目标发行渠道决定。

---

## 10. 新对话必须输出的交付成果

若后续开启“正式研发”任务，建议要求 AI 至少交付以下**可检查的工作产品**，不只是概念解释：

1. **更新版依赖支持矩阵**：列出 `ylcs-app/desktop-app` 或先从 Gallery 出发的实际依赖闭包，逐模块标注 Maven 坐标、版本、`linuxX64`、`mingwX64` 是否有工件、是否已有真实构建验证；附 GitHub/Maven Metadata 来源。
2. **最小可运行 PoC**：在独立 `native-compose/`（或确定的实际目录）产出真实的 Windows/Linux Native 可执行文件和测试命令，明确源代码位置。
3. **源码同步工具**：上游 Ref、模块文件清单、Vendor 生成路径、忽略规则、漂移验证、许可记录。
4. **Native Host / Renderer 分层说明和实现**：SDL3 与 Skiko 的边界、Windows HWND + D3D12、Linux GL、回退路径。
5. **Bridge Gradle 插件**：配置 Native 目标解析、正确替换官方依赖并明确保留官方 Runtime，附发布工件和独立消费者用法。
6. **可复现测试与报告**：每平台编译/链接/运行是否成功，CJK/IME/HiDPI/输入/窗口/资源的测试证据；失败项与下一步处理顺序。
7. **渐进迁移表**：哪些 YLCS 模块只需开启 Target，哪些需新写 actual，哪些 JNI/Java/AWT API 不可用，哪些可暂时跳过。
8. **持续维护机制**：升级脚本、版本兼容表、GitHub Actions、发布元数据完整性检查。

**禁止的交付方式**：把没有真实运行的伪代码说成完整实现；宣称 Gradle Metadata 能自动补全没有 Native 实现的官方 UI；不加区分地 Fork 所有 Compose/Koin/Coil；未验证版本混用；静态 README 支持声明替代 Native Windows/Linux 真机测试。

---

## 11. 首批检查的源码和文档入口

### 当前 YLCS 项目

- [仓库总览](https://github.com/rachel-ylcs/ylcs-kmp)
- [`settings.gradle.kts`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/settings.gradle.kts)
- [`libs.versions.toml`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/libs.versions.toml)
- [`buildSrc/.../KotlinMultiplatformTemplate.kt`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/buildSrc/src/main/kotlin/KotlinMultiplatformTemplate.kt)
- [`ylcs-app/desktop-app/build.gradle.kts`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/desktop-app/build.gradle.kts)
- [`ylcs-app/app/portal/build.gradle.kts`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/app/portal/build.gradle.kts)
- [`ylcs-app/gallery/build.gradle.kts`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/gallery/build.gradle.kts)
- [`ylcs-module/compose/app/.../PlatformApplication.desktop.kt`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/compose/app/src/desktopMain/kotlin/love/yinlin/compose/PlatformApplication.desktop.kt)
- [`ylcs-module/compose/core/build.gradle.kts`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/compose/core/build.gradle.kts)
- [`ylcs-module/compose/components/url-image/build.gradle.kts`](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/compose/components/url-image/build.gradle.kts)
- [`ylcs-module/compose/components/media`](https://github.com/rachel-ylcs/ylcs-kmp/tree/main/ylcs-module/compose/components/media)
- [`ylcs-module/compose/startup/mmkv-kmp`](https://github.com/rachel-ylcs/ylcs-kmp/tree/main/ylcs-module/compose/startup/mmkv-kmp)

### 社区对照资料

- [bitsycore Compose Native SDL3](https://github.com/bitsycore/compose-desktop-native)
- [bitsycore Vendor 同步与清单](https://github.com/bitsycore/compose-desktop-native/blob/main/scripts/compose-fork/README.md)
- [bitsycore Bridge Gradle 插件](https://github.com/bitsycore/compose-desktop-native/blob/main/gradle-plugin/compose-desktop-native-bridge/README.md)
- [bitsycore ROAD TO 1.0.0](https://github.com/bitsycore/compose-desktop-native/blob/main/PLAN.md)
- [bitsycore Native GPU 模式](https://github.com/bitsycore/compose-desktop-native/blob/main/compose/ui/ui/src/nativeMain/kotlin/com/compose/sdl/GpuMode.kt)
- [brahmkshatriya Compose Native](https://github.com/brahmkshatriya/compose-native)
- [brahmkshatriya Native Gradle 插件](https://github.com/brahmkshatriya/compose-native/blob/main/gradle-plugin/README.md)
- [brahmkshatriya 发布约定](https://github.com/brahmkshatriya/compose-native/blob/main/PUBLISHING.md)
- [brahmkshatriya Linux Demo](https://github.com/brahmkshatriya/compose-native/blob/main/demo/README.md)
- [brahmkshatriya Windows SDL3 / HWND 附着](https://github.com/brahmkshatriya/compose-native/blob/main/compose/desktop/desktop-native/src/mingwX64Main/kotlin/androidx/compose/ui/window/NativeDesktopIntegration.windows.kt)
- [brahmkshatriya Skiko Native（D3D12）](https://github.com/brahmkshatriya/skiko-native/blob/master/README.md)
- [官方 Skiko](https://github.com/JetBrains/skiko)
- [官方 Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [官方 Compose Multiplatform Core](https://github.com/JetBrains/compose-multiplatform-core)
- [Sketch](https://github.com/panpf/sketch)
- [Haze](https://github.com/chrisbanes/haze)

---

## 12. 当前最终共识（一页概览）

> **目标不是维护第二个完整 Compose 仓库，而是为官方 CMP 缺失的 Windows/Linux Native 部分建立小而可持续的扩展发行版，并让用户尽量保持官方 API 与依赖使用习惯。**
>
> - 官方有 Native 变体就用官方工件；没有就复用官方源码，在隔离构建中按需 Vendor + 补实际 Native 实现。
> - `src/vendor/` 不提交 Git，但构建时的确会复制并编译这些源码。
> - 消费者通过 YLCS Gradle Bridge 在 `commonMain` 声明官方 Compose 坐标，Native 目标由插件选择 YLCS 工件，普通用户不用手工维护双重依赖。
> - 实现参考 **brahmkshatriya 的完整 Native UI/Skiko/D3D12 路线**，源码管理和发行体验参考 **bitsycore 的轻量 Vendor/Bridge/测试体系**。
> - SDL3 负责跨平台窗口/输入，Windows GPU 默认仍可直接采用 D3D12，Linux 则采用 OpenGL。
> - 迁移成本不只 Compose/Skiko，还包括 Sketch、Haze、Navigation3 UI、Skottie、Rachel 窗口宿主及 JNI 平台服务；但已有 Foundation/Ktor/Native FFI 可以大量复用。
> - **实施从 Native Hello/Gallery 开始，以真实 Windows/Linux 编译和运行验收为准，再扩展到完整 Desktop App。**

