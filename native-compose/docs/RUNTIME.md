# Native 运行、静态链接与分发

## 消费者边界

APP 使用 commonMain 的 Skiko/Skottie 和 `org.jetbrains.skiko.window.NativeWindow`。
窗口、事件循环、SDL CInterop、D3D12/Vulkan/Metal adapters 已在移植库内实现。
业务模块没有 sdl.def、nativeInterop、windowsMain/linuxMain/macosMain 窗口代码，也不需要 `sdl.root`。

`nativeApplication { skikoWindow() }` 为这个窗口程序配置运行数据和启动参数；
桥插件负责 target 重定向与 stage/run/package。
它不会为消费者下载或构建 SDL SDK。

## 静态库及运行数据

| 项目 | 当前处理 |
| --- | --- |
| Skia、Core/Skottie C++ bridge | 静态归档嵌入 KLIB，再链接到可执行文件 |
| SDL3 | 生产者从锁定源码构建静态库，嵌入 Core 的 cinterop-sdl KLIB |
| SDL3 的系统链接依赖 | 从构建所得 pkg-config 元数据提取并导出，去除生产者 SDK 路径及 SDL 自身 -l 选项 |
| Windows ICU | 仍由插件收集 icudtl.dat；Maven 消费使用配套 classifier |
| 许可证 | 源码目录和 sources 工件保留来源及许可；插件不复制到运行暂存目录或 ZIP |

Windows 静态 SDL 使用 VS_PATH/vcvars64，与项目现有 BuildDesktopNativeTask 的约定一致。
SDL_SHARED=OFF、SDL_STATIC=ON；Windows 产物为 SDL3-static.lib，Linux/macOS 为 libSDL3.a。
Windows 的 SDL 归档经过与 Skia 相同的 COFF directive 过滤，不额外引入一套默认 CRT 初始化。

Linux SDL 使用当前 Gradle 选择的 Kotlin/Native 发行包中的 Clang、GNU toolchain 和 sysroot。
宿主 X11/Wayland/audio 开发头文件作为后置搜索目录，避免 Ubuntu 新版 glibc 头文件引入
__isoc23_*、memfd_create 等 Kotlin/Native 链接 sysroot 中不存在的符号。
Gradle 的 prepareSdlStatic 依赖下载发行包的任务，再通过 konan.txt 传递实际位置；
该设置只影响生产者，Maven 消费者不编译 SDL。

SDL 的独立 pkg-config 元数据直接解析；有 Requires/Requires.private 时才调用解析器。
元数据中的 -Wl,-framework,... 等 driver 参数转换成 Native ld 接受的参数，保留 weak framework 语义。
Windows Core 自身用于 CRT 的 clang driver 选项保持其原有处理，SDL 元数据审计单独检查规范化结果。

完整 Maven publication 包含主 KLIB、cinterop-skiko、cinterop-sdl、根 metadata、sources 和对应的数据工件。
只复制一个主 KLIB 不等于完整发布；消费者通过 Gradle metadata 自动获得配套 CInterop。
维护模式直接使用项目构建，普通模式使用这些 Maven 工件。

| 平台 | 当前配置随程序分发 | 来自系统 |
| --- | --- | --- |
| Windows x64 | EXE、icudtl.dat | Windows/UCRT、D3D12/DXGI 等系统库和驱动 |
| Linux x64 | 可执行文件 | glibc、Fontconfig/GL、窗口系统和驱动等系统库 |
| macOS ARM64 | 可执行文件 | macOS frameworks、Metal |

不再分发 SDL3.dll、libSDL3.so 或 SDL dylib；也不需要 skiko.dll、AWT runtime JAR 或 JVM。
程序仍依赖操作系统的系统库，静态 SDL 不表示静态化整套操作系统。

ICU 支持将数据编译嵌入或以 udata_setCommonData 提供数据，但需要相应的生成、对齐和初始化。
本阶段维持外部 icudtl.dat，保持插件收集流程不变。
依据：[ICU 数据打包](https://unicode-org.github.io/icu/userguide/icu_data/)。

静态链接通常以 object/section 为单位裁剪，受到函数表、全局初始化、导出和链接选项影响。
SDL 的动态 API 函数表也会影响保留范围，不能保证每个未调用函数都被移除。

## Debug 与 Release

Kotlin/Native 的 binaries.executable 默认创建 Debug/Release，不需要 Android 的 buildTypes。
构建类型由所选 binary/任务决定，和是否点击 IDE Debug、是否生成 ZIP 无关。

| 任务后缀 | 含义 |
| --- | --- |
| runWindowsDebugExecutable | Debug，收集数据后运行 |
| runWindowsReleaseExecutable | 优化的 Release，收集数据后运行 |
| packageWindowsDebugExecutable | Debug ZIP |
| packageWindowsReleaseExecutable | Release ZIP |

Linux/macOS 将 Windows 替换成 Linux/Macos。
IDE 配置 nativeComposeShowcase 为 Debug，nativeComposeShowcaseRelease 为 Release。
serverPublish 明确依赖 linkReleaseExecutableLinux。

Debug 的 Kotlin 优化较少；Release 默认有链接时优化，通常编译更慢。
底层 Skia、SDL 和 C++ bridge 使用 Release，所以 Debug APP 不代表整套图形库都是 Debug。
依据：[Kotlin Native 优化](https://kotlinlang.org/docs/native-improving-compilation-time.html)。

80 MiB 的一次工作集观察不能直接说明 Kotlin 堆大小或 Release 的收益。
应控制相同后端、尺寸和交互过程，区分工作集、私有提交和 GPU 资源。
目前没有可重复的分项内存/性能基准。

## 验证与当前窗口 API 边界

stage/run/package 共用暂存文件集合，显式资源缺失或文件重名会失败。
ZIP 验证会拒绝 SDL 动态文件，检查 Windows PE imports / Linux readelf / macOS otool 依赖，
并解压到新的带空格目录实际运行。

NativeWindow 必须在主线程调用，阻塞直到关闭；render 回调的 Canvas 为借用对象。
当前提供单个事件循环、缩放、基本按键和 CPU/GPU 呈现。
完整 Compose 输入/IME、鼠标与焦点模型、多窗口、无障碍、安装器及 macOS 签名/公证仍需后续实现。
