# macOS 移植工件接管与源码核验

日期：2026-10-26。范围按用户确认限定为 native-compose 下全部已移植工件：十四个 Compose 工件、Skiko Core、Skottie，共十六个。
macosArm64 与 Windows/Linux 一样，由本项目编译和发布，消费时重定向；官方存在 macOS 变体不构成改用官方二进制的理由。
Runtime、Lifecycle、SavedState 等尚未在本目录移植的基础依赖继续按原方案复用。

## 坐标与实现所有权

应用继续声明官方版本目录坐标。维护模式指向登记的 source project，普通模式指向 love.yinlin.native.* 工件；缺少自有 macOS 工件时解析失败。
源码来自固定版本的官方/社区实现，原样共享代码留在 vendor，维护差异在 adaptation；保留许可证及来源。
KLIB 身份和源码项目 group 保留 org.jetbrains.*，用于兼容预编译依赖。这不代表消费官方发布的 macOS KLIB；核验应检查 selected component，而不是仅看包名或 group 字符串。

- upstream/compose.json 的 ownedTargets 要求每个已移植 Compose 模块覆盖三个桌面 Native 目标及对应共享元数据，且使用自有发布坐标。
- 桥插件拒绝移除内置规则的 macOS 覆盖，或把自有端口改回官方 Maven group；此约束不强制业务模块创建额外 target。
- org.jetbrains.compose.ui:ui-macosarm64 等显式 Native 叶子坐标，与正常 ui 请求使用同一重定向。仅在相应 Native target 配置中匹配，JVM/Android 与混合公共 metadata 保持原有解析边界。
- verifyNativePortDependencies 解析 Native compile 图，检查每个实际出现的移植依赖是否选中登记源码项目或指定的自有 Maven GAV；遇到未解析依赖或官方端口二进制则失败。

```powershell
.\gradlew.bat "-Pnative.compose=true" :ylcs-app:native-compose:verifyNativePortDependencies
```

该任务不编译 macOS KLIB、不解析目标产物文件，也不运行应用。nativeBridgeReport 继续只展示配置规则，不触发依赖解析。

## 十六工件的源码审阅

| 工件 | macOS 使用的实现与核验结论 |
| --- | --- |
| Skiko Core | 自有 SDL 窗口与 Metal host，MacosMetalRenderer 创建真实 Metal device/queue/context/drawable；OpenGL 类有实际 context/texture/surface 操作。系统主题读取 NSUserDefaults。修复 SkiaLayer.isShowing 常量返回，并让 pixelGeometry 读取宿主值。共享 Native 已知限制见下文。 |
| Skottie | 复用 Native Kotlin/C++ Skottie bridge 与自有 Core，动画创建、时间轴和渲染调用实际原生实现；没有单独的 macOS 占位分支。 |
| ui-util | 共享工具实现；macOS trace 执行 block 一次，traceValue 不收集事件，属于未接入 profiling 的桌面策略。 |
| ui-geometry | 公共几何运算，无 macOS 专属 actual 或二进制实现需求。 |
| ui-unit | 公共/非 JVM 单位与 Native 实现，无 macOS 空平台分支。 |
| ui-graphics | Native/Skia 图形实现与自有 Core，沿用既有颜色矩阵修复，未选择 Stubs。 |
| ui-text | Darwin NSLocale/NSString 实现真实 locale、大小写和方向；macOS ResolveInterceptor.Default 表示不额外拦截，字体加载仍进入 Skia backend。 |
| ui-skiko | 自有 macOS 字节复制和字体 actual 委托已维护的共享实现，包含边界检查、Data 所有权、Typeface 加载及 variation 设置。 |
| ui-backhandler | 公共 NavigationEvent 实现，SDL ComposeScene 宿主负责分发返回输入，不依赖占位的 AppKit 宿主。 |
| ui | 采用 NativeSceneHost、NativeUiDispatcher、SDL 文本/IME、剪贴板、URI、指针及拖入实现。官方 macosMain 的 WIP 宿主与占位服务不在活动源集。 |
| foundation-layout | common/skiko/nonJvm 布局实现，共用 Native 后端。 |
| foundation | 采用已维护的 Native 菜单、复制/粘贴、选择、键盘映射、滚轮和 ReceiveContent；macOS 字符面板实际调用 AppKit。未使用官方复制快捷键 TODO 和空的 Native 菜单 provider。 |
| animation-core | 共享 Native 原子实现与 ThreadLocal 线程身份；Transition 的引用相等比较不依赖装箱线程号。 |
| animation | 公共/非 Android/非 JVM 动画和布局实现，没有额外 macOS 占位 actual。 |
| animation-graphics | 共享矢量动画实现；保留上游二维路径动画缺口，见下文。 |
| components-resources | 自有应用位置 reader、SDL 文件范围读取/URI/环境与可移植 VectorDrawable DOM，复用官方 common/Skiko 资源语义及官方 Gradle 生成流程；不使用官方 macOS 源码目录回退 reader。 |

