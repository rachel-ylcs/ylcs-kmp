# Rachel 多平台框架

![Rachel](assets/title.png)

Rachel 是银临茶舍项目沉淀出的 Kotlin Multiplatform / Compose Multiplatform 工程体系。它不只是一个 UI 组件库：仓库同时提供跨平台基础能力、应用启动编排、导航与浮层、客户端/原生服务端通信协议，以及一组可按需组合的业务组件。

本文档以 `main` 分支的 `15d3c8ba26ffa1ecbf882384d3bacaf46d96ac81` 为基线重写。示例优先解释当前代码为何这样设计，再给出可以落地的用法；若文档与后续代码出现差异，应以同一提交附近的源码与类型签名为准。

## 项目分层

| 层级 | 主要模块 | 职责 |
| --- | --- | --- |
| 基础层 | `ylcs-module/core`、`foundation/*` | 协程、JSON、URI、文件系统、网络与启动模型 |
| Compose 层 | `compose/core`、`theme`、`ui`、`screen`、`platform-view` | 状态辅助、主题、组件、导航、平台原生视图 |
| 可选能力 | `compose/components/*`、`compose/startup/*` | 媒体、富文本、二维码、图片、选择器、配置、缓存等 |
| C/S 契约 | `cs/core`、`client-engine`、`server-engine` | 一份声明同时约束客户端请求与服务端响应 |
| 平台支撑 | `platform/*`、`native/*` | 操作系统识别、FFI、动态库加载与 Win32 能力 |
| 应用样例 | `ylcs-app/*`、`ylcs-app/app/*` | Android、Desktop、iOS、Web、原生服务端和真实业务 |

核心依赖方向保持单向：基础层不知道 Compose；UI 可以依赖基础层；C/S 契约可被客户端和服务端共同引用；具体应用在最上层组合这些能力。这个边界让共享代码足够多，又不会强迫每个目标平台携带用不到的实现。

## 支持的平台

当前构建模板覆盖下列目标。并非每个叶子模块都发布到所有目标，实际可用目标以该模块的 `build.gradle.kts` 为准。

| 平台族 | 当前目标 |
| --- | --- |
| 手机 | Android、iOS arm64 |
| 桌面 | JVM Desktop、Windows Native、Linux Native、macOS Native |
| Web | JavaScript、WasmJS |
| 其他 Native | Android Native arm64 |

当前基线的 iOS 模拟器目标处于关闭状态；Android 只打包 `arm64-v8a`；原生服务端模块面向 Windows、Linux 与 macOS，而不是 JVM Ktor 服务端。

## 推荐阅读顺序

第一次接触项目时，按目标选择最短路径：

1. 构建应用：先读[环境与工程结构](rachel/1_start/environment.md)、[快速开始](rachel/1_start/entry.md)和[应用入口](rachel/2_ui/entry.md)。
2. 编写界面：继续阅读[主题](rachel/2_ui/theme.md)、[导航](rachel/2_ui/navigation.md)、[组件](rachel/2_ui/component.md)和[浮层](rachel/2_ui/floating.md)。
3. 接入后端：先理解[共享 C/S 契约](rachel/1_start/cs_core.md)，再分别阅读[客户端](rachel/3_client/entry.md)与[服务端](rachel/4_server/entry.md)。
4. 扩展底层能力：阅读[核心库](rachel/1_start/core.md)、[Compose 核心](rachel/1_start/compose_core.md)和[平台组件](rachel/2_ui/platform_component.md)。

## 贯穿项目的几个设计点

### 依赖图，而不是手写初始化顺序

启动服务用显式依赖描述拓扑关系。同步服务按序创建；异步服务可以并发启动，但会等待自己的依赖。销毁时按相反顺序执行。这比在各平台入口复制一长串初始化代码更容易验证，也让测试替换服务成为可能。

### 路由也是可恢复的数据

页面路由将页面键、实例 ID 与序列化参数编码到一个字符串中。导航栈因此可以保存和恢复，同一页面类型也能拥有多个实例；`New`、`Replace`、`Move`、`Resume` 等策略只是在这个模型上定义不同的栈操作。

### 浮层返回值遵循结构化并发

对话框不是“打开后注册回调”，而是通过挂起函数等待结果。调用协程取消时，等待也随之结束；浮层离开组合时会清理未完成结果。这使确认、输入和选择流程可以按顺序写成普通控制流。

### 平台视图先决定状态所有权

嵌入 Android View、Swing、UIKit 或 DOM 时，框架刻意区分“宿主内部持有状态”和“Compose 外部控制状态”。例如 WebView 的当前 URL 通常属于原生实例内部状态，而设置项可以由 Compose 提升并在更新阶段写入。先确定所有权，才能避免重组导致页面被反复重置。

### C/S 使用共享类型，而不是重复 DTO

API 的路径、输入数量、输出数量和上传形式都由共享声明推导。客户端负责把参数编码为 JSON 数组或 multipart，服务端用同一声明解码并生成响应。代价是协议有明确约束；收益是改动会尽早表现为编译错误。

## 示例与源码地图

- `ylcs-app/gallery`：组件、主题、浮层和布局的可运行展厅。
- `ylcs-app/app/portal`：完整应用壳、导航注册与多平台入口。
- `ylcs-app/server`：原生 Ktor 服务端、数据库、Redis、API 与 WebSocket 的真实组合。
- `ylcs-app/mod-manager`：桌面文件处理、拖放与 MOD 工具链。
- `ylcs-module/*`：可复用框架实现，也是确认行为边界的最终依据。

## 使用边界

Rachel 当前首先服务于本仓库，并非具有严格兼容承诺的公共产品。部分 API 仍会随应用演进而调整；若要在仓库外使用，建议锁定版本、只引入确实需要的模块，并为路由编码、API 线格式和文件格式建立自己的兼容测试。
