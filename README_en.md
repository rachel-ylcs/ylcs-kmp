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
[中文](README.md) | **English**

# Rachel Rapid Development Framework

`Rachel` is cross-platform rapid development framework that integrates logic, services, and UI based on `Kotlin Multiplatform` and `Compose Multiplatform`, and was developed by the 银临茶舍 project team.

Covering cross-platform entry, shared UI, dependent service startup, client and server interaction engines, free screen navigation, platform native components, custom themes, responsive layout, MDI configuration consistency, page loading, server routing, and other fields.

### We Support Native Compose Desktop Application !!!

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

## Feature

- **Cross-platform**: Based on `KMP` / `CMP`, it supports all platform clients and servers, with multi terminal consistency.
- **Native performance**: Generate platform native binary files without bridging or additional libraries.
- **Single language**: Mastering `kotlin` language is sufficient to complete most task requirements and development.
- **Quick**: You can start building your own cross platform application in just three minutes.
- **Simple**: A large number of type safe and structured framework DSL, without the need to write too much template code.
- **Modularization**: The entire framework is modularized, with clear dependencies between modules, allowing applications to introduce modules as needed.
- **High Collaboration**: The framework supports front-end and back-end collaborative development, sharing data organizational structures.

# Document
Because Rachel has many framework modules and rich functions,
the simplest application can only import the app module (note the version number):
```kotlin
implementation("love.yinlin.compose:app:x.x.x")
```

## Getting Started Documentation: [Rachel Framework Development Wizard](https://rachel-ylcs.github.io/ylcs-kmp)

## API Documentation: [Module API](https://rachel-ylcs.github.io/ylcs-kmp/overrides/dokka)

# Rachel Compose UI Library

The interface library covers various components such as themes, text, inputs, images, animations, containers, collections, navigation, floating windows, etc., supports dark mode, and the interface library is independent, without dependence on `compose.material`.

## UI Online Preview: [Rachel UI Gallery](https://rachel-ylcs.github.io/ylcs-kmp/overrides/gallery)

## Navigation Preview：[Modern Navigation](https://rachel-ylcs.github.io/ylcs-kmp/overrides/modern-screen)

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


# Cross-Platform Module Support

The hierarchy below covers every module under `ylcs-module`. `✅` indicates a supported target; `➖` indicates a target that is not enabled or has only a no-op/placeholder implementation. Feature-specific limits are described in the purpose column. Build tools run in Gradle and have no application runtime targets. A supported target does not establish that every API has been tested on every platform.

