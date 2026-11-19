---
title: Rachel 开发向导
description: Rachel 是以 Kotlin Multiplatform 与 Compose Multiplatform 构建全平台客户端、原生服务端与桌面应用的模块化开发框架。
hide:
  - navigation
  - toc
nostatistics: true
---

<div class="rc-hero">
  <div class="rc-hero__content">
    <div class="rc-eyebrow rc-hero__eyebrow">Kotlin Multiplatform · Compose Multiplatform</div>
    <h1 class="rc-hero__title">一套 Kotlin 代码<br><span class="rc-grad">构建全平台应用</span></h1>
    <p class="rc-hero__lede">
      Rachel 将跨平台入口、共享界面、应用服务、客户端与原生服务端通信、页面导航、平台视图与桌面分发组织为模块化体系，并提供统一的类型安全 DSL。框架能力按模块发布，应用可按实际目标引入依赖。
    </p>
    <div class="rc-hero__actions">
      <a class="rc-btn rc-btn--primary" href="start/"><span>开始阅读</span><span>→</span></a>
      <a class="rc-btn" href="start/first-app/"><span>建立第一个应用</span><span>↗</span></a>
      <a class="rc-btn" href="https://rachel-ylcs.github.io/ylcs-kmp/overrides/gallery/" target="_blank" rel="noopener"><span>UI Gallery</span><span>↗</span></a>
    </div>
    <div class="rc-hero__meta">
      <span class="rc-chip rc-chip--accent">Android</span>
      <span class="rc-chip">iOS</span>
      <span class="rc-chip">Windows</span>
      <span class="rc-chip">Linux</span>
      <span class="rc-chip">macOS</span>
      <span class="rc-chip">Web</span>
      <span class="rc-chip">Server</span>
    </div>
  </div>
  <div class="rc-hero__visual">
    <div class="rc-hero__frame">
      <div class="rc-hero__frame-inner">
        <img src="assets/title-hero.png" alt="Rachel 主题人物与流苏插画" loading="eager">
      </div>
    </div>
  </div>
</div>

<div class="rc-metrics">
  <div class="rc-metric">
    <span class="rc-metric__value">KMP</span>
    <span class="rc-metric__label">共享核心与目标配置</span>
  </div>
  <div class="rc-metric">
    <span class="rc-metric__value">CMP</span>
    <span class="rc-metric__label">跨平台界面体系</span>
  </div>
  <div class="rc-metric">
    <span class="rc-metric__value">Native</span>
    <span class="rc-metric__label">无 JVM 桌面与原生服务端</span>
  </div>
  <div class="rc-metric">
    <span class="rc-metric__value">MIT</span>
    <span class="rc-metric__label">开源许可</span>
  </div>
</div>

<div class="rc-section">
  <div class="rc-section__head">
    <div>
      <div class="rc-kicker">框架能力</div>
      <h2 class="rc-section__title">核心能力与模块边界</h2>
      <p class="rc-section__desc">以下能力均由独立模块提供。目标平台、依赖导出与资源范围由模块构建脚本定义，应用可按需引入。</p>
    </div>
  </div>
  <div class="rc-feature-grid">
    <div class="rc-feature">
      <div class="rc-feature__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v18M3 12h18"/><circle cx="12" cy="12" r="8"/></svg></div>
      <h3>多平台目标</h3>
      <p>构建模板支持 Android、iOS、JVM Desktop、Web，以及 Windows、Linux、macOS 的 Kotlin/Native 目标。模块按需启用目标。</p>
      <span class="rc-feature__tag">共享源集</span>
    </div>
    <div class="rc-feature">
      <div class="rc-feature__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 17 10 5l6 12"/><path d="M6.5 13h7"/><path d="M17 5v12"/></svg></div>
      <h3>原生构建路径</h3>
      <p>Kotlin/Native 生成平台二进制，桌面端不依赖 JVM。窗口、媒体、文件与平台句柄由对应所有者创建和释放。</p>
      <span class="rc-feature__tag">Kotlin/Native</span>
    </div>
    <div class="rc-feature">
      <div class="rc-feature__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M8 6 3 12l5 6"/><path d="m16 6 5 6-5 6"/><path d="M13 4l-2 16"/></svg></div>
      <h3>类型安全 DSL</h3>
      <p>模块、源集、依赖、接口、导航与构建产物通过结构化 DSL 声明。类型与导出关系在编译期检查。</p>
      <span class="rc-feature__tag">编译期约束</span>
    </div>
    <div class="rc-feature">
      <div class="rc-feature__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg></div>
      <h3>模块化依赖</h3>
      <p>框架按 core、foundation、compose、cs 与 platform 分组。应用只引入实际使用的模块，依赖方向保持单向。</p>
      <span class="rc-feature__tag">按需依赖</span>
    </div>
    <div class="rc-feature">
      <div class="rc-feature__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2 4 6v6c0 5 3.4 8.4 8 10 4.6-1.6 8-5 8-10V6l-8-4Z"/><path d="m9 12 2 2 4-4"/></svg></div>
      <h3>前后端共享契约</h3>
      <p>接口、资源路径、文件参数与 WebSocket 入口在共享模块中声明，客户端与服务端使用同一套类型与线格式。</p>
      <span class="rc-feature__tag">共享类型</span>
    </div>
    <div class="rc-feature">
      <div class="rc-feature__icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v18"/><path d="M5 8h14"/><path d="M5 16h14"/><circle cx="5" cy="8" r="2"/><circle cx="19" cy="8" r="2"/><circle cx="5" cy="16" r="2"/><circle cx="19" cy="16" r="2"/></svg></div>
      <h3>构建与发布约定</h3>
      <p>统一构建模板、目标开关、运行目录、产物注册与发布配置。新增模块可按固定步骤接入构建与测试流程。</p>
      <span class="rc-feature__tag">构建约定</span>
    </div>
  </div>
