# Rachel 多平台开发框架

![Rachel](assets/title.png)

Rachel 是基于 Kotlin Multiplatform 与 Compose Multiplatform 的模块化开发框架。仓库提供公共基础库、跨平台界面体系、应用启动与导航能力、客户端与原生服务端通信引擎，以及业务应用和独立工具示例。

本文档依据当前仓库源码编写。依赖、自有构建插件、框架与应用版本以 `libs.versions.toml` 为准，其他平台与应用常量以 根目录 `settings.gradle.kts` 的 `repository {}` 为准。各模块的目标平台、依赖导出范围和构建任务以对应 `build.gradle.kts` 及构建模板为准。配置详情见[编译环境与工程结构](rachel/1_start/environment.md)。

## 模块体系

| 模块路径 | 职责 |
| --- | --- |
| `ylcs-module/core` | 平台识别、协程、序列化、集合、编码及字节操作 |
| `ylcs-module/foundation/*` | 平台上下文、文件系统、网络、启动编排、压缩、加密及解析器 |
| `ylcs-module/compose/core` | Compose 状态扩展、图形辅助、窗口状态和平台上下文 Local |
| `ylcs-module/compose/theme`、`ui` | 主题、布局、输入、文本、图像及浮层组件 |
| `ylcs-module/compose/screen` | 页面模型、导航策略、宿主作用域和页面资源管理 |
| `ylcs-module/compose/platform-view` | Android View、Swing、UIKit 和 DOM 的宿主适配 |
| `ylcs-module/compose/components/*`、`startup/*`、`cache` | 可选组件、应用服务与缓存能力 |
| `ylcs-module/cs/*` | 共享接口契约、客户端引擎、原生服务端引擎及数据库服务 |
| `ylcs-module/platform/*`、`native/*` | FFI、动态库加载、操作系统集成及 Win32 封装 |
| `ylcs-app/*` | 业务应用、组件展厅、导航示例、服务端与 MOD 工具 |
| `build-logic` | 插件安装、源集组织、代码生成、原生构建、打包与发布 DSL |
| `native-compose` | Windows、Linux、macOS 的 Compose Native 上游锁、适配、桥接与消费验证 |

基础库与业务应用保持单向依赖。共享协议集中声明接口和传输模型，具体网络请求、数据库访问与界面交互分别由相应模块实现。

## 平台范围

构建模板支持 Android、iOS、JVM Desktop、JavaScript、WasmJS，以及 Windows、Linux、macOS 的 Kotlin/Native 目标。平台能力由模块按需启用，不能依据仓库整体的平台范围推断单个组件的可用性。

Android 当前仅打包 `arm64-v8a`。iOS 启用 `iosArm64`，并在 Apple Silicon macOS 主机上配置 `iosSimulatorArm64`；未启用 `iosX64`。桌面原生目标分别为 `mingwX64`、`linuxX64` 和 `macosArm64`。服务端采用 Kotlin/Native 与 Ktor CIO。

## 阅读路径

| 开发目标 | 文档入口 |
| --- | --- |
| 配置环境并建立应用 | [编译环境](rachel/1_start/environment.md)、[快速开始](rachel/1_start/entry.md)、[应用入口](rachel/2_ui/entry.md) |
| 编写跨平台界面 | [主题](rachel/2_ui/theme.md)、[设备模型](rachel/2_ui/device.md)、[组件](rachel/2_ui/component.md)、[浮层](rachel/2_ui/floating.md) |
| 管理页面与状态 | [导航与页面](rachel/2_ui/navigation.md)、[应用服务](rachel/2_ui/service.md) |
| 接入共享后端 | [C/S 共享契约](rachel/1_start/cs_core.md)、[客户端引擎](rachel/3_client/entry.md)、[服务端引擎](rachel/4_server/entry.md) |
| 扩展公共能力 | [核心库](rachel/1_start/core.md)、[Compose 核心](rachel/1_start/compose_core.md)、[平台组件](rachel/2_ui/platform_component.md) |
| 使用解析器、压缩、摘要或检索 | [基础能力模块](rachel/1_start/foundation.md) |
| 新增模块、配置目标与分发 | [构建插件与模块配置](rachel/1_start/build.md) |
| 维护媒体工具 | [MOD 文件格式](tool/mod.md)、[Android FFmpeg 解码库](tool/ffmpeg.md) |

