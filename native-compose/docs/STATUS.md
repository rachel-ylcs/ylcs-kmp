# Native Compose 当前状态

更新时间：2026-10-26（Asia/Shanghai）。后续进度以本文件为准，其他阶段文档保留历史记录。

## 本阶段范围与结论

范围按根版本目录 `libs.versions.toml` 中 Rachel 框架及业务所需的 Compose、导航依赖及其传递依赖闭包确定。包括 Runtime、UI、Foundation、Animation、Resources、Navigation3 和底层 Skiko/Skottie；不加入项目未使用的 Material、Material3。其他 AndroidX 及共享数据/网络库先核验官方 Native 支持，只移植实际缺失的工件。

这一范围的 Compose 与 Navigation3 库移植及发布接线已补齐，Windows 构建及独立 Maven 消费构建通过。用户已提交前一阶段 CI，本轮未等待或查询其结果。Linux/macOS 的完整编译、链接、运行及本轮新增 Navigation3 的 CI 尚待用户执行，不能由本机结果推断三平台验收通过。

| 依赖范围 | 当前处理 |
| --- | --- |
| Runtime、Runtime Saveable、Runtime Retain | 复用官方已有 Native 实现；已核对当前缓存的官方模块元数据均包含 `mingw_x64`、`linux_x64`、`macos_arm64` 变体，并纳入独立消费构建。 |
| UI 八工件 | 自有 `ui`、`ui-backhandler`、`ui-util`、`ui-geometry`、`ui-unit`、`ui-graphics`、`ui-skiko`、`ui-text`。 |
| Foundation | 自有 `foundation-layout`、`foundation`。 |
| Animation | 自有 `animation-core`、`animation`、`animation-graphics`。 |
| Resources | 自有 `components-resources` 运行时；复用官方 Compose Gradle 插件识别、生成和聚合资源，由 Native 桥部署资源目录。 |
| Navigation3 | 自有 `navigation3-ui:1.2.0`；官方 `androidx.navigation3:navigation3-runtime:1.2.0` 与 `androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0` 已有三个桌面 Native 实现，继续复用。 |
| Skiko | 自有 Core、Skottie，静态 Skia/SDL 路线保持不变。 |

十四个 Compose 工件、Navigation3 UI 和两个 Skiko 工件，共十七个工件，均接管三个桌面 Native 目标，包括 macOS。显式请求官方平台叶子坐标时也会重定向，不再回落到官方的同名移植工件。未移植、已有真实 Native 支持的官方 Runtime 工件不属于这十七个接管工件。

普通消费者继续使用版本目录中的公开依赖，如 `implementation(libs.compose.ui)`、`implementation(libs.compose.foundation)`、`implementation(libs.compose.resources)`。UI 子工件和 Skiko 从依赖图引入，无需消费者逐个声明，也无需手写 `Provider` 或 `.asProvider()`。

## 本轮 Navigation3

- 确认官方 `org.jetbrains.androidx.navigation3:navigation3-ui:1.2.0` 缺少 Windows/Linux Native 变体，接管为 `love.yinlin.native.androidx.navigation3:navigation3-ui:1.2.0`。版本目录新增独立 `navigation3` 版本项及 Runtime/私有工件别名；现有公开别名与版本保持兼容。
- 以官方 Maven 的 macOS `1.2.0` source JAR 为发布源码基线，SHA-256 锁定为 `876d621c030cad1c529333a3c880fc57d74f0ebad0736ce3228b25f2bad8482c`。公共源码由 `sync_navigation3.py` 在 vendor 重现；三个 Native 默认转场 actual 维护在 adaptation，保留上游许可证及来源记录。源码准备会检查哈希、路径和非托管修改，支持离线重现。
- 已检查官方 macOS 与 JVM 桌面的三个默认转场：均有意返回 `EnterTransition.None`/`ExitTransition.None`，并非抛错占位。自有 Native 实现保留这个默认行为，`NavDisplay` 仍支持调用方提供普通、返回及预测返回转场。
- 源码准备、KLIB 身份、本地发布、桥插件与维护模式映射支持工件独立版本及上游 group；Navigation3 保持 `1.2.0`，其余 Compose 工件保持 `1.13.0-beta01`，不把旧源码重标为 CMP 版本。
- Navigation3 发布元数据直接依赖同一移植构建的自有 UI/Animation 工件，Native 根坐标与显式平台叶子坐标均可重定向。已核对原始 KLIB 模块身份及完整 Maven 依赖坐标。
- Native 专用预览新增“导航”页，包含 `NavDisplay`、每条目 ViewModel 装饰器、可保存输入状态、前进/返回及转场。只构建，没有运行或验收这些交互。没有修改 `ylcs-module`、业务 APP、页面生命周期或路由设计。

