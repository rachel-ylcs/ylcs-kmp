---
description: foundation 模块提供的上下文、文件、网络、启动、压缩、加密、检索与解析能力。
---

# 基础能力模块

<p class="rc-lead"><code>ylcs-module/foundation</code> 在 <code>core</code> 之上提供可直接用于业务的基础设施。每个模块按需引用，目录分组本身不是汇总依赖。</p>

## 模块选择

| 模块 | 用途 | 主要包 |
| --- | --- | --- |
| `context` | 平台上下文与宿主能力提供者 | `love.yinlin.foundation` |
| `filesystem` | 文件、流、目录与隐式 URI | `love.yinlin.fs`、`love.yinlin.io` |
| `network` | Ktor Client、下载、文本 WebSocket | `love.yinlin.foundation` |
| `startup` | 服务工厂、依赖图、初始化与销毁 | `love.yinlin.foundation` |
| `compression` | 通用压缩与有界解压 | `love.yinlin.foundation.compression` |
| `cryptography` | 摘要、对称加密与编码 | `love.yinlin.foundation.cryptography` |
| `fts` | 内存中的多字段检索 | `love.yinlin.foundation.fts` |
| `parser/arguments` | 命令行参数 | `love.yinlin.foundation.parser.arguments` |
| `parser/lrc` | 歌词与时间戳 | `love.yinlin.foundation.parser.lrc` |
| `parser/markdown`、`toml`、`yaml` | 文档模型、解析、写入与 DSL | `love.yinlin.foundation.parser.<格式>` |
| `kv` | 跨平台键值存储 | `love.yinlin.kv` |
| `picker` | 系统文件与媒体选择器 | `love.yinlin.picker` |
| `exception` | 未处理异常收集 | `love.yinlin.foundation.exception` |

## 上下文与文件

`PlatformContext` 表示平台基础上下文；`PlatformContextProvider` 在运行期提供宿主能力。Compose 层通过 `LocalPlatformContext` 读取提供者。

```kotlin title="目录"
val dataPath = PlatformFileSystem.dataPath(rawContext, "example")
val cachePath = PlatformFileSystem.cachePath(rawContext, "example")
```

数据目录保存需要保留的内容，缓存目录保存可重建数据。安装目录或可执行文件位置不是默认可写目录。文件 API 包含元数据、遍历、创建、删除、复制、移动以及字节与文本读写；带 `Sync` 后缀的接口执行同步操作。

## 网络

`foundation:network` 封装 Ktor Client，适用于通用 HTTP、下载与文本 WebSocket：

```kotlin title="请求"
val profile = NetClient.Common.request<Profile, Profile>(
    onRequest = {
        method = HttpMethod.Get
        url = "https://example.com/profile.json"
        headers = NetHeader(HttpHeaders.Authorization to "Bearer $token")
    },
    onResponse = {
        require(status.isSuccess())
        body
    },
)
```

普通异常转换为 `null`，取消异常继续传播。响应会先读取完整字节，因此不适合不受控的大文件；大文件使用流式下载接口。访问 Rachel 共享契约后端时，应使用[客户端引擎](../client/client-engine.md)。

## 启动编排

`foundation:startup` 提供服务工厂、依赖图、初始化与销毁。应用级服务通过 ID 声明依赖，由 `StartupPool` 解析拓扑顺序。

```kotlin title="服务"
class SessionStartup(pool: StartupPool) : AsyncStartup(pool) {
    override suspend fun init() = restoreSession()

    class Factory : AsyncStartupFactory<SessionStartup>() {
        override val id: String = StartupID<SessionStartup>()
        override fun build(pool: StartupPool) = SessionStartup(pool)
    }
}
```

同步服务继承 `SyncStartup`，异步服务继承 `AsyncStartup`。完整规则见[入口与生命周期](../ui/lifecycle.md)。

## 压缩

`CompressionCodec` 提供 `compress`、`decompress`、`maxCompressedSize`，以及写入调用方数组的 `compressPlaceable`、`decompressPlaceable`。实现包括 RLE、LZ4、Snappy、Deflate、Zlib 与 Gzip。

```kotlin title="Gzip 往返"
val codec = Gzip()
val source = "Hello, compression!".encodeToByteArray()
val encoded = codec.compress(source)
assertContentEquals(source, codec.decompress(encoded))
```

