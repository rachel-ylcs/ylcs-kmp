---
description: 从环境准备、模块依赖到第一个可运行的 Rachel 应用，建立完整的开发闭环。
---

# 快速开始

<p class="rc-lead">这一章以一个最小的客户端应用为主线，依次完成环境准备、依赖声明、共享应用编写、多平台入口配置以及运行与打包。每一步都保持可独立验证，避免把问题留到最后一个平台才暴露。</p>

!!! abstract "读完本章，你将能够"
    - 使用仓库约定的 Gradle Wrapper 与 JDK 启动构建；
    - 理解 `rachel { multiplatform { sources { ... } } }` 的组织方式；
    - 编写一个共享 `PlatformApplication` 并在桌面、Web、Android 与 iOS 上运行；
    - 选择正确的任务完成调试运行与产物打包。

## 章节导航

<div class="rc-link-grid">
  <a class="rc-link-card" href="../environment/">
    <div class="rc-link-card__index">STEP 01</div>
    <h3>开发环境</h3>
    <p>JDK、Gradle Wrapper、宿主工具链、仓库结构与常见环境边界。</p>
    <span class="rc-link-card__arrow">开始配置 →</span>
  </a>
  <a class="rc-link-card" href="../modules/">
    <div class="rc-link-card__index">STEP 02</div>
    <h3>模块与依赖</h3>
    <p>模块模板、源集关系、<code>lib</code> 与 <code>ExportLib</code> 的导出语义。</p>
    <span class="rc-link-card__arrow">声明依赖 →</span>
  </a>
  <a class="rc-link-card" href="../first-app/">
    <div class="rc-link-card__index">STEP 03</div>
    <h3>编写第一个应用</h3>
    <p>共享应用、启动任务、页面导航与四种平台入口的完整实现。</p>
    <span class="rc-link-card__arrow">编写应用 →</span>
  </a>
  <a class="rc-link-card" href="../platforms/">
    <div class="rc-link-card__index">STEP 04</div>
    <h3>多平台配置</h3>
    <p>目标开关、源集划分、<code>expect/actual</code>、资源与权限。</p>
    <span class="rc-link-card__arrow">适配平台 →</span>
  </a>
  <a class="rc-link-card" href="../run-package/">
    <div class="rc-link-card__index">STEP 05</div>
    <h3>运行与打包</h3>
    <p>调试运行、JVM 与 Native 桌面分发、Web、Android 与 iOS 产物。</p>
    <span class="rc-link-card__arrow">生成产物 →</span>
  </a>
</div>

## 最小应用的组成

一个 Rachel 客户端并不是“所有模块的集合”，而是由三层稳定边界组成的应用：

| 层 | 职责 | 典型类型 |
| --- | --- | --- |
| 共享应用 | 主题、根内容、应用级服务与导航宿主 | `PlatformApplication`、`StartupPool` |
| 页面体系 | 页面生命周期、状态、导航与浮层 | `Screen`、`ScreenManager` |
| 平台入口 | 系统上下文、窗口、Activity 或控制器 | `ComposeActivity`、`NativeWindow` |

```text title="依赖方向"
平台入口  ──▶  共享应用  ──▶  页面与组件  ──▶  核心库与通信契约
```

平台入口只负责建立宿主并把控制权交给共享应用；共享应用不读取平台类型，除非它位于对应源集。这个方向一旦保持，新增平台就只是增加入口，而不是重写业务。

!!! tip "开始之前"
    如果你还没有配置过仓库，先阅读[开发环境](environment.md)。如果你已经能运行仓库中的 Gallery，可以直接跳到[编写第一个应用](first-app.md)。

## 本章使用的示例

示例刻意保持最小：一个页面、一个按钮、一次导航、一个启动任务。真实业务中的网络、数据库与媒体能力分别在后续章节展开。

```kotlin title="HelloApplication.kt"
class HelloApplication(context: PlatformContext) :
    PlatformApplication<HelloApplication>(appReference, context) {

    @Composable
    override fun Content() {
        Text("Hello Rachel")
    }
}
```

下一节先处理环境，确保命令、JDK 与宿主工具链一致。