后续迁移模块仍使用 `libs.androidx.navigation3` 和 `libs.androidx.navigation3.viewmodel`；在需要移植依赖的模块手动安装现有 Native Bridge 插件并声明 Native target。`native.compose` 继续只控制源码维护模式，不自动迁移业务模块。

| 本轮验证 | 结果 |
| --- | --- |
| Navigation3 Windows KLIB、公共/Native metadata、本地 file Maven 发布 | 通过，初次构建 `2m 37s`；没有公开发布。 |
| 最新 Native 预览 Windows 编译、链接、资源暂存和 ZIP 打包 | 通过，最终构建 `1m 18s`。 |
| 三目标依赖解析 | 通过；每个目标十七个自有工件，覆盖五十一个显式叶子坐标，同时加入二十四个官方运行时根依赖及 WinHttp/CIO/Darwin HTTP 引擎。只解析图，未进行 Linux/macOS 编译。 |
| 普通声明的独立 Maven 消费构建 | 通过，`1m 22s`；实际编译链接 NavDisplay、ViewModel 装饰器、Resources，并纳入当前共享 kotlinx/Ktor 依赖及 WinHttp。未使用 Compose/Skiko/Navigation3 的源码替换，构建插件来自本地 build-logic。 |
| 修正发布坐标后的独立私有工件消费 | 通过，`9s`；使用新 file Maven 路径检查 Navigation3 可达的八十三个 Maven 组件，确认无源码项目或仍需重定向的自有上游请求。相同 KLIB 的编译/链接任务复用缓存。 |
| 发布及源码检查 | 根 GMM 含三个 Native 变体，根/Windows 版本均为 `1.2.0`，KLIB 身份、依赖坐标、Native actual 与许可证正确；离线源码准备及差异检查通过。 |

已核验 Navigation3 Runtime、Lifecycle/ViewModel/SavedState、NavigationEvent、Collection、Graphics Shapes 等当前 AndroidX 依赖的桌面 Native 支持。共享 kotlinx 日期、IO、协程、序列化及 Ktor 客户端/插件也有三个目标变体，本轮不增加这些库的自有 fork。平台专用依赖仍须在业务迁移时按目标放置，例如现有 kotlinx-html 只用于 JS 站点，JVM/Android 专用依赖不转入 Native。

本轮日志：`vendor/build/logs/navigation3-initial.log`、`navigation3-published-closure.log`、`navigation3-final.log`、`navigation3-maven-consumer.log`、`navigation3-maven-closure.log`。未执行测试、程序或 CI，未修改 workflow、`ciModules`、提交或推送。

## Compose 基础层已补齐

- Skiko 的 `reachabilityBarrier` 改为实际的托管引用使用：线程局部原子引用在 FFI 调用后接收对象并立即清空，避免空实现及长期持有图形资源。
- Native 指针数组保留作用域内已知的元素数量，支持有明确长度的外部缓冲区回读；未知外部缓冲区必须提供数量，不猜测内存长度。数组解码异常也释放原生数组，作用域释放后清空 pin 和长度记录。
- 修正文本整形迭代器及 TextBlob handler 对借用字符串的持有。无论是否拥有字符串都保留引用，只有拥有者负责关闭；避免借用路径向 C++ 传空指针或提前失去字符串。迭代器结束后抛出 `NoSuchElementException`，只读游标的 `remove()` 明确抛出 `UnsupportedOperationException`，保持原有类型签名。
- 完成 `animation-graphics` 上游留空的二维路径运动：分离 x/y 属性，按整条路径距离采样和应用 easing，支持多轮廓、顺序组合、重复及反向。样本有数量上限，运行中的动画不共享可变原生 PathMeasure。
- 修正路径变形的零时长、单关键帧、前后边界和重复进度处理；重复计算改为常数时间。检查不同路径节点数量及 Close 节点不匹配，避免静默截断。Float/Color 动画在反向重复结束后保留实际终点。
- SDL 的主题、语言和显示缩放变化进入窗口事件，Native Compose 宿主刷新主题、locale 和 density 状态；重新获得焦点时也刷新环境。资源的公共环境读取可观察的 `LocalLocale`，非 Composable 资源读取使用 SDL 的系统偏好与主题缓存。
- 所有改动维护在 adaptation 或受漂移检查保护的源码准备脚本中；资源源码准备记录原始/准备后哈希，可识别中断状态并保留非托管修改。源码包包含新适配及来源记录。

