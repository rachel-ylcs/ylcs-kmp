## 通用

我们建议你使用`IntelliJ IDEA`作为开发集成环境，当然你也可以使用`VS Code`甚至命令行工具来完成开发。

!!! Tip
    `IntelliJ IDEA`官网: [https://www.jetbrains.com.cn/idea/](https://www.jetbrains.com.cn/idea/)

如果你使用`IDEA`可以获得实时调试、热重载、在线预览、Android部署等体验。

`IDEA`的版本建议使用最新版本，因为框架使用到的`Kotlin`或`AGP`版本可能依赖于最新的`IDEA`版本，
在开发前请在插件市场中安装以下插件来完善你的开发体验：

|                      插件                       |                插件                 |
|:---------------------------------------------:|:---------------------------------:|
|                    Android                    |               Ktor                |
|             Compose Multiplatform             | Kotlin Multiplatform (Only macOS) |
| Compose Multiplatform for Desktop IDE Support |                                   |

然后你需要为构建系统准备`Gradle`，参考版本：`9.1.0`。

!!! Tip
    `Gradle`下载链接: [https://services.gradle.org/distributions/gradle-9.1.0-all.zip](https://services.gradle.org/distributions/gradle-9.1.0-all.zip)
    
    如果你的网络状态较差可以单独下载或从其他镜像源下载然后手动拷贝到`.gradle`目录。

除此之外，你还需要准备`Java SDK`，其中桌面开发最低版本为`JDK 25`，Android开发最低版本为`JDK 21`。
然后设置好JDK的环境变量，并将你项目结构中使用的SDK设置为此JDK。

!!! Tip
    `JDK`下载链接: [https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/)

## Android开发

`Rachel`目前推荐版本是`Android 10` (`SDK 31`)，在更低版本上开发不保证完全的兼容性。

准备真机或模拟器，开启USB调试后连接。然后完成编译、安装、测试。

## iOS开发

在iOS商店中安装`Xcode 16.3`。

## 桌面开发

`Rachel`框架部分模块需要使用到Native库，如果你有使用到还需要安装相应的Native编译器。

#### Windows

下载最新版本的`Visual Studio 2026`，选择`MSVC v145`或以上版本的编译器，建议安装`Windows 10 SDK` 或 `Windows 11 SDK`。

!!! Tip
    Visual Studio下载链接: [https://visualstudio.microsoft.com/](https://visualstudio.microsoft.com/)

#### Linux / macOS

下载使用`g++ 13`或以上版本的编译器。

## Web开发

使用支持`WebAssembly`的主流浏览器，例如`Chrome(>=119)`, `Edge(>=119)`, `Firefox(>=120)`, `Safari(>=18.2)`。

!!! Tip
    具体兼容性文档参见: [https://kotlinlang.org/docs/wasm-configuration.html#wasm-proposals-support](https://kotlinlang.org/docs/wasm-configuration.html#wasm-proposals-support)

## 服务器开发

准备本地机器或部署在云服务器。

如果你需要使用数据库服务，请按需安装`Mysql`或`Redis`服务。