| 约束 | 说明 |
| --- | --- |
| 默认解压上限 | 单次解压存在上限，可按 codec 配置 |
| `Placeable` | 输入与输出不能是同一数组，返回实际写入长度 |
| 失败语义 | 输出容量不足、超过上限、损坏或截断分别报告 |
| 格式匹配 | LZ4、Snappy 等必须与对应 codec 约定匹配 |

压缩成功不代表数据经过认证。需要完整性或来源验证时，应使用摘要或签名，而不是依赖压缩格式。

## 摘要与加密

```kotlin title="SHA-256"
val sha2 = SHA2()
val hex = sha2.encodeToHex("abc")
```

模块还提供 SHA3、MD5、SM3、XXHash64 与对称加密实现。选择摘要位数、密钥、模式与填充属于协议设计，不能由库的默认值替代。

!!! danger "哈希不等于密码存储"
    MD5 或非密码学散列不应被当作密码存储或来源认证方案。密码存储应使用专门的慢哈希与随机盐。

## 全文检索

`FullTextSearch<T>` 接收文档集合与字段提取器。默认字符串查询为忽略大小写的子串匹配；另有 `Exact`、`WholeWord`、`Prefix` 与 `Fuzzy` 模式。

```kotlin title="检索"
val index = indexOf(
    ["Input", "PasswordInput InputState"],
    ["Button", "IconButton"],
    ["Other"],
)

assertEquals([0], index.search("input").ids())
assertEquals([1], index.search("Button").ids())
```

| 特性 | 说明 |
| --- | --- |
| 索引 | 内存结构，不包含持久索引或权限过滤 |
| 结果顺序 | 按输入文档顺序返回，一个文档只返回一次 |
| 模糊距离 | 以 UTF-16 Char 为单位 |
| 空字符串 | 返回全部文档 |
| 修改文档 | 不会自动更新索引 |

## 解析器

### 命令行参数

`Arguments` 保留键名前缀与大小写，支持引号、转义、空标志与重复值：

```kotlin title="Arguments"
val args = Arguments("enabled a=123")
assertNull(args.executable)
assertSame(Argument.Empty, args["enabled"])
assertEquals(123, args["a"].Int)
```

`--version` 不会自动改名为 `version`。`Argument.Empty` 与空字符串不同，重复键形成 `Argument.Values`。

### LRC

`Lrc.parse` 返回时间行与元数据，`plainText` 返回歌词文本，`toString()` 写回 LRC。解析得到时间行不等于建立播放时钟；逐字动画属于业务层。

### TOML 与 YAML

两者提供文档树、标量扩展、对象/数组 DSL 与写入器：

```kotlin title="TOML DSL"
val document = toml {
    "name" with "ylcs"
    "version" with 1
    obj("server") {
        "enabled" with true
        "port" with 8080
    }
}
```

```kotlin title="YAML DSL"
val root = buildYamlObject {
    "name" with "demo"
    "nullable" with null
    obj("server") { "port" with 8080 }
}
```

写入器输出文档模型，不保证保留原始注释、空白或全部源文件排版。解析深度、节点上限、重复键与别名限制由配置控制。

### Markdown

Markdown 模块处理语法树与文档文本。UI 富文本由独立的 `compose:components:rich-text` 提供，二者不是同一个渲染入口。

```kotlin title="Markdown DSL"
val doc = markdown {
    h1 { +"Markdown" }
    blockquote { p("A quote") }
    table {
        header("Feature", "State")
        tr("DSL", "ready")
    }
}
```

语法树可解析、可写入，不代表 HTML 已经过净化。转换为 HTML 时仍需定义链接与内容处理策略。

## 存储、选择器与异常

| 能力 | 模块 | 说明 |
| --- | --- | --- |
| 键值存储 | `foundation:kv` | 显式注册上下文与存储目录；关闭后由创建者释放 |
| 可观察配置 | `compose:config` | 基于 KV 的持久配置 |
| 系统选择器 | `foundation:picker` | 文件、图片等平台选择能力 |
| 异常收集 | `foundation:exception` | 未处理异常与自定义记录 |

这些能力通常由应用级服务封装，避免在页面中直接管理全局生命周期。

## 边界与建议

- 解析器只保证语法与结构，不保证业务内容合法；
- 压缩、加密与摘要解决不同问题，不要互相替代；
- 检索是内存能力，不承担数据库查询与权限过滤；
- 解析大文件时应设置明确的深度、节点与长度上限；
- 原生资源、文件流与监听器按所有权释放。

## 下一步

进入[客户端交互](../client/index.md)，把这些基础能力组合成共享契约与网络调用。
