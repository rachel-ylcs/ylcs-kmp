![logo](ylcs-docs/docs/assets/title.png)

<div>
    <h5></h5>
    <div>
        <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android" alt=""/>
        <img src="https://img.shields.io/badge/Platform-iOS%20%2F%20macOS-lightgrey.svg?logo=apple" alt=""/>
        <img src="https://img.shields.io/badge/Platform-Windows-blue.svg?logo=esotericsoftware" alt=""/>
        <img src="https://img.shields.io/badge/Platform-Linux-red.svg?logo=linux" alt=""/>
        <img src="https://img.shields.io/badge/Platform-WASM%20%2F%20JS-yellow.svg?logo=javascript" alt=""/>
        <img src="https://img.shields.io/badge/Platform-Server-orange.svg?logo=openjdk" alt=""/>
    </div>
    <h5></h5>
    <div>
        <img src="https://img.shields.io/github/v/tag/rachel-ylcs/ylcs-kmp" alt=""/>
        <img src="https://img.shields.io/github/v/release/rachel-ylcs/ylcs-kmp" alt=""/>
        <img src="https://github.com/rachel-ylcs/ylcs-kmp/actions/workflows/build.yml/badge.svg" alt=""/>
        <img src="https://github.com/rachel-ylcs/ylcs-kmp/actions/workflows/main.yml/badge.svg" alt=""/>
    </div>
    <h5></h5>
</div>

### 语言 (Language) :
**中文** | [English](README_en.md)

# Rachel快速开发框架

`Rachel`是基于`Kotlin Multiplatform`与`Compose Multiplatform`集成逻辑、服务、UI的跨平台快速开发框架，由银临茶舍项目组完成开发。

涵盖跨平台入口、共享UI、依赖服务启动、客户端和服务端交互引擎、自由屏幕导航、平台原生组件、自定义主题、响应式布局、MDI配置一致性、分页加载、服务端路由等领域。

### 我们支持原生 Compose 桌面程序 !!!

|Target|  VM  |  Native  |  Native Compose  |
|:---:|:---:|:---:|:---:|
|  Android  |✅|➖|✅|
|  iOS  |➖|✅|✅|
|  Windows  |✅|✅|✅|
|  Linux  |✅|✅|✅|
|  macOS  |✅|✅|✅|
|  Web  |✅|✅|✅|
|Server|✅|✅|➖|

- **VM**: `JVM` or `JavaScript`
- **Native**: `Kotlin Native` or `Web Assembly`
- **Native Compose**: **The bridge plugin makes native compose desktop possible!**

## 特性

- **跨平台**：基于`KMP` / `CMP`，支持全平台客户端与服务端，具有多端一致性。
- **原生性能**：生成平台原生二进制文件，无桥接或附加库。
- **单一语言**：仅需掌握`kotlin`语言即可完成大多数任务需求与开发。
- **快速**：三分钟便能上手构建自己的跨平台应用程序。
- **简洁**：大量类型安全与结构化的框架DSL，无需编写过多样板代码。
- **模块化**：全框架模块化，模块间依赖清晰，应用程序按需引入模块。
- **高协作**：框架支持前后端协作开发，共享数据组织结构。

# 文档
因为Rachel框架模块较多，功能丰富，一个最简单的应用程序可以仅导入app模块（注意版本号）：
```kotlin
implementation("love.yinlin.compose:app:x.x.x")
```

## 入门指引文档: [Rachel框架开发向导](https://rachel-ylcs.github.io/ylcs-kmp)

## API文档: [模块API合集](https://rachel-ylcs.github.io/ylcs-kmp/overrides/dokka)

# Rachel Compose UI 界面库

界面库涵盖主题、文本、输入、图片、动画、容器、集合、导航、浮窗等多种组件，支持暗色模式，界面库独立，无 `compose.material` 依赖。

## UI 在线预览: [Rachel UI Gallery](https://rachel-ylcs.github.io/ylcs-kmp/overrides/gallery)

## 现代导航预览：[Modern Navigation](https://rachel-ylcs.github.io/ylcs-kmp/overrides/modern-screen)

