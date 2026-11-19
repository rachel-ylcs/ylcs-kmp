---
description: 深入理解 Rachel 的字节读写、校验、压缩、加密、全文检索与解析器约束。
---

# 数据处理

<p class="rc-lead">数据处理模块的共同主题是“边界”：字节索引的边界、解压大小的边界、解析深度的边界、检索结果的顺序边界。理解这些边界，比记住 API 更重要。</p>

## 字节缓冲

`ByteArrayIO` 提供整数、字符串与大小端读写：

```kotlin title="共享底层数组"
val raw = ByteArray(4)
val wrapped = ByteArrayIO(raw)

wrapped.writeInt(0, 0x01020304)
assertContentEquals([1, 2, 3, 4], raw)

val created = ByteArrayIO(size = 2)
assertContentEquals([0, 0], created.bytes)
```

| 行为 | 语义 |
| --- | --- |
| `ByteArrayIO(raw)` | 不复制输入，写入会修改原数组 |
| `ByteArrayIO(size = n)` | 新建零初始化缓冲区 |
| 默认整数 | 大端 |
| 显式 `Endian.LITTLE` | 小端 |
| 越界/负长度 | 失败 |

读写索引与长度必须处于缓冲区范围内。需要独立副本时显式复制，不要假设构造函数会隔离输入。

## 校验值

`CRC32` 与 `Adler32` 用于校验值计算，不提供来源认证。校验值可以检测传输或存储中的意外变化，但不能抵抗主动篡改。

| 能力 | 适用 |
| --- | --- |
| CRC32 | 快速完整性校验 |
| Adler32 | 轻量校验 |
| SHA2 / SHA3 | 密码学摘要 |
| SM3 | 国密摘要 |
| XXHash64 | 非密码学高速散列 |

## 压缩

`CompressionCodec` 提供 `compress`、`decompress`、`maxCompressedSize`，以及写入调用方数组的 `compressPlaceable`、`decompressPlaceable`。

```kotlin title="Gzip 往返"
val codec = Gzip()
val source = "Hello, compression!".encodeToByteArray()
val encoded = codec.compress(source)
assertContentEquals(source, codec.decompress(encoded))
```

| 约束 | 说明 |
| --- | --- |
| 解压上限 | 默认存在单次解压上限，可按 codec 配置 |
| 输入输出 | `Placeable` 要求不是同一数组 |
| 返回值 | 返回实际写入长度，未使用区域不变 |
| 失败 | 已写入区域可能被修改 |
| 容量不足 | 通过压缩异常报告 |
| 损坏/截断 | 通过压缩异常报告 |

!!! warning "压缩不是加密"
    压缩只减少体积，不隐藏内容，也不提供完整性或来源认证。需要保密时使用加密，需要完整性时使用摘要或签名。

LZ4、Snappy 等格式必须与对应 codec 匹配。不能根据文件扩展名自动推断格式，也不要把一个 codec 的输出交给另一个 codec 解压。

## 摘要与加密

```kotlin title="摘要"
val sha2 = SHA2()
val hex = sha2.encodeToHex("abc")
```

`SHA2.BitType`、`SHA3.BitType` 选择摘要位数，`encodeToHex` 支持大小写格式。模块还包含 MD5、SM3、XXHash64 与对称加密实现。

| 选择 | 注意 |
| --- | --- |
| SHA2 / SHA3 | 通用密码学摘要 |
| SM3 | 国密场景 |
| MD5 | 仅兼容旧协议，不用于密码存储 |
| XXHash64 | 非密码学高速散列 |
| 对称加密 | 密钥、模式、填充由协议定义 |

!!! danger "安全边界"
    编解码往返测试不证明协议安全。密码存储应使用专门的慢哈希与随机盐；密钥管理、随机数来源、重放保护与认证加密需要单独设计。

## 全文检索

`FullTextSearch<T>` 接收文档集合与字段提取器。默认字符串查询为忽略大小写的子串匹配；另有 `Exact`、`WholeWord`、`Prefix` 与 `Fuzzy` 模式，以及独立的 Regex 重载。

```kotlin title="检索"
val index = indexOf(
    ["Input", "PasswordInput InputState"],
    ["Button", "IconButton"],
    ["Other"],
)

assertEquals([0], index.search("input").ids())
assertEquals([0], index.search("InputState").ids())
assertEquals([1], index.search("Button").ids())
assertEquals([], index.search("missing").ids())
```