</div>

<div class="rc-section">
  <div class="rc-section__head">
    <div>
      <div class="rc-kicker">分层结构</div>
      <h2 class="rc-section__title">模块分层与依赖方向</h2>
      <p class="rc-section__desc">上层模块依赖下层契约；平台差异停留在源集边界。框架模块不依赖业务应用。</p>
    </div>
    <a class="rc-section__more" href="module/">查看模块全景 →</a>
  </div>
  <div class="rc-board">
    <div class="rc-board__head">
      <span>模块分层 / 依赖方向</span>
      <span class="rc-board__dots"><i></i><i></i><i></i></span>
    </div>
    <div class="rc-arch">
      <div class="rc-arch__layer">
        <span class="rc-arch__label">App</span>
        <span class="rc-arch__chips">
          <span class="rc-arch__chip rc-arch__chip--accent">业务模块</span>
          <span class="rc-arch__chip">Gallery</span>
          <span class="rc-arch__chip">Modern Navigation</span>
          <span class="rc-arch__chip">Server</span>
        </span>
      </div>
      <div class="rc-arch__layer">
        <span class="rc-arch__label">UI</span>
        <span class="rc-arch__chips">
          <span class="rc-arch__chip rc-arch__chip--cyan">theme</span>
          <span class="rc-arch__chip">ui</span>
          <span class="rc-arch__chip">screen</span>
          <span class="rc-arch__chip">app</span>
          <span class="rc-arch__chip">platform-view</span>
          <span class="rc-arch__chip">components</span>
        </span>
      </div>
      <div class="rc-arch__layer">
        <span class="rc-arch__label">C / S</span>
        <span class="rc-arch__chips">
          <span class="rc-arch__chip rc-arch__chip--gold">cs:core</span>
          <span class="rc-arch__chip">client-engine</span>
          <span class="rc-arch__chip">server-engine</span>
          <span class="rc-arch__chip">mysql · redis</span>
        </span>
      </div>
      <div class="rc-arch__layer">
        <span class="rc-arch__label">Core</span>
        <span class="rc-arch__chips">
          <span class="rc-arch__chip">core</span>
          <span class="rc-arch__chip">foundation</span>
          <span class="rc-arch__chip">filesystem</span>
          <span class="rc-arch__chip">network</span>
          <span class="rc-arch__chip">startup</span>
          <span class="rc-arch__chip">parsers</span>
        </span>
      </div>
      <div class="rc-arch__layer">
        <span class="rc-arch__label">Native</span>
        <span class="rc-arch__chips">
          <span class="rc-arch__chip">ffi</span>
          <span class="rc-arch__chip">posix · win32</span>
          <span class="rc-arch__chip">native-compose</span>
          <span class="rc-arch__chip">bridge plugin</span>
        </span>
      </div>
    </div>
    <div class="rc-board__note"><i></i>依赖方向：App → UI → C/S → Core → Native；手写指南与生成页面分开维护。</div>
  </div>
