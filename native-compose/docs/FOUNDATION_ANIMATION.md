# Foundation / Animation 移植

日期：2026-10-25。延续 Guide3 与 Compose UI 阶段，版本仍为 Kotlin 2.5.0-Beta2、CMP 1.13.0-beta01、Skiko 0.156.0。
用户本轮要求只做简单构建，测试、程序运行和 CI 由用户完成。

## 模块与消费

| 运行时工件 | 维护源码坐标 | 选用的上游源集 |
| --- | --- | --- |
| foundation-layout | org.jetbrains.compose.foundation:foundation-layout | commonMain → skikoMain → nonJvmMain |
| foundation | org.jetbrains.compose.foundation:foundation | commonMain → skikoMain → nonJvmMain/nativeMain，加维护的桌面 actual |
| animation-core | org.jetbrains.compose.animation:animation-core | commonMain → nonJvmMain，加跨平台线程身份 actual |
| animation | org.jetbrains.compose.animation:animation | commonMain → nonAndroidMain/nonJvmMain |
| animation-graphics | org.jetbrains.compose.animation:animation-graphics | commonMain，共享实现 |

五模块均配置 windows/mingwX64、linux/linuxX64、macos/macosArm64。
Maven group 分别是 love.yinlin.native.compose.foundation 与 love.yinlin.native.compose.animation，版本跟随 CMP；KLIB 身份保持官方 group:artifact。
沿用单个 compose-ui included build，不把多个官方模块合并为单一 KLIB，也不加载上游 Stubs 源集。
根版本目录、Settings 源码映射、生成脚本、桥插件和发布模板现已按模块 family/group 处理三个 Compose 家族。
Runtime、Collection、Annotation 等继续复用官方 Native 工件；新增 shapes 依赖使用已有三目标变体的 Google androidx.graphics:graphics-shapes:1.1.0。

当前预览声明：

```kotlin
commonMain {
    lib(libs.compose.foundation)
}
```

Foundation 的 API 依赖导出 Animation、Runtime、UI；UI 依赖图再带入所需子工件和 Skiko。
因此应用无需重复列出底层依赖，也无需 asProvider()。只用 UI 的应用可用 lib(libs.compose.ui)，本机已单独编译确认此路径。
可选的 animation-graphics 不属于 Foundation 的自动依赖；使用它时再声明 libs.compose.animation.graphics。
Skottie 同样独立按需声明。

## Native 差异

- 复用锁定官方 macOS 的平台无关桌面策略及通用 Skiko 实现，补齐点击、焦点、布局、滚动、选择/光标绘制、TextField 手势与线程同步。
- 文本菜单采用官方 desktop 的真实剪切、复制、粘贴、全选流程，移除 AWT/Swing 依赖；本地化复用 Foundation 既有翻译，读取节点的 LocalLocale。
- 新旧 TextField 通过 UI 层 SDL 文本输入与剪贴板服务工作。字符来自 SDL_TEXT_INPUT，键盘 KeyDown 仅处理快捷键/导航，避免一次输入被插入两次。
- Windows/Linux 与 macOS 分别保留 Ctrl/Meta/Alt 快捷键策略；macOS 字符面板调用 AppKit，Windows/Linux 继续由系统快捷键处理。
- 滚轮使用 SDL 已处理方向的 delta，按平台转换为逻辑滚动距离；小数 delta 作为精细滚动。
- ReceiveContent 补齐媒体类型、拖入生命周期、监听器消费与 text/file-path 传递。UI ClipEntry 新增拥有自身副本的 filePaths；原 withPlainText/getPlainText API 保持兼容，不读取拖入文件或保留 OS 句柄。
- Animation Transition 按引用比较线程身份，使用 Native ThreadLocal token，避免装箱线程号导致同一线程比较失败。
- Kotlin 2.5 对两个表达式函数的控制流推断更严格，通过源准备补丁显式保留返回类型；没有修改其行为。

所有派生文件在 adaptation 中维护并保留上游许可证；compose-origins.json 记录来源。
compose_adapt.py 在固定 checkout 中应用可重复、检测漂移的修改，并移除被替代的 actual，让 Gradle 与 IDEA 使用同一份实现。
Foundation 的 skikoMain 直接声明其使用的 Skiko API，避免仅宿主 KLIB 编译通过而共享元数据缺失类型。

## 预览与构建

ylcs-app/native-compose 使用真实 Column/Row、BasicTextField(state)、SelectionContainer、LazyColumn、clickable 和 Canvas，
并演示 animateColorAsState、animateFloatAsState、AnimatedVisibility、animateContentSize 与既有 Dialog。
窗口仍由 androidx.compose.ui.window.NativeWindow 提供，应用没有新增平台源码、SDL CInterop 或 Material 依赖。

在仓库根目录的 Windows/JDK 25 环境构建：

```powershell
.\gradlew.bat "-Pnative.compose=true" :compose-ui:foundation-layout:compileKotlinWindows :compose-ui:foundation:compileKotlinWindows :compose-ui:animation-core:compileKotlinWindows :compose-ui:animation:compileKotlinWindows :compose-ui:animation-graphics:compileKotlinWindows :ylcs-app:native-compose:linkDebugExecutableWindows
```

共享元数据任务为各模块 compileNativeMainKotlinMetadata。实际命令和日志见 [VALIDATION.md](VALIDATION.md)。
Linux/macOS 需要对应宿主；本机 Windows 成功不能证明这两端通过。
新 included projects 首次接入后，IDE 需要 Reload All Gradle Projects。

## 验证与保留边界

本轮不修改 .github/workflows/native-core.yml、ciModules 或既有 UI 测试/报告契约；没有执行测试、软件/GPU smoke、运行、Maven 发布或独立消费者验证。
上一阶段 72 项 UI 测试与 6 项 Maven 消费测试不代表新增 Foundation/Animation 行为已验收。
发布配置已扩展五模块，但跨宿主工件、发布元数据和独立消费仍需用户后续验证。

桌面不显示触摸光标手柄、放大镜与 overscroll 效果，沿用官方 desktop/macOS 策略；没有把触摸支持标为完成。
系统剪贴板仍为纯文本；拖入可传递文本和文件路径，不表示具备图片/HTML 系统剪贴板或完整向外拖出。
animation-graphics 保留锁定上游 PropertyValuesHolder2D 的 path animation TODO，本轮没有扩展该上游功能。
IME、快捷键、菜单、滚动、动画、焦点、缩放与 GPU 的实际行为均等待运行验收。

参考源码：

- [锁定官方 Compose](https://github.com/JetBrains/compose-multiplatform-core/tree/bb05011eb10784ada1ec00494f1a710ba6ab6252/compose)：源集、build-fork.gradle、macOS 与 desktop 实现。
- [brahmkshatriya/compose-native](https://github.com/brahmkshatriya/compose-native)：Native 桌面源集与 Skiko/输入衔接。
- [bitsycore/compose-desktop-native](https://github.com/bitsycore/compose-desktop-native)：源码选择、Foundation Native 依赖及轻量维护方式。

社区代码用于核对方案；本实现继续采用本仓库固定的官方版本、独立工件边界与 SDL 宿主。
