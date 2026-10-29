# 基础能力模块

`ylcs-module/foundation` 在 `core` 之上提供上下文、文件系统、网络、启动编排，以及不依赖 Compose 的数据处理能力。模块按需引用；目录分组本身不是一个汇总依赖。

## 模块选择

| 模块 | 用途 | 主要包 |
| --- | --- | --- |
| `context` | 平台基础上下文、Activity/控制器/窗口能力提供者 | `love.yinlin.foundation` |
| `filesystem` | 文件、流、平台目录、隐式 URI | `love.yinlin.fs`、`love.yinlin.io` |
| `network` | Ktor Client、下载、文本 WebSocket | `love.yinlin.foundation` |
| `startup` | 服务工厂、依赖图、初始化与销毁 | `love.yinlin.foundation` |
| `compression` | 通用压缩与有界解压 | `love.yinlin.foundation.compression` |
| `cryptography` | 摘要、对称加密与编码 | `love.yinlin.foundation.cryptography` |
| `fts` | 内存中的多字段检索 | `love.yinlin.foundation.fts` |
| `parser/arguments` | 命令行参数 | `love.yinlin.foundation.parser.arguments` |
| `parser/lrc` | 歌词与时间戳 | `love.yinlin.foundation.parser.lrc` |
| `parser/markdown`、`toml`、`yaml` | 文档模型、解析、写入与 DSL | `love.yinlin.foundation.parser.<格式>` |

当前这些模块均配置 Android、iOS、Desktop JVM、WasmJS、JS 和三个桌面 Native 目标。具体系统操作仍受平台能力限制。上下文和文件访问见[平台上下文](../2_ui/context.md)，网络见[通用网络](../2_ui/network.md)，初始化编排见[应用服务](../2_ui/service.md)。

## 压缩

`CompressionCodec` 提供 `compress`、`decompress`、`maxCompressedSize`，以及写入调用方数组的 `compressPlaceable`、`decompressPlaceable`。实现包括 `RLE`、`LZ4`、`Snappy`、`Deflate`、`Zlib` 和 `Gzip`。

下面将 [TestCompressionCodec.roundTripCommonPayloads](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/compression/src/commonTest/kotlin/love/yinlin/foundation/compression/TestCompressionCodec.kt) 的通用往返调用收敛为单个 Gzip 实例：

```kotlin
import love.yinlin.foundation.compression.Gzip
import kotlin.test.assertContentEquals

val codec = Gzip()
val source = "Hello, compression!".encodeToByteArray()
val encoded = codec.compress(source)
assertContentEquals(source, codec.decompress(encoded))
```

默认单次解压上限为 64 MiB，可通过具体 codec 的构造参数配置。`Placeable` 接口要求输入、输出不是同一数组，返回实际写入长度，未使用区域保持不变；失败时已写入区域可能被修改。输出容量不足、超过解压上限、损坏或截断的数据分别通过压缩异常类型报告。LZ4、Snappy 等数据必须与对应 codec 的格式约定匹配，不能根据文件扩展名自动推断。

## 摘要与加密

[TestDigest.testSHA2](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/cryptography/src/commonTest/kotlin/love/yinlin/foundation/cryptography/TestDigest.kt) 使用固定向量验证默认 SHA-256：

```kotlin
val sha2 = SHA2()
assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha2.encodeToHex("abc"))
```

`SHA2.BitType`、`SHA3.BitType` 选择摘要位数，`encodeToHex` 支持大小写格式。模块还包含 MD5、SM3、XXHash64 及对称加密实现；具体调用见同模块的 `TestDigest` 与 `TestCipher`。摘要、校验值、加密密文具有不同语义，MD5 或非密码学散列不应被当作密码存储或来源认证方案。密钥、模式、填充与协议的选择由使用方定义，编解码往返测试不证明某种业务协议的安全性。

## 全文检索

`FullTextSearch<T>` 接收文档集合和字段提取器。默认字符串查询为忽略大小写的子串匹配；另有 `Exact`、`WholeWord`、`Prefix` 与 `Fuzzy` 模式和独立的 Regex 重载。`prepare` 可以提前建立所需索引。

以下片段来自 [FullTextSearchTest](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/fts/src/commonTest/kotlin/love/yinlin/foundation/fts/FullTextSearchTest.kt)，其中 `indexOf`、`ids` 是同文件的测试辅助函数：

```kotlin
val index = indexOf(["Input", "PasswordInput InputState"], ["Button", "IconButton"], ["Other"])
assertEquals([0], index.search("input").ids())
assertEquals([0], index.search("InputState").ids())
assertEquals([1], index.search("Button").ids())
assertEquals([], index.search("missing").ids())
```

