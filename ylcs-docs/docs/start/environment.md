---
description: 配置 Rachel 仓库所需的 JDK、Gradle Wrapper、平台 SDK 与宿主工具链。
---

# 开发环境

<p class="rc-lead">Rachel 使用仓库根目录的 Gradle Wrapper 和统一的构建模板。环境配置的目标不是“装上所有东西”，而是让当前宿主的工具链与目标平台保持一致，并能通过构建脚本自身完成校验。</p>

## 版本基线从哪里读取

文档不重复记录具体版本号，避免文档与源码脱节。以下位置是唯一可信来源：

| 信息 | 权威位置 |
| --- | --- |
| Gradle 发行版 | `gradle/wrapper/gradle-wrapper.properties` |
| Kotlin、Compose、Ktor 与依赖版本 | 根目录 `libs.versions.toml` |
| JDK、Android SDK、iOS 目标、桌面 Native 与发布参数 | 根目录 `settings.gradle.kts` 的 `repository {}` |
| 模块启用的平台 | 各模块 `build.gradle.kts` 与构建模板 |

!!! warning "不要用版本目录推断运行环境"
    依赖声明成功编译，不代表目标平台的 SDK、Xcode、NDK 或本机动态库已经就绪。以实际任务结果为准。

## 必要工具

| 工具 | 用途 | 说明 |
| --- | --- | --- |
| JDK | 运行 Gradle 与编译 JVM 目标 | 命令行与 IDE 的 Gradle JVM 必须指向同一主版本 |
| Gradle Wrapper | 统一构建入口 | 始终使用仓库自带的 `gradlew` / `gradlew.bat` |
| Android SDK | Android 目标 | 需要与构建脚本配置的 SDK、NDK 和 ABI 匹配 |
| Xcode | iOS 目标 | 仅在 macOS 主机可用，需完成命令行工具授权 |
| CMake、Ninja、C/C++ 工具链 | 原生库与桌面 Native | 需要出现在 Gradle 进程的 `PATH` 中 |
| Git 子模块 | 第三方原生依赖 | 按 `.gitmodules` 初始化所需子模块 |

首次检出仓库时，建议先确认 Wrapper 可用：

```bash title="检查构建入口"
./gradlew -version
```

Windows PowerShell 使用：

```powershell title="检查构建入口"
.\gradlew.bat -version
```

输出中的 JVM 应与 IDE 设置一致。若 IDE 使用另一套 JDK，命令行与 IDE 会出现“同一条命令结果不同”的假象。

## 仓库结构

```text title="仓库地图"
ylcs-kmp/
├─ build-logic/          # 构建插件、模板、原生桥与仓库约定
├─ ylcs-module/          # 公共框架：core、foundation、compose、cs、platform
├─ ylcs-app/             # 业务应用、示例、服务端与工具
├─ native-compose/       # 桌面 Native Compose 移植、适配与验证
├─ ylcs-docs/            # 本指南与生成页面入口
└─ settings.gradle.kts   # 仓库身份、全局常量与模块注册
```

模块注册、依赖方向与目录职责见[模块全景](../module/index.md)。本章只需要记住：**框架能力在 `ylcs-module`，业务与示例在 `ylcs-app`，构建规则在 `build-logic`**。

## 宿主与目标限制

Kotlin Multiplatform 允许声明多个目标，但每个目标仍受宿主限制：

- iOS 编译、链接与归档需要 macOS 与 Xcode。
- Windows 原生库通常需要 MSVC、Windows SDK 与对应 C++/WinRT 组件。
- Linux 桌面 Release 的 AOT 训练会实际启动应用，无显示服务的 CI 需要虚拟 X11。
- 预编译动态库存在，不等于对应原生源码已经重新构建。

!!! note "源集不是目录约定"
    仓库关闭了 Kotlin 默认层级模板，显式建立源集依赖。目录名相同不代表继承关系相同；以 `sources { ... }` 声明为准。详见[源集与 API](../module/sourcesets.md)。

## 环境校验与常见问题

### Gradle 能启动，但任务找不到

先确认任务确实存在于当前模块：

```bash title="查询模块任务"
./gradlew :ylcs-app:gallery:tasks --all
```

任务是否可用取决于模块目标、宿主平台与插件配置。不要假设仓库中所有模块都在当前宿主启用了全部目标。

### 原生任务失败

按以下顺序排查：

1. `local.properties` 中的镜像与本地路径是否正确；
2. 所需 Git 子模块是否初始化；
3. CMake、Ninja 与编译器是否对 Gradle 进程可见；
4. 目标平台 SDK 与构建脚本配置是否匹配；
5. 失败发生在 CInterop、C++ 编译、Kotlin 链接还是打包阶段。

### IDE 与命令行结果不一致

检查 IDE 的 Gradle JVM、Gradle 分发方式与工作目录。项目应使用 Wrapper 分发，而不是本机安装的 Gradle。

## 文档与生成页面

本指南由 MkDocs 构建。组件展厅、导航示例与 API 页面属于生成内容，分别由 Gallery、ModernScreen 与 Dokka 任务产出。手写指南不负责生成这些页面，但需要在导航与页脚中保留入口。

```bash title="本地预览指南"
cd ylcs-docs
mkdocs serve
```

构建站点前请确认 `docs/overrides` 目录存在；CI 会在生成示例页面前创建它。指南构建成功不代表示例页面已经生成。

## 下一步

环境可用后，进入[模块与依赖](modules.md)声明你的第一个应用模块。
