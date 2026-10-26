# Compose Resources 移植与部署

日期：2026-10-26。用户已报告上一阶段 CI 通过，本轮继续 resources，仍只构建和检查产物，不执行程序、测试或 CI。

## 运行时与构建插件的边界

官方 org.jetbrains.compose.components:components-resources 提供运行时 API。commonMain 已实现资源标识、限定符匹配、字符串/数组/复数、缓存和大部分加载流程；Skiko 源集实现图片、SVG、字体等能力。
文件读取、URI、系统环境、同步及 XML DOM 需要平台实现。官方 1.13.0-beta01 有 macosArm64 变体，没有 mingwX64/linuxX64；其 macOS 可执行程序 reader 仍会尝试读取工程 src 下的资源，并未完整部署到可搬移程序目录。

识别 composeResources、把 XML 字符串转换成 .cvr、生成 Res 和各 target 的访问器、组装及发布跨模块资源，都属于 org.jetbrains.compose Gradle 插件和 KGP 的资源协议，不属于运行时库中的 commonMain。
本移植继续使用官方插件，增加 Native 的部署衔接与运行时 actual。

当前构建链路：

1. src/commonMain/composeResources 等有效源集目录由官方插件准备，生成 Res 与 .cvr。
2. assembleWindowsMainResources 等任务按模块包名隔离资源；KGP 收集当前模块和依赖模块的资源变体。
3. Native 桥读取 compilation.defaultSourceSet.resources 中官方插件登记的解析结果，不重新扫描依赖源码。
4. copyWindowsDebugExecutableComposeResources 等任务在链接前同步到二进制目录的 composeResources。
5. stage/run/package 使用同一份资源目录，ZIP 保留目录层级与 UTF-8 文件名。

例如当前 APP 的部署布局：

```text
native_compose.exe
icudtl.dat
composeResources/
    love.yinlin.native_compose.resources/
        drawable/
        files/
        values/strings.commonMain.cvr
        values-zh/strings.commonMain.cvr
```

同步只管理 composeResources 子目录，不清理 EXE 或其他运行文件；即使最后一个资源被删除，也会同步清除旧资源。
有既有可执行 binary 但未启用 nativeApplication 打包时，安装桥插件与官方 Compose 插件也会接入链接前的资源复制。
没有可执行 binary 的库模块继续由官方插件处理资源发布。

## 消费方式

当前预览使用普通插件和根版本目录：

```kotlin
plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
        libs.plugins.composeCompiler,
        libs.plugins.composeMultiplatform,
        libs.plugins.nativeBridge,
    )
}

// rachel { nativeExecutable { sources { ... } } }
commonMain {
    lib(libs.compose.foundation, libs.compose.resources)
}
```

无需 asProvider()，也无需手工配置生成目录或在 APP 添加 SDL CInterop。
Native DSL 现与 multiplatform DSL 一样，为已有资源的模块设置 public Res 及 <uniqueSafeModuleName>.resources 包名；显式资源设置仍可在 DSL 之后覆盖。
预览的包名是 love.yinlin.native_compose.resources。

只有执行准备/组装任务还不足以部署资源；链接、运行和打包应通过已安装 Native 桥的工程任务进行。

## 源码与 Native 实现

- 锁定 JetBrains/compose-multiplatform 的 926b833de872721337612d7ac4629636e9db4d1e，与 CMP 1.13.0-beta01 对应；该库不位于 compose-multiplatform-core。
- vendor/resources/workspace 为这个上游的稀疏工作区，仍将 :components-resources 纳入原有 compose-ui included build。现在共十四个 Compose 运行时工件，不合并官方模块。
- 维护目录为 adaptation/org.jetbrains.compose.components_components-resources，Maven group 为 love.yinlin.native.compose.components，KLIB 身份保持 org.jetbrains.compose.components:components-resources。
- commonMain、skikoMain、blockingMain、nativeMain 复用锁定官方实现；替换官方 Apple-only DOM 与锁实现。Windows/Linux/macOS 共用维护的桌面实现。
- SDL 文件接口提供 UTF-8 路径、64 位偏移和拥有自身数据的 ByteArray，避免 Windows 中文路径与文本资源分段读取依赖 JVM。
- 读取支持空文件与零长度范围，校验负数、文件范围和 ByteArray 容量；流在 finally 中释放，不保留 OS 文件句柄。
- reader 相对 SDL_GetBasePath 定位，平面程序目录不依赖工作目录或工程源码。file URI 校验目标文件并对空格、中文、# 等字符编码；绝对路径及父目录段被拒绝。
- SDL CInterop 仅进入三个目标的叶子 fragment，nativeMain 通过只含 Kotlin 类型的 expect 接口调用，保持共享元数据可编译。
- 资源环境复用 Native UI 的 locale 和 Skiko 主题。Composable 的资源密度沿用 LocalDensity；非窗口或后台系统环境查询使用默认密度，避免跨线程调用 SDL 显示接口。
- VectorDrawable DOM 参考固定社区实现，支持命名空间、文本、CDATA 和字符引用；补齐补充平面 Unicode、无前缀属性规则、大小/嵌套限制，并拒绝 DTD。

来源及修改记录位于 upstream/compose.json、compose-origins.json。来源准备脚本保留本地非生成修改，删除替代 actual 前检查维护实现，并保留官方及社区 Apache 2.0 许可。
SVG、图片和字体解码继续使用官方 Skiko 共享实现及已移植的 UI，不新增另一套渲染器或字体后端。

## 本轮构建与交接

预览新增“资源”页，使用生成访问器展示中英文字符串、数组、复数、PNG、SVG、XML 矢量和中文/空格文件名的原始资源。
Windows 生产者构建命令：

```powershell
.\gradlew.bat "-Pnative.compose=true" :compose-ui:components-resources:compileNativeMainKotlinMetadata :compose-ui:components-resources:windowsSourcesJar :ylcs-app:native-compose:packageWindowsDebugExecutable
```

实际日志、结果与未验证范围见 VALIDATION.md。构建、运行暂存目录与 ZIP 中的六个资源文件已逐字节核对一致。
Windows 编译和打包不代表 painterResource、Font、语言切换、URI 打开或搬移后运行已验收；本轮没有启动 APP。
Linux/macOS 构建、真实多模块 Maven 资源发布/消费、文件变更/删除后的运行和三平台交互均留给用户后续验证。
没有修改现有 workflow、ciModules、UI 测试契约，也没有公开 Maven 发布。

一手及参考源码：

- [官方资源库与平台实现](https://github.com/JetBrains/compose-multiplatform/tree/926b833de872721337612d7ac4629636e9db4d1e/components/resources/library)。
- [官方 Gradle 资源生成与多模块协议衔接](https://github.com/JetBrains/compose-multiplatform/tree/926b833de872721337612d7ac4629636e9db4d1e/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/resources)。
- [社区桌面 Native 资源实现](https://github.com/brahmkshatriya/compose-native/tree/660fda7cbad94ba05870aee4f7be9b8e5e224972/compose/components/components-resources)。
- [bitsycore 资源移植](https://github.com/bitsycore/compose-desktop-native/tree/main/components/resources/components-resources)，仅参考维护边界；本项目采用目录部署。