`NativeWindowEvent` 新增 `EnvironmentChanged` 事件，现有 Compose 宿主已同步处理。自行使用完整 `when` 枚举该事件的消费者需增加此分支。

## Resources 链路

`commonMain` 已包含资源类型、缓存、环境选择、图片/字体/字符串等共享逻辑，但不能单独完成桌面 Native 的文件读取与部署。本项目补齐 Native actual、跨三桌面目标的 XML DOM、同步锁及 SDL 文件后端。

官方插件继续识别 `composeResources`、生成 `Res`/访问器、转换 values、选择目标源集并聚合依赖模块资源。Native 桥把最终资源树同步到可执行文件旁的 `composeResources`，暂存和 ZIP 使用同一目录；程序按应用位置读取，不依赖启动时的工作目录。保留文件路径检查、范围读取和 file URI 编码。

现有预览仍是 `ylcs-app/native-compose`，直接声明 Foundation 与 Resources，包含图片、SVG、VectorDrawable、字符串/数组/复数及 Unicode 文件名资源。

## 前一阶段 Compose 验证记录

按用户要求未执行测试、APP 运行或 CI，也未扩展 workflow、测试报告契约或 `ciModules`。以下均是构建、依赖解析及产物静态检查。

| 验证 | 结果 |
| --- | --- |
| 本机 Windows 编译、链接、资源暂存和 Debug ZIP 打包 | 通过。最终构建 `BUILD SUCCESSFUL in 2m 41s`。 |
| 十四个 Compose 工件及 Skiko/Skottie 的本地 file Maven 发布任务 | 通过；包含公共/Native metadata、Windows KLIB 与源码产物。没有公开发布。 |
| 三目标依赖图，含全部十六工件、官方根坐标及四十八个显式平台叶子坐标 | 通过；每个目标均选中十六个自有实现，加入版本目录的三个官方 Runtime 根依赖后也没有未解析依赖。此项只解析图，不代表 Linux/macOS 编译。 |
| 十六对根/Windows 发布模块及三十二份源码包 | 通过；根模块含三个 Native 变体，自有工件的传递坐标正确，Windows KLIB 存在，源码与许可证完整。新动画实现、资源环境补丁及 Skiko 生命周期修正均在源码包中。 |
| 预览资源部署与 ZIP | 通过；六个文件在二进制旁、暂存目录和 ZIP 中逐文件字节相同，包括含空格的中文文件名。 |
| 外部独立 Maven 消费者 | 通过，`BUILD SUCCESSFUL in 1m 4s`。Compose/Skiko 均来自 file Maven，未包含其源码构建；构建插件来自本地 build-logic。使用普通版本目录声明，生成并使用 Res，编译并链接 NativeWindow、Foundation、Animation、Resources、Skottie，完成资源 ZIP；消费图核验十六个自有 Maven 工件。未执行生成的程序。 |
| macOS 可见性访问式 | 使用已安装的真实 AppKit 平台 KLIB，将窗口 visible/miniaturized 与视图 hiddenOrHasHiddenAncestor 的访问式编译为 macOS Arm64 KLIB。仅验证该访问式，不代表完整 macOS 工件构建。 |
| 源码与配置检查 | Python 语法、JSON 来源清单及 `git diff --ignore-submodules=all --check` 通过。 |

构建记录位于 `native-compose/vendor/build/logs/`：

- `compose-completion-final.log`：最终本地发布、Windows 编译/链接/打包。
- `compose-catalog-targets.log`：三目标全部接管工件及显式叶子坐标解析。
- `compose-catalog-maven-consumer.log`：外部 Maven 消费编译、链接和资源打包。

## 后续边界

截至本轮，当前版本目录内已知缺失的 Compose/Navigation3 桌面 Native 工件已补齐。接下来由用户进行本轮三平台 CI、运行验收及 GC/回调寿命等压力验证，然后自行进入 Rachel 模块和业务模块的 Native 接入。

本阶段完成的是 Compose 依赖层，未声称 JVM/AWT/Swing 专用业务源码可直接在 Native 上运行。仓库现有桌面 Window/Tray、SwingPanel 等平台封装仍需在对应业务迁移时提供 Native actual；多窗口宿主、OS 无障碍桥、嵌入本机控件及拖出系统等专项宿主能力也没有在本轮扩展。Compose 的共享布局、绘制、动画、文本与资源实现不因此缺少另一个待补的库工件。

用户已报告的前序 CI 成功保留为历史验证，不能替代本轮新改动的验收。没有提交、推送或公开 Maven 发布。