| 行为 | 说明 |
| --- | --- |
| 索引 | 内存结构，不包含数据库查询、持久索引或权限过滤 |
| 字段复制 | 构建时复制字段文本，并保留原 item 引用 |
| 修改 item | 不会自动更新索引 |
| 结果顺序 | 按输入文档顺序返回 |
| 多次命中 | 一个文档只返回一次 |
| `limit` | 在输入顺序上截取 |
| 相关度排序 | 不提供 |
| 空字符串 | 返回全部文档 |
| trim | 不自动执行 |
| 模糊距离 | 以 UTF-16 Char 为单位 |
| 交换相邻字符 | 按两次编辑计算 |

普通字符串不会解释正则元字符。子串要求命中同一字段中的连续内容，不跨字段拼接。分词由 `SearchTokenizer` 控制；自定义别名对 WholeWord、Prefix、Fuzzy 的影响与对 Contains / Exact 的影响不同。

## 命令行参数

`Arguments` 保留键名前缀与大小写，支持引号、转义、空标志与重复值：

```kotlin title="解析"
val args = Arguments("enabled a=123")
assertNull(args.executable)
assertSame(Argument.Empty, args["enabled"])
assertEquals(123, args["a"].Int)
```

| 边界 | 说明 |
| --- | --- |
| 可执行程序 | 只有第一项可能识别为 executable |
| `--version` | 不会自动改名为 `version` |
| `Argument.Empty` | 与空字符串不同 |
| 重复键 | 可形成 `Argument.Values` |

`ServerEngine` 使用自己的 `--key=value` 解析逻辑，没有直接采用此解析器。

## TOML 与 YAML

两者提供文档树、标量扩展、对象/数组 DSL 与写入器：

```kotlin title="TOML"
val document = toml {
    "name" with "ylcs"
    "version" with 1
    obj("server") {
        "enabled" with true
        "port" with 8080
    }
    arr("products") {
        obj { "name" with "one" }
        obj { "name" with "two" }
    }
}
```

```kotlin title="YAML"
val root = buildYamlObject {
    "name" with "demo"
    "nullable" with null
    obj("server") { "port" with 8080 }
}
```

| 配置 | 默认行为 |
| --- | --- |
| TOML | 采用 TOML 1.1；深度与节点数有限制 |
| YAML | 拒绝重复键；支持 merge keys；别名与节点数有限制 |

需要不同限制时创建带配置的 `Toml` / `Yaml` 实例。写入器输出文档模型，不保证保留原始注释、空白或全部排版。

## Markdown

Markdown 模块处理语法树与文档文本。UI 富文本由独立的 `compose:components:rich-text` 提供，二者不是同一个渲染入口。

```kotlin title="构建"
val doc = markdown {
    h1 { +"Markdown" }
    blockquote { p("A quote") }
    table {
        header("Feature", "State")
        tr("DSL", "ready")
    }
}
assertEquals(doc, Markdown.parse(Markdown.encodeToString(doc)))
```

默认启用表格、任务列表与删除线，并限制深度、节点数与源文本长度。语法树可解析、可写入，不代表转换为 HTML 后已经过净化。

!!! warning "Markdown 转 HTML 需要内容策略"
    展示层仍需定义链接协议、图片加载、HTML 白名单与脚本处理策略。不要直接把不可信 Markdown 转换为未净化的 HTML。

## LRC

`Lrc.parse` 返回时间行与元数据，`plainText` 仅返回歌词文本，`toString()` 写回 LRC。解析得到时间行不等于建立播放时钟；逐字动画、偏移与播放状态属于业务层。

跨模块复用时传递解析后的数据模型，避免在重组中反复解析整份歌词。

## 性能与内存

| 场景 | 建议 |
| --- | --- |
| 大文件 | 使用流式读写，避免完整读入内存 |
| 解压 | 设置合理上限，防止压缩炸弹 |
| 检索 | 控制字段数量与文档规模，避免在 UI 线程构建 |
| 解析 | 限制深度、节点数与源文本长度 |
| 字节缓冲 | 明确是否需要复制，避免共享底层数组引发意外修改 |
| 摘要 | 大文件分块计算，避免一次性读取 |

## 安全清单

- [ ] 压缩数据有解压上限与格式校验；
- [ ] 不可信输入不直接进入正则或解析器深层递归；
- [ ] 用户输入不拼接为文件路径或 SQL 结构；
- [ ] 密码、令牌与密钥不写入日志；
- [ ] 摘要用于完整性时，明确是否还需要认证；
- [ ] Markdown、HTML 与富文本有内容处理策略。

## 下一步

继续阅读[平台与 FFI](platform.md)，了解系统集成与资源所有权。