| Module hierarchy (under `ylcs-module/`) | Android | iOS | Desktop JVM | WasmJS | JS | Native Windows | Native Linux | Native macOS | Purpose |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | --- |
| [`├─ build-tool`](ylcs-module/build-tool) | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Aggregate platform test tasks; no application runtime target |
| [`├─ dokka`](ylcs-module/dokka) | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Aggregate module APIs into the Dokka site; no application runtime target |
| [`├─ core`](ylcs-module/core) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Platform detection, coroutines, serialization, collections, encodings and byte operations |
| **`├─ foundation/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  ├─ context`](ylcs-module/foundation/context) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Platform context and host capability providers |
| [`│  ├─ filesystem`](ylcs-module/foundation/filesystem) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Files, streams, platform directories and implicit file URIs |
| [`│  ├─ network`](ylcs-module/foundation/network) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Ktor HTTP requests, downloads and text WebSockets |
| [`│  ├─ startup`](ylcs-module/foundation/startup) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Startup dependency graphs, initialization and teardown |
| [`│  ├─ compression`](ylcs-module/foundation/compression) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | RLE, LZ4, Snappy, Deflate, Zlib and Gzip codecs |
| [`│  ├─ cryptography`](ylcs-module/foundation/cryptography) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Digests, symmetric ciphers and encoding utilities |
| [`│  ├─ fts`](ylcs-module/foundation/fts) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Multi-field full-text, prefix, whole-word and fuzzy search |
| **`│  ├─ parser/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  │  ├─ arguments`](ylcs-module/foundation/parser/arguments) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Command-line parsing and typed argument access |
| [`│  │  ├─ lrc`](ylcs-module/foundation/parser/lrc) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | LRC lyrics, timestamps and metadata parsing and writing |
| [`│  │  ├─ markdown`](ylcs-module/foundation/parser/markdown) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Markdown syntax trees, DSL, parsing and writing |
| [`│  │  ├─ toml`](ylcs-module/foundation/parser/toml) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | TOML document model, DSL, parsing and writing |
| [`│  │  ├─ yaml`](ylcs-module/foundation/parser/yaml) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | YAML document model, DSL, parsing and writing |
| **`├─ compose/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  ├─ core`](ylcs-module/compose/core) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Compose state helpers, graphics utilities and lifecycle signals |
| [`│  ├─ theme`](ylcs-module/compose/theme) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Themes, typography, dimensions, insets and layout classification |
| [`│  ├─ ui`](ylcs-module/compose/ui) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Basic layouts, text, inputs, images, animations and floating UI |
| [`│  ├─ icons`](ylcs-module/compose/icons) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Compose icon resources |
| [`│  ├─ platform-view`](ylcs-module/compose/platform-view) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Android View, Swing, UIKit and DOM hosts; Native exposes the common state layer only |
| [`│  ├─ screen`](ylcs-module/compose/screen) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Navigation 3 screens and ViewModel lifecycles |
| [`│  ├─ cache`](ylcs-module/compose/cache) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | In-memory state caches and persistent cache abstractions |
| [`│  ├─ app`](ylcs-module/compose/app) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Application entry points, themes and window lifecycles |
| **`│  ├─ game/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  │  ├─ core`](ylcs-module/compose/game/core) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Compose game engine, entities, viewports, events and plugins |
| **`│  ├─ startup/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  │  ├─ config`](ylcs-module/compose/startup/config) | ✅ | ✅ | ✅ | ✅ | ✅ | ➖ | ➖ | ➖ | Observable persistent configuration backed by KV storage |
| [`│  │  ├─ exception`](ylcs-module/compose/startup/exception) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Unhandled exception collection and reporting service |
| [`│  │  ├─ mmkv-kmp`](ylcs-module/compose/startup/mmkv-kmp) | ✅ | ✅ | ✅ | ✅ | ✅ | ➖ | ➖ | ➖ | Platform KV storage; mobile/desktop MMKV and Web storage adapters |
| [`│  │  ├─ picker`](ylcs-module/compose/startup/picker) | ✅ | ✅ | ✅ | ✅ | ✅ | ➖ | ➖ | ➖ | Platform file and directory pickers |
| **`│  ├─ components/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  │  ├─ image`](ylcs-module/compose/components/image) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Async images, animations, zoom, caching and crop regions |
| [`│  │  ├─ blur`](ylcs-module/compose/components/blur) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Blur and mask effects |
| [`│  │  ├─ calendar`](ylcs-module/compose/components/calendar) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Calendars, lunar dates and date-selection state |
| [`│  │  ├─ data-table`](ylcs-module/compose/components/data-table) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Data tables, sorting, editing and selection |
| [`│  │  ├─ drag-drop`](ylcs-module/compose/components/drag-drop) | ✅ | ➖ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Text and file drag-and-drop; the iOS implementation is currently a no-op |
| [`│  │  ├─ hexview`](ylcs-module/compose/components/hexview) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Hexadecimal data viewer |
| [`│  │  ├─ log-output`](ylcs-module/compose/components/log-output) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Log output and viewer components |
| [`│  │  ├─ lottie`](ylcs-module/compose/components/lottie) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Lottie animation rendering |
| [`│  │  ├─ media`](ylcs-module/compose/components/media) | ✅ | ✅ | ✅ | ✅ | ✅ | ➖ | ➖ | ➖ | Audio, music and video; JVM Linux/macOS video backends are currently no-ops |
| [`│  │  ├─ pagination-layout`](ylcs-module/compose/components/pagination-layout) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Paginated data state and loading layouts |
| [`│  │  ├─ qrcode`](ylcs-module/compose/components/qrcode) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | QR generation on all targets; scanning implemented on Android/iOS only |
| [`│  │  ├─ reordering`](ylcs-module/compose/components/reordering) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Drag-to-reorder lists |
| [`│  │  ├─ rich-text`](ylcs-module/compose/components/rich-text) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Rich-text parsing, nodes and rendering |
| [`│  │  ├─ webview`](ylcs-module/compose/components/webview) | ✅ | ✅ | ➖ | ✅ | ✅ | ➖ | ➖ | ➖ | Android/iOS/Web views; JVM provides a placeholder implementation |
| **`├─ cs/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  ├─ core`](ylcs-module/cs/core) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Shared API, resource-tree, file-parameter and socket contracts |
| [`│  ├─ client-engine`](ylcs-module/cs/client-engine) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Typed POST and multipart requests and socket client helpers |
| [`│  ├─ server-engine`](ylcs-module/cs/server-engine) | ➖ | ➖ | ➖ | ➖ | ➖ | ✅ | ✅ | ✅ | Ktor CIO native server, routes, uploads and plugins |
| [`│  ├─ server-service-mysql`](ylcs-module/cs/server-service-mysql) | ➖ | ➖ | ➖ | ➖ | ➖ | ✅ | ✅ | ✅ | SQLx4k MySQL pools, queries and transactions |
| [`│  ├─ server-service-redis`](ylcs-module/cs/server-service-redis) | ➖ | ➖ | ➖ | ➖ | ➖ | ✅ | ✅ | ✅ | ReThis Redis pools and command helpers |
| **`├─ native/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| **`│  ├─ win32/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  │  ├─ core`](ylcs-module/native/win32/core) | ➖ | ➖ | ➖ | ➖ | ➖ | ✅ | ➖ | ➖ | Kotlin/Native Win32 foundations |
| **`├─ platform/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  ├─ native-lib-loader`](ylcs-module/platform/native-lib-loader) | ✅ | ✅ | ✅ | ➖ | ➖ | ✅ | ✅ | ✅ | Native library loading; Web calls are no-ops |
| **`│  ├─ ffi/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  │  ├─ core`](ylcs-module/platform/ffi/core) | ➖ | ➖ | ✅ | ➖ | ➖ | ➖ | ➖ | ➖ | JDK Foreign Function & Memory API foundations |
| [`│  │  ├─ posix`](ylcs-module/platform/ffi/posix) | ➖ | ➖ | ✅ | ➖ | ➖ | ➖ | ➖ | ➖ | POSIX libc access from JVM on the relevant operating systems |
| [`│  │  ├─ win32`](ylcs-module/platform/ffi/win32) | ➖ | ➖ | ✅ | ➖ | ➖ | ➖ | ➖ | ➖ | Win32 access from JVM on Windows |
| **`│  ├─ os/`** | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | ➖ | Module group |
| [`│  │  ├─ single-instance`](ylcs-module/platform/os/single-instance) | ➖ | ➖ | ✅ | ➖ | ➖ | ➖ | ➖ | ➖ | OS-level single-instance locks for JVM desktop applications |
| [`│  │  ├─ window`](ylcs-module/platform/os/window) | ➖ | ➖ | ✅ | ➖ | ➖ | ➖ | ➖ | ➖ | JVM native-window click-through; Linux is currently a no-op |

Targets and defaults come from each module’s `build.gradle.kts` and `build-logic/public-plugin` templates. WasmJS is the browser WebAssembly target. Native desktop targets are `mingwX64("windows")`, `linuxX64("linux")` and `macosArm64("macos")`; iOS includes `iosArm64` and, on an Apple Silicon macOS host, `iosSimulatorArm64`. Android currently uses minSdk 28 and packages `arm64-v8a`. Compose Native consumers need the matching [Native Compose bridge](native-compose/README.md); JVM FFI targets do not imply Kotlin/Native support. Platform permissions, system services and native dependencies remain API-specific.

# Sample: 银临茶舍 Cross-Platform App

> [!IMPORTANT]
>
>  A small silver gathering application developed based on the `Rachel` framework, integrating information, music listening, beautiful pictures, forums, and social networking.
>
>  银临茶舍 QQ Group：`828049503`
> 
>  银临茶舍 Official WebSite: [https://yinlin.love](https://yinlin.love)

Based on this case, you can learn that in addition to the `Rachel framework` and `Rachel interface library`,
there are more interesting implementations of `KMP` in terms of architecture design and cross-platform compatibility,
including but not limited to the following:

- Multi-module, Multi-APP data sharing and structure sharing
- A single source of truth for Screen-based development
- Black box interface for multi-window and floating windows
- Cross-platform implementation of audio and video, multimedia, and WebView
- A simple game engine based on canvas self drawing

### Feature Preview

![app1](ylcs-docs/docs/assets/app1.png)
![app2](ylcs-docs/docs/assets/app2.png)
![app3](ylcs-docs/docs/assets/app3.png)
![app4](ylcs-docs/docs/assets/app4.png)
![app5](ylcs-docs/docs/assets/app5.png)
![app6](ylcs-docs/docs/assets/app6.png)
![app7](ylcs-docs/docs/assets/app7.png)

### Video

https://private-user-images.githubusercontent.com/76944654/586743677-ce62598a-7516-4ca9-bc2c-5b74d3f0907b.mp4?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3Nzc3MjYzMzcsIm5iZiI6MTc3NzcyNjAzNywicGF0aCI6Ii83Njk0NDY1NC81ODY3NDM2NzctY2U2MjU5OGEtNzUxNi00Y2E5LWJjMmMtNWI3NGQzZjA5MDdiLm1wND9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNjA1MDIlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjYwNTAyVDEyNDcxN1omWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTRmMTE4ODg4OGFlMzQzYTIwNWJkYTg1NDNiYjA2NTZmYzVkMjJmMTg3NTZlNTRiMDMyMDkwYTZlMGY3NzEzMWQmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0JnJlc3BvbnNlLWNvbnRlbnQtdHlwZT12aWRlbyUyRm1wNCJ9.5JTH4EpoqLDxRf3v-pLe-ae4Oex3jkzVXeSvhUO2Y_g

## Contributors

[![Contributors](https://contrib.rocks/image?repo=rachel-ylcs/ylcs-kmp)](https://github.com/rachel-ylcs/ylcs-kmp/graphs/contributors)

## License

`银临茶舍` is an open-source software licensed under the MIT license, Please refer to [LICENSE](LICENSE) for more information.

## Credits & Dependencies

Sort by first letter.

|   Type   |     Author      |                                                                                Name                                                                                |
|:--------:|:---------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| Library  |     Google      |                                                            [media3](https://github.com/androidx/media)                                                             |
| Library  |    Jetbrains    | [kotlin](https://github.com/JetBrains/kotlin), [compose-multiplatform](https://github.com/JetBrains/compose-multiplatform), [ktor](https://github.com/ktorio/ktor) |
| Library  |     mlabbe      |                                                   [nativefiledialog](https://github.com/mlabbe/nativefiledialog)                                                   |
| Library  |      smyrgeorge      |                                                             [sqlx4k](https://github.com/smyrgeorge/sqlx4k)                                                              |
| Library  |     Tencent     |                                        [MMKV](https://github.com/Tencent/MMKV), [libpag](https://github.com/Tencent/libpag)                                        |
| Library  |     vendelieu     |                                        [re.this](https://github.com/vendelieu/re.this)                                        |