|                       Theme                        |                     Dark Mode                      |
|:--------------------------------------------------:|:--------------------------------------------------:|
|   ![rachel](ylcs-docs/docs/assets/ui/gallery.png)    |    ![rachel](ylcs-docs/docs/assets/ui/theme.png)    |
|                        Text                        |                      RichText                      |
|   ![rachel](ylcs-docs/docs/assets/ui/text.png)    |   ![rachel](ylcs-docs/docs/assets/ui/richtext.png)    |
|                       Input                       |                       Animation                        |
|   ![rachel](ylcs-docs/docs/assets/ui/input.png)   |   ![rachel](ylcs-docs/docs/assets/ui/animation.png)   |
|                     Navigation                      |                   Dialog                   |
| ![rachel](ylcs-docs/docs/assets/ui/tab.png) | ![rachel](ylcs-docs/docs/assets/ui/dialog.png) |
|                     Image                     |                      Container                      |
| ![rachel](ylcs-docs/docs/assets/ui/image.png) |  ![rachel](ylcs-docs/docs/assets/ui/container.png)  |
|                     Collection                      |                     Calendar                     |
| ![rachel](ylcs-docs/docs/assets/ui/collection.png)  | ![rachel](ylcs-docs/docs/assets/ui/calendar.png) |


# 全平台模块支持

按当前目录结构列出 `ylcs-module` 中的全部 53 个模块：核心与基础能力 18 个、Compose 24 个、客户端与服务端 5 个、平台集成 4 个、构建工具 2 个。点击模块名进入源码目录；每个条目先标注支持平台，再说明用途和平台差异。

- **全平台**：Android、iOS、JVM 桌面、Web（JS / WasmJS），以及 Native Windows / Linux / macOS。
- **Web**：同时支持 JS 和 WasmJS；两者存在差异时分别标注。
- **JVM 桌面**与 **Native Windows / Linux / macOS** 分别表示 JVM 与 Kotlin/Native target；仅支持其中部分系统时单独列出。
- **Native Compose**：通用基础库与 Compose 模块均已启用三个桌面 Native target；Compose 通过[桥插件](native-compose)接入自有移植工件，具体系统能力的差异见各模块说明。

未列出的平台表示未启用对应 target，或仅有空实现、占位实现。模块内部分 API 的限制见条目说明。支持 target 不等同于所有 API 均已通过该平台的运行验证。构建工具在 Gradle 中运行，不具有应用运行 target。

按用途查找：

