---
description: Rachel UI 框架由主题、组件、页面、平台视图与应用入口组成，保持一致的跨平台体验。
---

# 界面体系

<p class="rc-lead">Rachel 的 UI 不是 Material 的封装，而是一套独立的组件与主题体系。它使用 Compose Foundation，保持对原生桌面、移动端与浏览器的适配能力，同时避免把业务界面绑定到单一设计语言。</p>

## 模块地图

| 模块 | 职责 |
| --- | --- |
| `compose:theme` | 颜色、排版、形状、尺寸、边框、阴影、动画与文案 |
| `compose:ui` | 输入、容器、图像、文本、浮层、集合与状态组件 |
| `compose:screen` | 页面模型、导航策略、宿主作用域与页面资源 |
| `compose:app` | 应用入口、平台宿主、独立窗口与系统悬浮窗 |
| `compose:platform-view` | Android View、Swing、UIKit 与 DOM 宿主适配 |
| `compose:components:image` | 异步图片加载、缓存、缩放与动图 |
| `compose:components:*` | 日历、表格、媒体、二维码、富文本、WebView 等可选组件 |

## 设计原则

1. **主题优先。** 使用 `Theme` 中的颜色、尺寸、圆角、阴影与动画，而不是字面量。
2. **状态归属明确。** 页面模型拥有业务状态，组合位置只拥有临时 UI 状态。
3. **组件可组合。** 基础组件不强制业务结构，容器与状态组件通过参数和插槽组合。
4. **平台能力可替换。** 原生视图通过 state + host 接入，不把平台句柄塞进可保存状态。
5. **自适应而非平台分支。** 布局根据窗口容器尺寸分类，而不是根据操作系统名称。

## 页面层级

```text title="页面类型"
ScreenModel : ViewModel
└─ BasicScreen      返回处理、页面浮层、FAB
   └─ Screen        标题栏、动作区、第二标题栏、内容、底栏
```

页面由导航管理，工厂必须为新条目返回新的页面对象。页面实例隔离、`viewModelScope` 与清理语义见[导航与页面](navigation.md)。

## 本章结构

<div class="rc-link-grid">
  <a class="rc-link-card" href="../lifecycle/">
    <div class="rc-link-card__index">5.1</div>
    <h3>入口与生命周期</h3>
    <p>共享应用、启动阶段、平台宿主、独立窗口与服务清理。</p>
    <span class="rc-link-card__arrow">阅读入口与生命周期 →</span>
  </a>
  <a class="rc-link-card" href="../theme/">
    <div class="rc-link-card__index">5.2</div>
    <h3>主题系统</h3>
    <p>颜色、排版、几何、形状、动画、安全区与局部明暗模式。</p>
    <span class="rc-link-card__arrow">阅读主题系统 →</span>
  </a>
  <a class="rc-link-card" href="../components/">
    <div class="rc-link-card__index">5.3</div>
    <h3>组件体系</h3>
    <p>按钮、输入、容器、图像、调色板与可选组件模块。</p>
    <span class="rc-link-card__arrow">阅读组件体系 →</span>
  </a>
  <a class="rc-link-card" href="../navigation/">
    <div class="rc-link-card__index">5.4</div>
    <h3>导航与页面</h3>
    <p>注册、生命周期、参数、策略、恢复边界与深链。</p>
    <span class="rc-link-card__arrow">阅读导航与页面 →</span>
  </a>
  <a class="rc-link-card" href="../floating/">
    <div class="rc-link-card__index">5.5</div>
    <h3>浮层与窗口</h3>
    <p>Dialog、Sheet、Tip、FAB 与 Android 系统悬浮窗。</p>
    <span class="rc-link-card__arrow">阅读浮层与窗口 →</span>
  </a>
  <a class="rc-link-card" href="../adaptive/">
    <div class="rc-link-card__index">5.6</div>
    <h3>自适应与平台视图</h3>
    <p>窗口尺寸分级、布局分支、原生 View / UIView / DOM 宿主。</p>
    <span class="rc-link-card__arrow">阅读自适应与平台视图 →</span>
  </a>
</div>

## 与 Material 的关系

Rachel 的界面库保持独立，不依赖 Material。需要 Material 组件时，可以在应用模块中单独引入，但不要把它作为 Rachel 组件的主题来源。两套设计语言的尺寸、颜色与交互语义不同，混用会增加维护成本。

## 下一步

从[入口与生命周期](lifecycle.md)开始，理解应用从平台宿主到共享页面的完整路径。
