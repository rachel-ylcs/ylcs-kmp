---
description: 理解 Rachel 核心库、Compose 核心与基础能力模块的分层与选择。
---

# 能力分层

<p class="rc-lead">核心库回答三个问题：平台是什么、异步如何调度、数据如何表达。<code>core</code> 提供不依赖 Compose 的通用能力，<code>compose/core</code> 提供 Compose 状态与窗口辅助，<code>foundation</code> 提供文件、网络、启动与数据处理。</p>

## 三层能力

| 层 | 模块 | 依赖 | 适合放置 |
| --- | --- | --- | --- |
| 通用核心 | `ylcs-module/core` | 无 Compose | 平台识别、协程、序列化、URI、数据模型 |
| Compose 核心 | `ylcs-module/compose/core` | Compose Runtime | 状态工厂、窗口生命周期、图形辅助 |
| 基础能力 | `ylcs-module/foundation/*` | 按需 | 文件、网络、启动、压缩、加密、解析器 |

```text title="依赖方向"
compose/ui、compose/screen  →  compose/core  →  core
foundation/*               →  core
cs/*                       →  core、foundation
```

!!! note "不要从分层推断平台支持"
    某个模块可能声明了多个目标，但部分平台实现仍可能是占位或空实现。以源集和模块构建脚本为准。

## 选择模块

| 你需要 | 使用 |
| --- | --- |
| 判断当前平台 | `core` 的 `Platform` |
| 切换 I/O、CPU、主线程 | `core` 的 `Coroutines` |
| 处理异常与清理 | `core` 的 `Catching` 系列 |
| JSON 编解码 | `core` 的 `JsonEx`、`SerializerEx` |
| URI 解析与编码 | `core` 的 `Uri` |
| 文件与目录 | `foundation:filesystem` |
| HTTP 与下载 | `foundation:network` |
| 应用级服务编排 | `foundation:startup` |
| 压缩、摘要、检索 | `foundation:compression`、`cryptography`、`fts` |
| TOML、YAML、Markdown、LRC | `foundation:parser/*` |
| Compose 状态委托 | `compose/core` |
| 窗口焦点与生命周期 | `compose/core` 的 window API |

## 本章结构

<div class="rc-link-grid">
  <a class="rc-link-card" href="../essentials/">
    <div class="rc-link-card__index">3.1</div>
    <h3>基础核心</h3>
    <p>平台、协程、异常、JSON、URI、文件、字节读写与依赖分析。</p>
    <span class="rc-link-card__arrow">阅读基础核心 →</span>
  </a>
  <a class="rc-link-card" href="../compose-core/">
    <div class="rc-link-card__index">3.2</div>
    <h3>Compose 核心</h3>
    <p>状态工厂、延迟引用、一次执行标记、窗口生命周期与可移动内容。</p>
    <span class="rc-link-card__arrow">阅读 Compose 核心 →</span>
  </a>
  <a class="rc-link-card" href="../capabilities/">
    <div class="rc-link-card__index">3.3</div>
    <h3>基础能力模块</h3>
    <p>上下文、文件系统、网络、启动、压缩、加密、检索与解析器。</p>
    <span class="rc-link-card__arrow">阅读基础能力 →</span>
  </a>
</div>

## 设计原则

1. **平台差异留在边界。** 公共类型不直接暴露 JVM、Android 或 Apple 专属 API。
2. **阻塞操作离开 UI 线程。** I/O 与 CPU 工作使用对应调度上下文。
3. **资源所有权明确。** 文件、流、监听器与原生句柄由创建者或明确的所有者释放。
4. **协议与持久化保持兼容。** 字段名、默认值与序列化格式的修改需要评估旧数据。

下一节从 `core` 的最小能力开始。