- **[核心与基础能力](#核心与基础能力)**：平台基础、协程、上下文、文件、网络、存储、选择器、启动服务、异常处理与解析。
- **[Compose 界面与应用](#compose-界面与应用)**：主题、UI、导航、应用入口、配置、游戏与组件。
- **[客户端与服务端](#客户端与服务端)**：共享协议、客户端引擎、Native 服务端与数据库。
- **[平台集成](#平台集成)**：JVM FFI 与 Kotlin/Native Win32 专属封装。
- **[构建工具](#构建工具)**：测试聚合与 API 文档生成。

## 核心与基础能力

<details open>
<summary>18 个模块 · 核心、上下文、动态库、文件、网络、单实例、存储、选择器、服务与解析</summary>

### 核心

- **[core](ylcs-module/core) · 全平台**<br>
  平台识别、协程、序列化、集合、编码与字节操作

### 基础服务 · `foundation/`

- **[context](ylcs-module/foundation/context) · 全平台**<br>
  平台基础上下文与宿主能力提供者

- **[libloader](ylcs-module/foundation/libloader) · Android · iOS · JVM 桌面 · Native Windows / Linux / macOS**<br>
  动态库装载；JVM 支持资源中的动态库，Native 使用系统装载接口，Web 调用为空操作

- **[filesystem](ylcs-module/foundation/filesystem) · 全平台**<br>
  文件、流、平台目录与隐式文件 URI

- **[network](ylcs-module/foundation/network) · 全平台**<br>
  Ktor HTTP、下载与文本 WebSocket

- **[startup](ylcs-module/foundation/startup) · 全平台**<br>
  启动服务依赖图、初始化与销毁；选择器、KV、配置和异常处理可独立使用，由业务按需接入服务

- **[os](ylcs-module/foundation/os) · JVM 桌面 · Native Windows / Linux / macOS**<br>
  桌面应用的进程单实例控制；Android、iOS、Web 提供不限制实例的兼容实现

- **[compression](ylcs-module/foundation/compression) · 全平台**<br>
  RLE、LZ4、Snappy、Deflate、Zlib、Gzip 编解码

- **[cryptography](ylcs-module/foundation/cryptography) · 全平台**<br>
  摘要、对称加密与相关编码工具

- **[fts](ylcs-module/foundation/fts) · 全平台**<br>
  多字段全文、前缀、整词与模糊检索

- **[kv](ylcs-module/foundation/kv) · 全平台**<br>
  独立键值存储，通过构造参数注入上下文与存储目录；移动端/桌面采用 MMKV，Web 使用浏览器存储

- **[picker](ylcs-module/foundation/picker) · 全平台**<br>
  图片、文件与保存位置选择，通过构造参数注入上下文；不依赖 Compose 或 startup，JVM/Native 桌面复用 SDL3 系统对话框

- **[exception](ylcs-module/foundation/exception) · 全平台**<br>
  未处理异常回调与报告，通过 `ExceptionHandler.setup` 独立注册，可由业务启动服务管理生命周期

### 文本与数据解析 · `foundation/parser/`

- **[arguments](ylcs-module/foundation/parser/arguments) · 全平台**<br>
  命令行参数解析与类型化读取

- **[lrc](ylcs-module/foundation/parser/lrc) · 全平台**<br>
  LRC 歌词、时间戳与元数据读写

- **[markdown](ylcs-module/foundation/parser/markdown) · 全平台**<br>
  Markdown 语法树、DSL、解析与写入

- **[toml](ylcs-module/foundation/parser/toml) · 全平台**<br>
  TOML 文档模型、DSL、解析与写入

- **[yaml](ylcs-module/foundation/parser/yaml) · 全平台**<br>
  YAML 文档模型、DSL、解析与写入

</details>

## Compose 界面与应用

<details open>
<summary>基础模块 · 10 个模块 · 主题、UI、导航、应用入口与游戏</summary>

### 基础模块 · `compose/`

- **[core](ylcs-module/compose/core) · 全平台**<br>
  Compose 状态扩展、图形辅助与生命周期信号

- **[theme](ylcs-module/compose/theme) · 全平台**<br>
  主题、排版、尺寸、安全区与设备布局分类

- **[ui](ylcs-module/compose/ui) · 全平台**<br>
  基础布局、文本、输入、图像、动画与浮层

- **[icons](ylcs-module/compose/icons) · 全平台**<br>
  Compose 图标资源

- **[platform-view](ylcs-module/compose/platform-view) · 全平台**<br>
  平台视图的状态绑定与生命周期；提供 Android View、Swing、UIKit、DOM 宿主，Native 可用公共状态层，尚无系统原生控件宿主

- **[screen](ylcs-module/compose/screen) · 全平台**<br>
  Navigation 3 页面导航与 ViewModel 生命周期

- **[cache](ylcs-module/compose/cache) · 全平台**<br>
  内存状态缓存与持久缓存抽象

- **[app](ylcs-module/compose/app) · 全平台**<br>
  跨平台应用入口、主题与窗口生命周期；JVM/Native 桌面多窗口及 Android 悬浮窗

- **[config](ylcs-module/compose/config) · 全平台**<br>
  基于 `foundation/kv` 的 Compose 可观察持久配置，可独立使用，不依赖 startup

### 游戏 · `compose/game/`

- **[core](ylcs-module/compose/game/core) · 全平台**<br>
  Compose 游戏引擎、实体、视口、事件与插件

</details>

<details>
<summary>界面组件 · 14 个模块 · 图片、模糊、日历、拖放、多媒体与二维码（compose/components）</summary>

- **[image](ylcs-module/compose/components/image) · 全平台**<br>
  异步图片、动图、缩放、缓存与裁剪选区

- **[blur](ylcs-module/compose/components/blur) · 全平台**<br>
  模糊与遮罩效果

- **[calendar](ylcs-module/compose/components/calendar) · 全平台**<br>
  日历、农历与日期选择状态

- **[data-table](ylcs-module/compose/components/data-table) · 全平台**<br>
  数据表格、排序、编辑与选择

- **[drag-drop](ylcs-module/compose/components/drag-drop) · JVM 桌面 · Web · Native Windows / Linux / macOS**<br>
  组件区域内的文本与文件拖放；Android/iOS 调用不启用拖放

- **[hexview](ylcs-module/compose/components/hexview) · 全平台**<br>
  十六进制数据查看

- **[log-output](ylcs-module/compose/components/log-output) · 全平台**<br>
  日志输出与查看组件

- **[lottie](ylcs-module/compose/components/lottie) · 全平台**<br>
  Lottie 动画渲染

- **[media](ylcs-module/compose/components/media) · 全平台**<br>
  音频、音乐、音效与视频；JVM/Native 桌面音频共用 miniaudio，视频分别使用 Windows WinRT、Linux GStreamer、macOS AVFoundation，Linux 视频需要系统 GStreamer 及解码插件。Web 支持视频与字节音效，AudioPlayer/MusicPlayer 当前为空实现

- **[pagination-layout](ylcs-module/compose/components/pagination-layout) · 全平台**<br>
  分页数据状态与加载布局

- **[qrcode](ylcs-module/compose/components/qrcode) · 全平台**<br>
  全 target 二维码生成；扫码仅 Android/iOS 实现

- **[reordering](ylcs-module/compose/components/reordering) · 全平台**<br>
  拖动列表重排

- **[rich-text](ylcs-module/compose/components/rich-text) · 全平台**<br>
  富文本解析、节点与渲染

- **[webview](ylcs-module/compose/components/webview) · Android · iOS · Web**<br>
  Android/iOS 原生网页视图与 Web iframe；JVM/Native 桌面 target 提供占位实现，无界面网页拦截仅 Android 实现

</details>

## 客户端与服务端

<details>
<summary>5 个模块 · 共享协议、客户端、Native 服务端、MySQL 与 Redis</summary>

### 共享协议与客户端 · `cs/`

- **[core](ylcs-module/cs/core) · 全平台**<br>
  共享 API、资源树、文件参数与 Socket 契约

- **[client-engine](ylcs-module/cs/client-engine) · 全平台**<br>
  类型化 POST、multipart 请求与 Socket 客户端

### Native 服务端 · `cs/`

- **[server-engine](ylcs-module/cs/server-engine) · Native Windows / Linux / macOS**<br>
  Ktor CIO 原生服务端、路由、上传与插件

- **[server-service-mysql](ylcs-module/cs/server-service-mysql) · Native Windows / Linux / macOS**<br>
  SQLx4k MySQL 连接池、查询与事务

- **[server-service-redis](ylcs-module/cs/server-service-redis) · Native Windows / Linux / macOS**<br>
  ReThis Redis 连接池与命令封装

</details>

## 平台集成

<details>
<summary>4 个模块 · JVM FFI 与 Kotlin/Native Win32 专属封装</summary>

### Kotlin/Native Win32 · `platform/native/win32/`

- **[core](ylcs-module/platform/native/win32/core) · Native Windows**<br>
  Kotlin/Native Win32 基础封装

### JVM FFI · `platform/ffi/`

以下模块面向 JVM 调用 C ABI；Kotlin/Native 端通过 CInterop 接入系统接口，不依赖这些 JVM 模块。

- **[core](ylcs-module/platform/ffi/core) · JVM 桌面**<br>
  JDK Foreign Function & Memory API 基础封装

- **[posix](ylcs-module/platform/ffi/posix) · JVM 桌面**<br>
  JVM 调用 POSIX libc；仅适用于相应操作系统

- **[win32](ylcs-module/platform/ffi/win32) · JVM 桌面**<br>
  JVM 调用 Win32；仅适用于 Windows

</details>

## 构建工具

以下模块在 Gradle 中运行，不具有应用运行 target。

- **[build-tool](ylcs-module/build-tool) · Gradle 构建工具**<br>
  聚合各平台库测试任务；无应用运行 target

- **[dokka](ylcs-module/dokka) · Gradle 构建工具**<br>
  聚合模块 API 并生成 Dokka 站点；无应用运行 target

# 案例：银临茶舍跨平台 App

> [!IMPORTANT]
>
>  基于`Rachel`框架开发的集资讯、听歌、美图、论坛、社交于一体的小银子聚集地应用程序
> 
>  银临茶舍水群：`828049503`
> 
>  银临茶舍官网：[https://yinlin.love](https://yinlin.love)

基于本案例，你可以学习到除了 `Rachel` 框架、`Rachel` 界面库外，更多 `KMP` 在架构设计、跨平台兼容等方面上有趣的实现，包括但不限于如下：

- 多模块、多APP的数据共享、结构共享
- 基于 Screen 的单一数据源的开发模式
- 多窗口、悬浮窗口的黑盒接口
- 音视频、多媒体、WebView 的跨平台实现
- 基于 Canvas 自绘的简单游戏引擎和一款音游的设计实现

### 功能预览

![app1](ylcs-docs/docs/assets/app1.png)
![app2](ylcs-docs/docs/assets/app2.png)
![app3](ylcs-docs/docs/assets/app3.png)
![app4](ylcs-docs/docs/assets/app4.png)
![app5](ylcs-docs/docs/assets/app5.png)
![app6](ylcs-docs/docs/assets/app6.png)
![app7](ylcs-docs/docs/assets/app7.png)

### 宣发视频

https://private-user-images.githubusercontent.com/76944654/586743677-ce62598a-7516-4ca9-bc2c-5b74d3f0907b.mp4?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3Nzc3MjYzMzcsIm5iZiI6MTc3NzcyNjAzNywicGF0aCI6Ii83Njk0NDY1NC81ODY3NDM2NzctY2U2MjU5OGEtNzUxNi00Y2E5LWJjMmMtNWI3NGQzZjA5MDdiLm1wND9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNjA1MDIlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjYwNTAyVDEyNDcxN1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTRmMTE4ODg4OGFlMzQzYTIwNWJkYTg1NDNiYjA2NTZmYzVkMjJmMTg3NTZlNTRiMDMyMDkwYTZlMGY3NzEzMWQmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JnJlc3BvbnNlLWNvbnRlbnQtdHlwZT12aWRlbyUyRm1wNCJ9.5JTH4EpoqLDxRf3v-pLe-ae4Oex3jkzVXeSvhUO2Y_g

## 贡献者

[![Contributors](https://contrib.rocks/image?repo=rachel-ylcs/ylcs-kmp)](https://github.com/rachel-ylcs/ylcs-kmp/graphs/contributors)

## 许可

`银临茶舍` 是在 MIT 许可下许可的开源软件，查看[LICENSE](LICENSE)获取更多信息。

## 致谢 & 依赖

按首字母排序。

|   Type   |     Author      |                                                                                Name                                                                                |
|:--------:|:---------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| Library  |     Google      |                                                            [media3](https://github.com/androidx/media)                                                             |
| Library  |    Jetbrains    | [kotlin](https://github.com/JetBrains/kotlin), [compose-multiplatform](https://github.com/JetBrains/compose-multiplatform), [ktor](https://github.com/ktorio/ktor) |
| Library  |mackron|[miniaudio](https://github.com/mackron/miniaudio)|
| Library  |      smyrgeorge      |                                                             [sqlx4k](https://github.com/smyrgeorge/sqlx4k)                                                              |
| Library  |     Tencent     |                                        [MMKV](https://github.com/Tencent/MMKV), [libpag](https://github.com/Tencent/libpag)                                        |
| Library  |     vendelieu     |                                        [re.this](https://github.com/vendelieu/re.this)                                        |