文档中的业务示例用于说明框架调用方式；未展开的业务类型、数据仓库和校验函数需要由使用方实现。平台专属示例应放置在对应源集中。

## 文档位置与维护边界

| 位置 | 内容与维护方式 |
| --- | --- |
| 根目录 `README.md`、`README_en.md` | 项目介绍、预览与全模块平台支持矩阵 |
| 根目录 `AGENTS.md` | 自动化代码协作的仓库约定，不是面向使用方的 API 说明 |
| `native-compose/README.md` | 桌面 Native Compose 的消费、窗口、分发与移植维护 |
| `ylcs-docs/docs/index.md` | 框架与指南索引 |
| `ylcs-docs/docs/rachel/1_start/*.md` | 环境、构建、核心库、共享契约与基础能力 |
| `ylcs-docs/docs/rachel/2_ui/*.md` | 入口、上下文、主题、设备、导航、服务、网络、组件与浮层 |
| `ylcs-docs/docs/rachel/3_client/*.md` | 客户端请求、资源、上传与 WebSocket |
| `ylcs-docs/docs/rachel/4_server/*.md` | 原生服务端、接口、资源、上传、Socket、回调、数据库与插件 |
| `ylcs-docs/docs/tool/*.md` | MOD 协议、MOD Manager 与 Android FFmpeg 适配 |
| `ylcs-docs/docs/overrides/` | 站点模板及发布流程生成的 Gallery、ModernScreen、Dokka 页面；生成结果从相应源码维护 |

模块平台矩阵来自各模块构建脚本及模板的默认目标，源码中的占位或空实现另外注明。完整矩阵见[仓库首页](https://github.com/rachel-ylcs/ylcs-kmp#全平台模块支持)。指南中的源码链接指向当前 `main`；阅读历史版本时应同时切换代码与文档版本。

## 状态与资源归属

应用级服务由 `StartupPool` 管理。页面继承 `ScreenModel`，保留 ViewModel、`viewModelScope` 与清理语义；`BasicScreen` 和 `Screen` 在其上提供浮层及标准页面布局。导航条目保存类型、初始参数和实例标识，运行时页面由所属 `ScreenManager` 的 `ScreenStore` 管理。

当前导航通过宿主的 `ViewModelStoreOwner` 保留管理器，仅将管理器标识放入 `rememberSaveable`。该实现不等同于完整导航栈的进程死亡恢复。可持久化内容应限制为标识、可序列化参数和小型界面状态；平台句柄、回调、文件流与 ViewModel 应由运行时所有者管理。

原生视图、上传流、数据库连接与媒体播放器均具有独立的资源生命周期。使用相应能力前，应确认创建、复用和释放责任，避免将临时资源保存到更长的生命周期中。

## 示例工程

| 路径 | 内容 |
| --- | --- |
| `ylcs-app/gallery` | UI 组件、主题、布局和浮层演示 |
| `ylcs-app/modern-screen` | 导航策略、参数、工厂、嵌套导航和页面状态演示 |
| `ylcs-app/app/portal`、`app/global` | 业务应用入口、全局服务与导航注册 |
| `ylcs-app/server` | 原生服务端、接口、数据库、Redis 和 WebSocket 组合 |
| `ylcs-app/mod-manager` | MOD 预览、解包、打包和部署工具 |

组件展厅、导航示例和模块 API 页面由文档发布流程生成。本文档描述手写指南；生成页面与源码应使用相同版本。
