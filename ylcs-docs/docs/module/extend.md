---
description: 新增模块、补充测试、生成文档并交付框架变更的完整检查清单。
---

# 扩展与验证

<p class="rc-lead">新增一个框架模块不是“创建一个目录”。它同时改变模块图、依赖方向、源集关系、测试覆盖、发布坐标与文档边界。下面是一份可以逐项执行的清单。</p>

## 新增模块

### 1. 注册项目

```kotlin title="settings.gradle.kts"
modules {
    module("ylcs-module") {
        module("foundation") {
            module("network", "filesystem")
            module("myfeature")
        }
    }
}
```

未注册的目录不会被 Gradle 纳入构建，也不会参与发布坐标生成。

### 2. 创建构建脚本

```kotlin title="build.gradle.kts"
import love.yinlin.gradle.*

plugins {
    install(
        libs.plugins.rachel,
        libs.plugins.kotlinMultiplatform,
    )
}

rachel {
    multiplatform {
        sources {
            commonMain {
                lib(projects.ylcsModule.core)
            }

            androidMain(commonMain)
            iosMain(commonMain)
            desktopMain(commonMain)
            webMain(commonMain)
            jsMain(webMain)
            wasmJsMain(webMain)
        }
    }
}
```

只启用实际需要的目标。为目标安装工具链不等于功能已经实现。

### 3. 建立源集

- 共享逻辑放在最上层可用源集；
- 平台实现放在对应源集；
- `expect/actual` 保持最小；
- 平台专属依赖不要进入 `commonMain`。

### 4. 声明依赖边界

- 公开 API 需要的依赖放在 `ExportLib` 之后；
- 内部实现依赖放在 `ExportLib` 之前；
- 使用 `projects.*` 引用仓库模块；
- 外部版本统一放在版本目录。

### 5. 处理资源与原生库

- 资源路径与分发目录一致；
- 原生库由构建任务生成，不提交产物；
- 平台图标、权限与签名配置由应用负责；
- 动态库加载失败有明确降级或错误。

## 测试

### 放置位置

| 测试类型 | 源集 |
| --- | --- |
| 共享行为 | `commonTest` |
| 平台行为 | `androidTest`、`iosTest`、`desktopTest` 等 |
| 协程 | `kotlinx.coroutines.test` |
| 原生库 | 对应平台测试或独立验证工程 |

默认使用 `kotlin.test`。需要网络、账号、数据库或本地原生库的集成测试应与纯单元测试分开考虑，避免把环境依赖混入所有平台。

### 测试原则

- 测试可重复，不依赖当前时间、随机顺序或外部服务；
- 覆盖行为与边界，而不是复述实现；
- 对资源释放、取消传播、异常映射与状态恢复写明确断言；
- 不通过删除测试、降低断言、禁用平台或扩大忽略范围来掩盖失败。

```kotlin title="共享测试"
fun parseEmptyInput() {
    assertEquals([], parseItems(""))
}
```

### 聚合任务

`ylcs-module/build-tool` 聚合现有测试任务，包括 Android、iOS、Desktop、Web 与桌面 Native 测试。没有对应目标的模块不会自动获得测试覆盖。

## 文档

| 内容 | 位置 |
| --- | --- |
| 手写指南 | `ylcs-docs/docs/*.md` |
| 图片与样式 | `ylcs-docs/docs/assets` |
| 主题覆盖 | `ylcs-docs/theme` |
| Gallery 生成页 | `ylcs-docs/docs/overrides/gallery` |
| 导航示例生成页 | `ylcs-docs/docs/overrides/modern-screen` |
| API 生成页 | `ylcs-docs/docs/overrides/dokka` |

手写指南与生成页面分开维护。生成目录属于构建产物，不直接编辑。

### 本指南的本地预览

```bash title="预览"
cd ylcs-docs
mkdocs serve
```

构建前确认 `docs/overrides` 存在。指南构建不会自动生成 Gallery、ModernScreen 或 Dokka 页面。

## 发布

发布前检查：

- [ ] 模块已注册并选择合适的模板；
- [ ] 目标平台与依赖导出正确；
- [ ] 测试覆盖共享行为与平台边界；
- [ ] 坐标、许可证与签名配置正确；
- [ ] 产物包含资源、原生库与运行数据；
- [ ] 文档与示例页面来自当前源码；
- [ ] 没有提交本地路径、Token 或用户数据。

发布流程与 CI 产物见[产物与发布](../build/artifacts.md)。

## 变更审查

### 兼容性

- 公共 API 是否发生破坏性变化？
- 序列化字段、默认值与资源路径是否兼容旧数据？
- 源集继承变化是否影响消费者？
- 依赖导出是否扩大或缩小？

### 资源与生命周期

- 新增长期任务是否有明确所有者？
- 原生句柄、监听器与流是否释放？
- 取消是否继续传播？
- 失败路径是否保持状态一致？

### 文档

- 新能力是否在对应章节说明？
- 示例是否使用虚构数据？
- 是否避免记录易过期的版本号？
- 是否保留 Gallery、导航与 API 入口？

## 常见反模式

| 反模式 | 问题 |
| --- | --- |
| 为单个调用引入通用框架 | 增加抽象成本，收益不足 |
| 把平台依赖放入 `commonMain` | 其他平台无法解析 |
| 用 `null` 折叠所有失败 | 丢失鉴权、超时与协议错误 |
| 把生成目录纳入手写修改 | 下次生成会被覆盖 |
| 用链接产物作为分发包 | 缺少运行资源 |
| 在页面中直接管理全局资源 | 生命周期不清，易泄漏 |

## 下一步

回到[快速开始](../start/index.md)验证一个完整应用，或进入[附录](../appendix/index.md)查看生态入口与文档工程说明。