本轮不把源码中出现空函数、false、UNKNOWN 或 error 一律认定为占位。例如：

- loadAngleLibrary/loadOpenGLLibrary 在自有 Native macOS 构建中无需像 JVM 一样动态加载 JNI 库，系统框架和静态归档由链接参数提供。
- Metal-only 的 SDL host 拒绝 OpenGL 调用是后端能力约束；真正的 OpenGL host/renderer 实现单独存在。
- 未提供 LCD 像素排列时使用 PixelGeometry.UNKNOWN 是有效默认值；宿主明确提供该值时，SkiaLayer 现在会传递它。
- 桌面不显示触摸放大镜/光标手柄、无触摸模式及无 overscroll 效果，是已记录的桌面交互策略，不等于选择、滚动或光标绘制为空。

## 已确认的官方缺口与活动源码选择

以下结论针对固定提交中的实际源码，不据此推断官方整体投入程度：

| 上游路径 | 发现 | 本项目处理 |
| --- | --- | --- |
| ui/src/macosMain/.../draganddrop/DragAndDrop.macos.kt | nativeEvent getter 为 TODO | 未选择该源集，使用自有 Native 数据及 SDL 拖入事件 |
| ui/src/macosMain/.../platform/PlatformClipboard.macos.kt | ClipMetadata 为 TODO | 未选择该文件，使用拥有自身数据的 ClipEntry/ClipMetadata |
| ui/src/macosMain/.../window/ComposeWindow.macos.kt | 生命周期及关闭清理仍有 TODO | 使用自有 ComposeScene owner 与关闭流程 |
| foundation/src/macosMain/.../selection/SelectionManager.macos.kt | isCopyKeyEvent 为 TODO | 使用已维护的 Ctrl/Meta/Copy key 映射 |
| resources/library/src/macosMain/.../ResourceReader.macos.kt | 可执行程序回退读取工程 src | 使用自有部署目录 reader |
| Skiko SkiaLayer.macos.kt | 官方和锁定社区参考均将 isShowing 固定返回 true | 自有实现查询 SDL hidden/minimized 状态；NSView 路径检查所属窗口显示/最小化及视图隐藏状态，保留 MetalRedrawer 单独处理遮挡的逻辑 |

MacosSkiaLayerComponent 新增带默认实现的 isShowing 属性；已有自定义宿主源码无需新增实现，默认按 drawable 尺寸判断，宿主可覆写真实可见性。
本轮同时修正不支持 renderApi 时显示错误值的诊断，以及 fullscreen 的过时注释；没有重写窗口生命周期或改变渲染后端路线。

## 保留的明确限制

- Skiko 共享 Native reachabilityBarrier 仍为空，fromInteropNativePointerArray 仍有 TODO；本轮没有把它们包装成已实现，需要单独的 GC/FFI 设计与压力验证。
- animation-graphics 的 PropertyValuesHolder2D path animation 仍保留上游 TODO，影响所有目标，不是 macOS 独有问题。
- 系统主题 getter 是真实系统查询，但动态系统主题事件到 Compose 状态刷新的完整衔接仍有限；不能从 getter 存在推断实时切换已完成。
- 完整 OS 无障碍、系统控件嵌入、向外拖出、多窗口等仍按原阶段边界推进。
- 静态审阅不等于逐 API 行为通过；macOS 编译、AppKit/Metal/GPU、显示状态及长期资源行为必须在对应宿主验收。

## 本轮实际验证

Windows 上经 IDEA MCP 执行依赖解析核验与 APP Kotlin 编译，macos-owned-ports.log 为 BUILD SUCCESSFUL in 2m15s。
外部 init 脚本仅对本次构建向 macosMain 临时加入十六个工件的官方主坐标及 *-macosarm64 坐标，共 32 个请求；未改 APP 依赖声明。

- macOS 解析图选中十六个登记的自有源码项目，所有请求均可解析，未消费对应官方二进制。
- Windows/Linux 的正常 APP 编译图各核对十四个实际依赖；可选 animation-graphics 与 Skottie 不在普通 APP 闭包中。
- Windows 编译通过，包含新的 SDL CInterop 可见性 helper；不是 macOS 编译或运行通过。

未执行测试、程序、CI、Maven 发布或独立 Maven 消费。workflow 与 ciModules 不变。详细记录见 VALIDATION.md。
