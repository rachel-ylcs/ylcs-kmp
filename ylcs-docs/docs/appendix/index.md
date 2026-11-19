---
description: Rachel 的在线示例、API 参考、仓库状态与社区入口。
---

# 生态入口

<p class="rc-lead">文档解释“为什么”和“怎么用”，在线示例与 API 参考回答“现在是什么样”。三者来自同一套源码，但承担不同的阅读任务。</p>

## 三个在线入口

<div class="rc-eco-grid">
  <a class="rc-eco rc-eco--gallery" href="https://rachel-ylcs.github.io/ylcs-kmp/overrides/gallery/" target="_blank" rel="noopener">
    <div class="rc-eco__top"><span class="rc-eco__icon">UI</span><span class="rc-eco__external">KMP Web ↗</span></div>
    <h3>Rachel UI Gallery</h3>
    <p>直接操作组件：按钮、输入、容器、图像、浮层、动画与主题切换。适合在阅读组件章节时对照查看真实交互与布局。</p>
    <span class="rc-eco__link">打开 UI Gallery →</span>
  </a>
  <a class="rc-eco rc-eco--nav" href="https://rachel-ylcs.github.io/ylcs-kmp/overrides/modern-screen/" target="_blank" rel="noopener">
    <div class="rc-eco__top"><span class="rc-eco__icon">NAV</span><span class="rc-eco__external">KMP Web ↗</span></div>
    <h3>Modern Navigation</h3>
    <p>观察导航策略、参数传递、页面恢复、嵌套宿主与生命周期。适合在实现页面栈之前理解行为差异。</p>
    <span class="rc-eco__link">打开导航示例 →</span>
  </a>
  <a class="rc-eco rc-eco--api" href="https://rachel-ylcs.github.io/ylcs-kmp/overrides/dokka/" target="_blank" rel="noopener">
    <div class="rc-eco__top"><span class="rc-eco__icon">API</span><span class="rc-eco__external">Dokka ↗</span></div>
    <h3>模块 API 参考</h3>
    <p>由源码生成的公开 API。适合在掌握概念后查询精确签名、重载与类型约束。</p>
    <span class="rc-eco__link">打开 API 参考 →</span>
  </a>
</div>

## 示例工程

| 路径 | 内容 |
| --- | --- |
| `ylcs-app/gallery` | UI 组件、主题、布局与浮层演示 |
| `ylcs-app/modern-screen` | 导航策略、参数、工厂、嵌套导航与页面状态 |
| `ylcs-app/app/portal`、`app/global` | 业务应用入口、全局服务与导航注册 |
| `ylcs-app/server` | 原生服务端、接口、数据库、Redis 与 WebSocket |
| `ylcs-app/mod-manager` | MOD 预览、解包、打包与部署工具 |
| `ylcs-app/web-app` | 浏览器入口与 Web 分发 |

## 社区

| 渠道 | 用途 |
| --- | --- |
| [GitHub Issues](https://github.com/rachel-ylcs/ylcs-kmp/issues) | 缺陷、功能建议与文档问题 |
| [GitHub Discussions](https://github.com/rachel-ylcs/ylcs-kmp/discussions) | 使用讨论与方案交流 |
| QQ 群 | 项目组与用户交流 |

!!! note "提问时提供可复现信息"
    说明目标平台、模块、任务、错误日志与最小复现步骤。不要粘贴令牌、密码、真实用户数据或完整隐私请求体。

## 继续阅读

- [MOD 归档与 MOD Manager](tools.md)：归档格式、打包、解包与部署；
- [文档工程](docs.md)：本指南的技术实现、统计与维护方式。
