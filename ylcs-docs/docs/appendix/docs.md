---
description: 本指南的技术实现、构建流程、搜索、统计、主题与维护约定。
statistics: true
---

# 文档工程

<p class="rc-lead">这份指南本身也是一个工程：内容、主题、搜索、阅读统计与生成页面各有边界。理解这些边界，才能在不破坏站点的情况下持续补充内容。</p>

## 站点结构

| 位置 | 内容 |
| --- | --- |
| `ylcs-docs/docs/*.md` | 手写指南 |
| `ylcs-docs/docs/assets` | 图片、样式与脚本 |
| `ylcs-docs/theme` | Material 主题覆盖 |
| `ylcs-docs/docs/overrides` | Gallery、导航示例与 API 生成页 |
| `ylcs-docs/mkdocs.yml` | 导航、主题、插件与 Markdown 扩展 |

手写指南与生成页面分开维护。`overrides` 目录中的内容由构建流程生成，不应直接编辑。

## 文档数据

<div class="rc-metrics">
  <div class="rc-metric">
    <span class="rc-metric__value">{{ pages }}</span>
    <span class="rc-metric__label">页面</span>
  </div>
  <div class="rc-metric">
    <span class="rc-metric__value">{{ words }}</span>
    <span class="rc-metric__label">全文字数</span>
  </div>
  <div class="rc-metric">
    <span class="rc-metric__value">{{ codes }}</span>
    <span class="rc-metric__label">代码行</span>
  </div>
  <div class="rc-metric">
    <span class="rc-metric__value">{{ images }}</span>
    <span class="rc-metric__label">图像</span>
  </div>
</div>

统计由文档构建插件在生成阶段计算，包含页面数、中英文与代码块行数、图片数量。单页阅读时间与字数显示在每页标题下方。

!!! note "统计的含义"
    字数只用于阅读时间估算，不代表内容质量。代码行与图片数量用于观察页面结构，不参与评分。

## 主题与视觉

站点基于 Material for MkDocs，但视觉层完全自定义：

| 层 | 实现 |
| --- | --- |
| 颜色 | `rachel-dark` / `rachel-light` 两套 CSS 变量 |
| 字体 | 展示字体、中文正文、等宽字体的组合 |
| 背景 | 极光渐变、网格与噪点层 |
| 组件 | 卡片、看板、指标、路径、生态入口 |
| 动效 | 滚动显现、指针聚光、进度条与渐变流动 |
| 可访问性 | 减少动态效果偏好、焦点样式、语义结构 |

!!! tip "动效遵循系统偏好"
    `prefers-reduced-motion` 生效时，滚动显现、浮动与渐变动画会被关闭或缩短。

## 搜索

内置搜索启用中文与英文分词。中文需要 `jieba` 依赖；构建环境必须安装，否则搜索索引无法按中文词切分。

| 能力 | 说明 |
| --- | --- |
| 中文分词 | `jieba` |
| 建议 | 输入时显示候选 |
| 高亮 | 结果中标记匹配词 |
| 分享 | 可复制搜索结果链接 |
| 快捷键 | `Ctrl/Cmd + K` 或 `/` 聚焦搜索 |

## 插件

| 插件 | 作用 |
| --- | --- |
| `search` | 全文搜索与中文分词 |
| `statistics` | 页面与全文字数、代码行、阅读时间 |

插件通过 `mkdocs.yml` 配置。增加新插件时，需要同步更新 CI 安装步骤，否则部署环境会因缺少依赖而失败。

## 构建与发布

```bash title="本地构建"
cd ylcs-docs
mkdocs build --strict
```

```bash title="本地预览"
cd ylcs-docs
mkdocs serve
```

CI 的文档工作流按顺序：

1. 生成 Gallery 与 ModernScreen 的 Web 示例；
2. 生成 Dokka API；
3. 下载生成产物到 `docs/overrides`；
4. 安装 MkDocs 主题、统计与中文分词依赖；
5. 构建并发布站点。

文档工作流不负责编译应用或验证所有平台；生成页面失败时，指南构建不能替代应用构建验证。

## 写作约定

### 语气与结构

- 使用正式、严谨的说明语气；
- 先解释边界，再给示例；
- 每个代码块尽量带标题，说明文件或用途；
- 重要前提使用提示、警告或危险标记；
- 不使用无法验证的性能数据或虚构引用。

### 版本与数字

- 不记录易过期的依赖版本、SDK 版本或框架版本；
- 版本信息指向 `libs.versions.toml` 与 `settings.gradle.kts`；
- 必要的协议常量（如路由、字段名）应说明其来源；
- 不把“当前版本”写进标题或正文。

### 示例

- 使用虚构数据与占位符；
- 不包含 Token、密码、真实用户数据或本地绝对路径；
- 代码示例应能说明一个明确行为；
- 大段源码应标注来源文件，而不是假装是完整实现。

### 标记

| 场景 | 写法 |
| --- | --- |
| 前提与要点 | `!!! abstract` |
| 实践建议 | `!!! tip` |
| 边界与约束 | `!!! warning` |
| 危险操作 | `!!! danger` |
| 可展开说明 | `??? note` |
| 重点文本 | `==高亮==` |
| 行内标记 | `<span class="rc-chip">标签</span>` |

## 新增页面

- [ ] 在 `mkdocs.yml` 的 `nav` 中注册；
- [ ] 使用唯一的一级标题；
- [ ] 添加 `description` 元数据；
- [ ] 检查内部链接与图片路径；
- [ ] 运行 `mkdocs build --strict`；
- [ ] 在桌面与移动宽度下检查布局；
- [ ] 确认没有版本号、令牌或真实用户数据；
- [ ] 同步更新相关章节的导航卡片。

## 生成页面

| 页面 | 生成任务 | 输出 |
| --- | --- | --- |
| UI Gallery | Gallery Web 发布任务 | `overrides/gallery` |
| Modern Navigation | 导航示例 Web 发布任务 | `overrides/modern-screen` |
| API | Dokka | `overrides/dokka` |

生成页面与手写指南使用同一版本源码。文档站点中的入口只是链接，不复制生成内容。

## 维护边界

!!! warning "不要编辑生成目录"
    `docs/overrides` 中的页面属于构建产物。修改生成行为应修改源码、模板或构建脚本；修改手写内容应编辑 Markdown。

主题文件位于 `theme`，不会被生成流程覆盖。样式与脚本位于 `docs/assets`，可以随指南一起维护。

## 下一步

返回[生态入口](index.md)，或回到[首页](../index.md)重新选择阅读路径。
