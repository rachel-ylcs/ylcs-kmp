---
description: 理解 Rachel 的 FFI、动态库、操作系统集成与平台资源所有权。
---

# 平台与 FFI

<p class="rc-lead">平台模块的目标不是把所有系统 API 包装成通用接口，而是把外部世界的风险收敛到少数边界：指针、句柄、动态库、权限与资源释放。</p>

## 平台分层

| 层 | 模块 | 内容 |
| --- | --- | --- |
| FFI 核心 | `platform/ffi/core` | 外部函数与内存访问基础 |
| POSIX | `platform/ffi/posix` | POSIX 系统调用与类型 |
| Win32 | `platform/ffi/win32` | Windows 原生 API |
| Win32 Native | `platform/native/win32/core` | Win32 原生实现 |
| 动态库 | `foundation/libloader` | 动态库加载与符号解析 |
| 系统集成 | `foundation/os` | 操作系统能力 |
| 选择器 | `foundation/picker` | 文件、图片与媒体选择 |
| 键值存储 | `foundation/kv` | 跨平台 KV |
| 异常 | `foundation/exception` | 未处理异常收集 |

这些模块的平台覆盖并不相同。FFI 与 Win32 目标通常启用桌面 Native；KV、Picker 与 Exception 等基础能力覆盖更广。以各模块构建脚本为准。

## 动态库加载

`foundation/libloader` 负责在运行时定位并加载动态库。使用方需要明确：

| 问题 | 要求 |
| --- | --- |
| 库文件从哪里来 | 构建产物、系统库或应用分发目录 |
| 何时加载 | 首次使用前，避免在 UI 线程阻塞 |
| 加载失败如何处理 | 降级、禁用功能或显式错误 |
| 符号缺失如何处理 | 不假设版本兼容 |
| 何时卸载 | 仅在确认没有活动调用时 |

!!! warning "编译成功不等于运行可加载"
    动态库可能因 ABI、架构、路径、依赖符号或平台策略而加载失败。构建阶段应验证产物，运行阶段应提供明确错误。

## 原生句柄与所有权

平台上下文可能暴露 `windowHandle`、`controller`、`displayHandle` 等借用的句柄：

| 平台 | 句柄 | 所有权 |
| --- | --- | --- |
| Windows Native | HWND | SDL 持有，消费方不得释放 |
| Linux Native | X11 Window / wl_surface | SDL 与合成器管理 |
| macOS Native | NSWindow 指针 | 窗口宿主管理 |
| Android | Activity / Context | 系统与宿主管理 |
| iOS | UIViewController | Swift 宿主管理 |

消费方不得释放借用句柄，也不得在窗口关闭后继续使用。Linux 的 `displayHandle` 与 `windowHandle` 必须来自同一后端，X11 与 Wayland 的值不能互换。

## 选择器

`foundation:picker` 提供系统文件与媒体选择能力。使用方应：

- 在应用级服务中初始化，避免页面重复注册；
- 处理用户取消、权限拒绝与平台不支持；
- 对返回的 URI 使用相应读写能力，而不是直接把 path 当普通绝对路径；
- 限制文件大小、类型与数量。

| URI 类型 | 语义 |
| --- | --- |
| `RegularUri` | 常规地址或路径 |
| `ContentUri` | Android ContentResolver 资源 |
| `SandboxUri` | iOS 沙盒 URL |
| `ImplicitUri` | 不保证可转换为普通绝对路径 |

## 键值存储

`foundation:kv` 提供显式注册上下文与存储目录的跨平台键值存储。`compose:config` 在其上提供可观察持久配置。

```kotlin title="配置"
class AppConfig(kv: KV) : Config(kv) {
    var darkMode by booleanState(default = false)
    var fontScale by floatState(default = 1f)
}
```

| 边界 | 说明 |
| --- | --- |
| 生命周期 | 创建者关闭 KV；管理器使用 `shutdown()` |
| 键生成 | 由属性名与版本参数组合 |
| 重命名 | 会改变存储键，需要迁移或重置 |
| 复杂 JSON | 兼容性由配置模型负责 |

## 异常收集

`foundation:exception` 收集未处理异常并允许自定义记录。它不替代业务错误处理，也不保证捕获所有原生崩溃。记录时应避免写入密码、令牌与完整隐私数据。

## 平台矩阵

| 能力 | Android | iOS | JVM Desktop | Web | Desktop Native |
| --- | --- | --- | --- | --- | --- |
| KV | 支持 | 支持 | 支持 | 支持 | 支持 |
| Picker | 支持 | 支持 | 支持 | 受浏览器限制 | 支持 |
| Exception | 支持 | 支持 | 支持 | 受浏览器限制 | 支持 |
| FFI | 部分 | 部分 | 部分 | 不适用 | 支持 |
| Win32 | — | — | — | — | Windows |
| POSIX | — | — | — | — | Linux / macOS |

表格只描述模块声明与常见实现，不代替具体功能验证。

## 安全边界

1. 外部输入的路径、URL、文件名与命令参数必须校验；
2. 动态库路径与搜索顺序应受控，避免加载不可信目录中的同名库；
3. 原生句柄不跨窗口、不跨生命周期缓存；
4. 文件、流、监听器与设备在所有者结束时释放；
5. 权限请求失败时提供降级路径，不假设系统一定授权；
6. 日志不记录原始隐私内容与密钥。

## 资源释放检查

- [ ] 动态库加载有失败处理；
- [ ] 原生句柄在窗口关闭后不再使用；
- [ ] 选择器返回的 URI 使用正确读写接口；
- [ ] KV 在应用退出时关闭；
- [ ] 监听器、观察者与权限 launcher 已解除；
- [ ] 后台任务在所有者结束时取消。

## 下一步

继续阅读[扩展与验证](extend.md)，了解如何新增模块、补充测试并交付变更。
