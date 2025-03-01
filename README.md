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

## 跨平台特性

| 名称      | 平台  | 运行环境   |
|---------|-----|--------|
| Android | 安卓  | Native |
| IOS     | 苹果  | Native |
| Windows | 桌面  | JVM    |
| Linux   | 桌面  | JVM    |
| Mac     | 桌面  | JVM    |
| Web     | 网页  | Wasm   |
| Server  | 服务器 | JVM    |

1. 各端UI共享，逻辑共享，资源共享，数据共享。
2. 桌面、网页、平板横屏适配、安卓、苹果竖屏适配。
3. 桌面、安卓、网页Wasm代码混淆，高性能低体积。
4. 服务端与客户端共享数据格式、序列化库、网络库

## 工程目录

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

## 部署

### 编译与运行环境

- IntelliJ IDEA 2025.1 EAP (Ultimate Edition)
- Clion 2025.1
- Windows(可编译Android/Windows/Web/Server)
- Mac(可编译Android/IOS/Mac/Web/Server)
- Linux(可编译Android/Linux/Web/Server)

### Common

git clone 源码，gradle同步，下载依赖，构建。

### Android

直接运行或发布。

### IOS

待补充

### Desktop

需要先使用Clion编译项目中的C++库，会自动生成动态链接库文件到libs目录。

打包时会自动将动态链接库复制到输出目录。

### Web

直接运行或发布

### Server

构建后会自动将初始目录复制到当前目录下，可以直接运行或发布。

#### 分离环境

redis和mysql配置可在resources中的config.properties配置
可选择本地调试（连接备用服务器）或生产环境部署（连接localhost）

#### 手动更新

服务器只在第一次运行时复制初始目录，当更新项目resources中如server.json, photos.json类的文件时
并不会同步到服务器中，且重新启动服务器也是如此。
需要手动将文件复制到服务器对应目录下的文件中。