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

It supports `Android`, `iOS`, `Windows`, `Linux`, `macOS`, `Web(Wasm)` and other client and server platforms.

## Feature

- **Cross platform**: Based on `KMP` / `CMP`, it supports six platform clients and servers, with multi terminal consistency.
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

|                       Theme                        |                     Dark Mode                      |
|:--------------------------------------------------:|:--------------------------------------------------:|
|   ![rachel](ylcs-docs/docs/assets/ui/theme.png)    |    ![rachel](ylcs-docs/docs/assets/ui/dark.png)    |
|                        Text                        |                      RichText                      |
|   ![rachel](ylcs-docs/docs/assets/ui/text1.png)    |   ![rachel](ylcs-docs/docs/assets/ui/text2.png)    |
|                       Button                       |                       Input                        |
|   ![rachel](ylcs-docs/docs/assets/ui/input1.png)   |   ![rachel](ylcs-docs/docs/assets/ui/input2.png)   |
|                        Icon                        |                       Image                        |
|   ![rachel](ylcs-docs/docs/assets/ui/image1.png)   |   ![rachel](ylcs-docs/docs/assets/ui/image2.png)   |
|                     Container                      |                   Data Container                   |
| ![rachel](ylcs-docs/docs/assets/ui/container1.png) | ![rachel](ylcs-docs/docs/assets/ui/container2.png) |
|                     Collection                     |                      Calendar                      |
| ![rachel](ylcs-docs/docs/assets/ui/collection.png) |  ![rachel](ylcs-docs/docs/assets/ui/calendar.png)  |
|                     Animation                      |                     Navigation                     |
| ![rachel](ylcs-docs/docs/assets/ui/animation.png)  | ![rachel](ylcs-docs/docs/assets/ui/navigation.png) |
|                       Dialog                       |                       Sheet                        |
|   ![rachel](ylcs-docs/docs/assets/ui/dialog.png)   |   ![rachel](ylcs-docs/docs/assets/ui/sheet.png)    |
|                       Flyout                       |                        Tip                         |
|   ![rachel](ylcs-docs/docs/assets/ui/flyout.png)   |    ![rachel](ylcs-docs/docs/assets/ui/tip.png)     |


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

## Credits

Sort by first letter.

|  Type   |     Author      |                                                                                Name                                                                                |
|:-------:|:---------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| Library | brettwooldridge |                                                      [HikariCP](https://github.com/brettwooldridge/HikariCP)                                                       |
| Library |    Calvin-LL    |                                                      [reorderable](https://github.com/Calvin-LL/Reorderable)                                                       |
| Library |   chrisbanes    |                                                             [haze](https://github.com/chrisbanes/haze)                                                             |
| Library |     Google      |                                                            [media3](https://github.com/androidx/media)                                                             |
| Library |    jenly1314    |                            [zxing-lite](https://github.com/jenly1314/ZXingLite), [camera-scan](https://github.com/jenly1314/CameraScan)                            |
| Library |    Jetbrains    | [kotlin](https://github.com/JetBrains/kotlin), [compose-multiplatform](https://github.com/JetBrains/compose-multiplatform), [ktor](https://github.com/ktorio/ktor) |
| Library |     mlabbe      |                                                   [nativefiledialog](https://github.com/mlabbe/nativefiledialog)                                                   |
| Library |      mysql      |                                                                   [mysql](https://dev.mysql.com)                                                                   |
| Library |      panpf      |                                                             [sketch](https://github.com/panpf/sketch)                                                              |
| Library |     qos-ch      |                                                            [logback](https://github.com/qos-ch/logback)                                                            |
| Library |      redis      |                                                              [jedis](https://github.com/redis/jedis)                                                               |
| Library |     Tencent     |                                        [MMKV](https://github.com/Tencent/MMKV), [libpag](https://github.com/Tencent/libpag)                                        |