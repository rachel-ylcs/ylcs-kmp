<div align="center">
    <h1>银临茶舍跨平台APP</h1>
    <p>集资讯、听歌、美图、论坛、社交于一体的小银子聚集地 水群：828049503</p>
    <div>
        <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android"/>
		<img src="https://img.shields.io/badge/Platform-iOS%20%2F%20macOS-lightgrey.svg?logo=apple"/>
        <img src="https://img.shields.io/badge/Platform-Windows-blue.svg?logo=esotericsoftware"/>
        <img src="https://img.shields.io/badge/Platform-Linux-red.svg?logo=linux"/>
        <img src="https://img.shields.io/badge/Platform-WASM%20%2F%20JS-yellow.svg?logo=javascript"/>
        <img src="https://img.shields.io/badge/Platform-Server-orange.svg?logo=openjdk"/>
    </div>
    <br>
</div>


| 名称      | 平台  | 运行环境   |
|---------|-----|--------|
| Android | 安卓  | Native |
| IOS     | 苹果  | Native |
| Windows | 桌面  | JVM    |
| Linux   | 桌面  | JVM    |
| Mac     | 桌面  | JVM    |
| Web     | 网页  | Wasm   |
| Server  | 服务器 | JVM    |


### 跨平台特性

1. 各端UI共享，逻辑共享，资源共享，数据共享。
2. 桌面、网页、平板横屏适配、安卓、苹果竖屏适配。
3. 桌面、安卓、网页Wasm代码混淆，高性能低体积。
4. 服务端与客户端共享数据格式、序列化库、网络库

发布目录 `outputs`


### `compose UI multiplatform`

源代码目录 `composeApp`


1. androidMain
   - 运行：`IDEA - Android App`
   - 发布签名安装包：`composeApp:androidPublish`
2. iosArm64Main
   - 运行
   - 发布签名安装包
3. desktopMain
   - 运行(Debug)：`composeApp:desktopRunDebug`
   - 运行(Release)：`composeApp:desktopRunRelease`
   - 检查模块完整性: `composeApp:desktopCheckModules`
   - 发布可执行文件：`composeApp:desktopPublish`
4. wasmJsMain
   - 浏览器运行：`composeApp:webRun`
   - 发布网页：`composeApp:webPublish`


### `kotlin logic multiplatform`

源代码目录 `shared`


### `iosApp`

源代码目录 `iosApp`, `composeApp/iosArm64Main`


### `server`

源代码目录 `server`

- 运行： `IDEA - Ktor`
- 发布可执行文件：`server:serverPublish`