</div>

<div class="rc-section">
  <div class="rc-section__head">
    <div>
      <div class="rc-kicker">阅读路径</div>
      <h2 class="rc-section__title">按任务选择阅读路径</h2>
      <p class="rc-section__desc">各章可独立阅读。初次接触 Rachel 时，建议先完成快速开始，再按实际开发目标进入对应章节。</p>
    </div>
  </div>
  <div class="rc-path-grid">
    <a class="rc-path" href="start/">
      <span class="rc-path__index">01</span>
      <h3>建立应用</h3>
      <p>环境、模块依赖、共享应用、平台入口与运行打包。</p>
      <span class="rc-path__arrow">进入快速开始 →</span>
    </a>
    <a class="rc-path" href="ui/">
      <span class="rc-path__index">02</span>
      <h3>编写界面</h3>
      <p>主题、组件、导航、浮层、自适应布局与平台视图。</p>
      <span class="rc-path__arrow">进入 UI 框架 →</span>
    </a>
    <a class="rc-path" href="client/">
      <span class="rc-path__index">03</span>
      <h3>连接后端</h3>
      <p>共享契约、客户端请求、原生服务端、实时通信与数据服务。</p>
      <span class="rc-path__arrow">进入客户端交互 →</span>
    </a>
    <a class="rc-path" href="module/">
      <span class="rc-path__index">04</span>
      <h3>扩展框架</h3>
      <p>源集、API 导出、数据处理、FFI、测试与发布边界。</p>
      <span class="rc-path__arrow">进入深入模块 →</span>
    </a>
  </div>
</div>

<div class="rc-section">
  <div class="rc-section__head">
    <div>
      <div class="rc-kicker">在线资源</div>
      <h2 class="rc-section__title">组件、导航与 API 在线示例</h2>
      <p class="rc-section__desc">以下页面由当前源码生成，与手写指南分开维护。阅读对应章节时可结合在线页面核对实际行为与签名。</p>
    </div>
  </div>
  <div class="rc-eco-grid">
    <a class="rc-eco rc-eco--gallery" href="https://rachel-ylcs.github.io/ylcs-kmp/overrides/gallery/" target="_blank" rel="noopener">
      <div class="rc-eco__top"><span class="rc-eco__icon">UI</span><span class="rc-eco__external">↗</span></div>
      <h3>Rachel UI Gallery</h3>
      <p>组件交互、主题切换、布局与浮层示例。</p>
      <span class="rc-eco__link">打开在线示例 →</span>
    </a>
    <a class="rc-eco rc-eco--nav" href="https://rachel-ylcs.github.io/ylcs-kmp/overrides/modern-screen/" target="_blank" rel="noopener">
      <div class="rc-eco__top"><span class="rc-eco__icon">NAV</span><span class="rc-eco__external">↗</span></div>
      <h3>Modern Navigation</h3>
      <p>导航策略、参数传递、页面恢复与嵌套宿主示例。</p>
      <span class="rc-eco__link">打开导航示例 →</span>
    </a>
    <a class="rc-eco rc-eco--api" href="https://rachel-ylcs.github.io/ylcs-kmp/overrides/dokka/" target="_blank" rel="noopener">
      <div class="rc-eco__top"><span class="rc-eco__icon">API</span><span class="rc-eco__external">↗</span></div>
      <h3>模块 API 参考</h3>
      <p>由源码生成的公开 API、重载与类型约束。</p>
      <span class="rc-eco__link">打开 API 参考 →</span>
    </a>
  </div>
</div>

<div class="rc-panel" style="margin: 4rem 0 1rem; text-align:center">
  <div class="rc-kicker" style="justify-content:center">开始使用</div>
  <h2 class="rc-heading--center" style="border:0; margin:.7rem 0 .4rem; font-size:clamp(1.4rem,2.6vw,2rem)">从模块依赖开始建立应用</h2>
  <p style="max-width:36rem; margin:0 auto 1.2rem; color:var(--rc-text-dim); font-size:.8rem">快速开始包含环境、模块、共享应用、平台入口与运行打包的完整步骤。</p>
  <a class="rc-btn rc-btn--primary" href="start/first-app/"><span>进入快速开始</span><span>→</span></a>
</div>
