## **银临茶舍APP跨平台项目**

| 名称      | 平台  | 运行环境   |
|---------|-----|--------|
| Android | 安卓  | Native |
| IOS     | 苹果  | Native |
| Windows | 桌面  | JVM    |
| Linux   | 桌面  | JVM    |
| Mac     | 桌面  | JVM    |
| Web     | 网页  | Wasm   |
| Server  | 服务器 | JVM    |

### `compose UI multiplatform`

源代码目录 `composeApp`

发布目录 `composeApp/build/production`

1. androidMain
   - 运行：`IDEA - Android App`
   - 发布签名安装包：`composeApp:assembleRelease`
2. iosArm64Main
   - 运行
   - 发布签名安装包
3. desktopMain
   - 运行(Debug)：`composeApp:run`
   - 运行(Release)：`composeApp:runRelease`
   - 发布可执行文件：`composeApp:createReleaseDistributable`
4. wasmJsMain
   - 浏览器运行：`composeApp:wasmJsBrowserRun`
   - 发布网页：`composeApp:wasmJsBrowserDistribution`

  
### `kotlin logic multiplatform`

源代码目录 `shared`


### `iosApp`

源代码目录 `iosApp`, `composeApp/iosArm64Main`


### `server`

源代码目录 `server`

发布目录 `build/production`

- 运行： `IDEA - Ktor`
- 发布可执行文件：`server:buildFatJar`
