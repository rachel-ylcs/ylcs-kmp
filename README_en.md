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

Browse all 54 modules under `ylcs-module` by function. Each module name links to its source directory, followed by its supported platforms, purpose and platform-specific limits.

- **All platforms**: Android, iOS, Desktop JVM, Web (JS / WasmJS), and Native Windows / Linux / macOS.
- **Web**: both JS and WasmJS; any differences are listed separately.
- **Desktop JVM** and **Native Windows / Linux / macOS** identify JVM and Kotlin/Native targets respectively. Modules limited to particular operating systems list those systems explicitly.

An omitted platform has no enabled target or only a no-op/placeholder implementation. Individual API limitations appear in the module descriptions. A supported target does not establish that every API has been tested on that platform. Build tools run in Gradle and have no application runtime targets.

Find modules by purpose:

- **[Core and foundation](#core-and-foundation)**: Platform basics, coroutines, files, networking, compression, cryptography and parsing.
- **[Compose UI and applications](#compose-ui-and-applications)**: Themes, UI, navigation, application entry points, games, startup services and components.
- **[Client and server](#client-and-server)**: Shared contracts, client engine, Native servers and databases.
- **[Platform integration](#platform-integration)**: Win32, library loading, JVM FFI and operating-system features.
- **[Build tools](#build-tools)**: Test aggregation and API documentation.

## Core and foundation

<details open>
<summary>16 modules · Platform basics, files, networking, compression, cryptography and parsing</summary>

### Core

- **[core](ylcs-module/core) · All platforms**<br>
  Platform detection, coroutines, serialization, collections, encodings and byte operations

### Foundation services · `foundation/`

- **[context](ylcs-module/foundation/context) · All platforms**<br>
  Platform context and host capability providers

- **[filesystem](ylcs-module/foundation/filesystem) · All platforms**<br>
  Files, streams, platform directories and implicit file URIs

- **[network](ylcs-module/foundation/network) · All platforms**<br>
  Ktor HTTP requests, downloads and text WebSockets

- **[startup](ylcs-module/foundation/startup) · All platforms**<br>
  Startup dependency graphs, initialization and teardown

- **[compression](ylcs-module/foundation/compression) · All platforms**<br>
  RLE, LZ4, Snappy, Deflate, Zlib and Gzip codecs

- **[cryptography](ylcs-module/foundation/cryptography) · All platforms**<br>
  Digests, symmetric ciphers and encoding utilities

- **[fts](ylcs-module/foundation/fts) · All platforms**<br>
  Multi-field full-text, prefix, whole-word and fuzzy search

- **[kv](ylcs-module/foundation/kv) · All platforms**<br>
  Platform KV storage; mobile/desktop MMKV and Web storage adapters

- **[picker](ylcs-module/foundation/picker) · All platforms**<br>
  Platform file and directory pickers

- **[exception](ylcs-module/foundation/exception) · All platforms**<br>
  Unhandled exception collection and reporting service

### Text and data parsers · `foundation/parser/`

- **[arguments](ylcs-module/foundation/parser/arguments) · All platforms**<br>
  Command-line parsing and typed argument access

- **[lrc](ylcs-module/foundation/parser/lrc) · All platforms**<br>
  LRC lyrics, timestamps and metadata parsing and writing

- **[markdown](ylcs-module/foundation/parser/markdown) · All platforms**<br>
  Markdown syntax trees, DSL, parsing and writing

- **[toml](ylcs-module/foundation/parser/toml) · All platforms**<br>
  TOML document model, DSL, parsing and writing

- **[yaml](ylcs-module/foundation/parser/yaml) · All platforms**<br>
  YAML document model, DSL, parsing and writing

</details>

## Compose UI and applications

<details open>
<summary>Core libraries · 10 modules · Themes, UI, navigation, application entry points and games</summary>

### Core libraries · `compose/`

- **[core](ylcs-module/compose/core) · All platforms**<br>
  Compose state helpers, graphics utilities and lifecycle signals

- **[theme](ylcs-module/compose/theme) · All platforms**<br>
  Themes, typography, dimensions, insets and layout classification

- **[ui](ylcs-module/compose/ui) · All platforms**<br>
  Basic layouts, text, inputs, images, animations and floating UI

- **[icons](ylcs-module/compose/icons) · All platforms**<br>
  Compose icon resources

- **[platform-view](ylcs-module/compose/platform-view) · All platforms**<br>
  Android View, Swing, UIKit and DOM hosts; Native exposes the common state layer only

- **[screen](ylcs-module/compose/screen) · All platforms**<br>
  Navigation 3 screens and ViewModel lifecycles

- **[cache](ylcs-module/compose/cache) · All platforms**<br>
  In-memory state caches and persistent cache abstractions

- **[app](ylcs-module/compose/app) · All platforms**<br>
  Application entry points, themes and window lifecycles

- **[config](ylcs-module/compose/config) · All platforms**<br>
  Observable persistent configuration backed by KV storage

### Games · `compose/game/`

- **[core](ylcs-module/compose/game/core) · All platforms**<br>
  Compose game engine, entities, viewports, events and plugins

</details>

<details>
<summary>UI components · 14 modules · Images, blur, calendars, drag-and-drop, media and QR codes (compose/components)</summary>

- **[image](ylcs-module/compose/components/image) · All platforms**<br>
  Async images, animations, zoom, caching and crop regions

- **[blur](ylcs-module/compose/components/blur) · All platforms**<br>
  Blur and mask effects

- **[calendar](ylcs-module/compose/components/calendar) · All platforms**<br>
  Calendars, lunar dates and date-selection state

- **[data-table](ylcs-module/compose/components/data-table) · All platforms**<br>
  Data tables, sorting, editing and selection

- **[drag-drop](ylcs-module/compose/components/drag-drop) · Android · Desktop JVM · Web · Native Windows / Linux / macOS**<br>
  Text and file drag-and-drop; the iOS implementation is currently a no-op

- **[hexview](ylcs-module/compose/components/hexview) · All platforms**<br>
  Hexadecimal data viewer

- **[log-output](ylcs-module/compose/components/log-output) · All platforms**<br>
  Log output and viewer components

- **[lottie](ylcs-module/compose/components/lottie) · All platforms**<br>
  Lottie animation rendering

- **[media](ylcs-module/compose/components/media) · Android · iOS · Desktop JVM · Web**<br>
  Audio, music and video; JVM Linux/macOS video backends are currently no-ops

- **[pagination-layout](ylcs-module/compose/components/pagination-layout) · All platforms**<br>
  Paginated data state and loading layouts

- **[qrcode](ylcs-module/compose/components/qrcode) · All platforms**<br>
  QR generation on all targets; scanning implemented on Android/iOS only

- **[reordering](ylcs-module/compose/components/reordering) · All platforms**<br>
  Drag-to-reorder lists

- **[rich-text](ylcs-module/compose/components/rich-text) · All platforms**<br>
  Rich-text parsing, nodes and rendering

- **[webview](ylcs-module/compose/components/webview) · Android · iOS · Web**<br>
  Android/iOS/Web views; JVM provides a placeholder implementation

</details>

## Client and server

<details>
<summary>5 modules · Shared contracts, client engine, Native servers, MySQL and Redis</summary>

### Shared contracts and client · `cs/`

- **[core](ylcs-module/cs/core) · All platforms**<br>
  Shared API, resource-tree, file-parameter and socket contracts

- **[client-engine](ylcs-module/cs/client-engine) · All platforms**<br>
  Typed POST and multipart requests and socket client helpers

### Native servers · `cs/`

- **[server-engine](ylcs-module/cs/server-engine) · Native Windows / Linux / macOS**<br>
  Ktor CIO native server, routes, uploads and plugins

- **[server-service-mysql](ylcs-module/cs/server-service-mysql) · Native Windows / Linux / macOS**<br>
  SQLx4k MySQL pools, queries and transactions

- **[server-service-redis](ylcs-module/cs/server-service-redis) · Native Windows / Linux / macOS**<br>
  ReThis Redis pools and command helpers

</details>

## Platform integration

<details>
<summary>7 modules · Win32, library loading, JVM FFI, windows and single-instance locks</summary>

### Kotlin/Native Win32 · `native/win32/`

- **[core](ylcs-module/native/win32/core) · Native Windows**<br>
  Kotlin/Native Win32 foundations

### Library loading · `platform/`

- **[native-lib-loader](ylcs-module/platform/native-lib-loader) · Android · iOS · Desktop JVM · Native Windows / Linux / macOS**<br>
  Native library loading; Web calls are no-ops

### JVM FFI · `platform/ffi/`

- **[core](ylcs-module/platform/ffi/core) · Desktop JVM**<br>
  JDK Foreign Function & Memory API foundations

- **[posix](ylcs-module/platform/ffi/posix) · Desktop JVM**<br>
  POSIX libc access from JVM on the relevant operating systems

- **[win32](ylcs-module/platform/ffi/win32) · Desktop JVM**<br>
  Win32 access from JVM on Windows

### Desktop operating systems · `platform/os/`

- **[single-instance](ylcs-module/platform/os/single-instance) · Desktop JVM**<br>
  OS-level single-instance locks for JVM desktop applications

- **[window](ylcs-module/platform/os/window) · Desktop JVM**<br>
  JVM native-window click-through; Linux is currently a no-op

</details>

## Build tools

These modules run in Gradle and have no application runtime targets.

- **[build-tool](ylcs-module/build-tool) · Gradle build tool**<br>
  Aggregate platform test tasks; no application runtime target

- **[dokka](ylcs-module/dokka) · Gradle build tool**<br>
  Aggregate module APIs into the Dokka site; no application runtime target

## Platform configuration and integration

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
| Library  |      smyrgeorge      |                                                             [sqlx4k](https://github.com/smyrgeorge/sqlx4k)                                                              |
| Library  |     Tencent     |                                        [MMKV](https://github.com/Tencent/MMKV), [libpag](https://github.com/Tencent/libpag)                                        |
| Library  |     vendelieu     |                                        [re.this](https://github.com/vendelieu/re.this)                                        |