普通字符串不会解释正则元字符。子串要求命中同一字段中的连续内容，不跨字段拼接。分词由 `SearchTokenizer` 控制；自定义别名对 WholeWord、Prefix、Fuzzy 的影响与对原文 Contains/Exact 的影响不同，不能将全部模式视为同一套匹配规则。索引是内存检索结构，不包含数据库查询、持久索引或业务权限过滤。

索引构建时复制字段文本并保留原 item 引用，修改 item 不会自动更新索引。结果按输入文档顺序返回，一个文档命中多次也只返回一次，`limit` 在该顺序上截取；没有相关度排序。空字符串返回全部文档，其余查询不自动 trim。模糊距离以 UTF-16 Char 为单位，交换相邻字符按两次编辑计算。

## 命令行参数

`Arguments` 可以接收整段命令行或字符串数组，保留键名前缀及大小写，支持引号、转义、空标志与重复值。以下调用来自 [TestArguments](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/parser/arguments/src/commonTest/kotlin/love/yinlin/foundation/parser/arguments/TestArguments.kt)：

```kotlin
val args = Arguments("enabled a=123")
assertNull(args.executable)
assertSame(Argument.Empty, args["enabled"])
assertEquals(123, args["a"].Int)
```

该片段组合了 `parametersOnly` 中的输入与同文件中的类型化断言。只有第一项可能识别为可执行程序；`--version` 不会自动改名为 `version`。`Argument.Empty` 与值为空字符串不同，重复键可形成 `Argument.Values`。`ServerEngine` 当前使用自己的 `--key=value` 解析逻辑，没有直接采用此解析器。

## TOML 与 YAML

二者提供文档树、标量扩展、对象/数组 DSL 与写入器。下面来自 [TestTomlDsl.dslAndNodeAccess](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/parser/toml/src/commonTest/kotlin/love/yinlin/foundation/parser/toml/TestTomlDsl.kt)：

```kotlin
val document = toml {
    "name" with "ylcs"
    "version" with 1
    "created" with TomlPrimitive.dateTime("2026-10-05")
    obj("server") {
        "enabled" with true
        "port" with 8080
    }
    arr("products") {
        obj { "name" with "one" }
        obj { "name" with "two" }
    }
}
assertEquals(8080, document.root.obj("server").getValue("port").Int)
assertEquals("two", document.root.arr("products")[1].Object.getValue("name").String)
assertEquals(document, Toml.parse(Toml.encodeToString(document)))
```

YAML 用法见 [TestYamlDsl](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/parser/yaml/src/commonTest/kotlin/love/yinlin/foundation/parser/yaml/TestYamlDsl.kt)：

```kotlin
val root = buildYamlObject {
    "name" with "demo"
    "nullable" with null
    obj("server") { "port" with 8080 }
}
assertEquals(root, Yaml.parse(Yaml.encodeToString(root)).root)
```

该片段保留测试中的构建、空值与往返调用，减少了演示字段。`TomlConfiguration` 默认采用 TOML 1.1，深度为 128、节点上限为 1,000,000；`YamlConfiguration` 默认拒绝重复键，支持 merge keys，深度为 128、别名上限为 1,000、节点上限为 1,000,000。需要不同限制时创建带配置的 `Toml` / `Yaml` 实例。写入器输出文档模型，不应假定保留原始注释、空白或全部源文件排版。

## Markdown

Markdown 模块处理语法树与文档文本，UI 富文本由独立的 `compose:components:rich-text` 提供，二者不是同一个渲染入口。

下面从 [TestMarkdownDsl](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/parser/markdown/src/commonTest/kotlin/love/yinlin/foundation/parser/markdown/TestMarkdownDsl.kt) 的构建方式摘取标题、引用和表格：

```kotlin
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

默认启用表格、任务列表与删除线，默认深度为 128、节点上限为 1,000,000、源文本长度上限为 16,777,216 个 UTF-16 代码单元。`MarkdownConfiguration` 可以调整这些限制。语法树可解析、可写入，不代表网页 HTML 已经过净化；若后续转换为 HTML，展示层仍需定义链接与内容处理策略。

## LRC

`Lrc.parse` 返回时间行与元数据，`plainText` 仅返回歌词文本，`toString()` 写回 LRC 文本。[LrcTest](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-module/foundation/parser/lrc/src/commonTest/kotlin/love/yinlin/foundation/parser/lrc/LrcTest.kt) 验证真实多行样本、换行、时间戳、元数据与序列化往返。

歌词播放器的当前位置映射与动态歌词动画属于业务层；解析得到时间行不等同于已经建立播放时钟或逐字渲染。需要跨模块复用时传递解析后的数据模型，避免在重组中反复解析整份歌词。
