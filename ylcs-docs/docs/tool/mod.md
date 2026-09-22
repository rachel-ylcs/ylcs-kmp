# Music / MOD v5 协议

MOD 是银临茶舍使用的音乐资源容器。当前 `ModFactory.VERSION = 5`，文件扩展名为 `.rachel`；工作目录中的单项资源扩展名为 `.rachelres`。格式由 `ylcs-app/mod` 实现，不是 ZIP，也没有使用媒体文件本身的扩展名。

## 三种形态

### 工作/解包目录

每首媒体一个以 ID 命名的目录：

```text
music/
├─ 1077/
│  ├─ config.rachelres
│  ├─ audio.rachelres
│  ├─ record.rachelres
│  ├─ background.rachelres
│  ├─ lyrics.rachelres
│  ├─ animation.rachelres
│  ├─ video.rachelres
│  ├─ rhyme.rachelres
│  └─ accompaniment.rachelres
└─ 2013/
   └─ ...
```

`.rachelres` 只是资源槽位名，内容仍是原始 JSON、音频、图片或视频字节。

### 分发归档

一个 `.rachel` 可以包含一首或多首媒体及筛选后的资源。`ModFactory.Merge` 负责写入，`Preview` 只查看元数据与清单，`Release` 解包到工作目录。

### 服务端部署目录

MOD Manager 的“部署”会为每首媒体生成 `base.rachel`（只打包基础资源），同时把除音源外的资源作为 `.rachelres` 旁路文件复制到部署目录。这样客户端可先读取封面/配置，真正下载歌曲时再取得基础归档。它是当前工具的部署策略，不是 v5 二进制本身的额外结构。

## 资源类型

| type | 文件名 | `base` | 内容约定 |
| --- | --- | --- | --- |
| `config` | `config.rachelres` | 是 | UTF-8 `MusicInfo` JSON |
| `audio` | `audio.rachelres` | 是 | 原始音频字节 |
| `record` | `record.rachelres` | 是 | 方形封面图片 |
| `background` | `background.rachelres` | 是 | 9:16 背景图片 |
| `lyrics` | `lyrics.rachelres` | 是 | LRC 逐行歌词文本 |
| `animation` | `animation.rachelres` | 否 | 动态背景资源 |
| `video` | `video.rachelres` | 否 | PV 视频 |
| `rhyme` | `rhyme.rachelres` | 否 | 动态歌词/琴韵数据 |
| `accompaniment` | `accompaniment.rachelres` | 否 | 伴奏音频 |

`ModResourceType.BASE` 是前五项；`ALL` 是全部；`DEPLOYMENT` 是除 `audio` 外的全部资源。协议实现只检查资源 type 与长度，不验证 MIME、分辨率或 codec。表中的媒体形态是应用约定，导入工具应实际解析后再接受。

## 配置 JSON

`config.rachelres` 对应：

```kotlin
@Serializable
data class MusicInfo(
    val version: String,
    val author: String,
    val id: String,
    val name: String,
    val singer: String,
    val lyricist: String,
    val composer: String,
    val album: String,
    val chorus: List<Long>?,
    @Transient val modification: Int = 0
)
```

示例：

```json
{
  "version": "1.0",
  "author": "官方",
  "id": "1054",
  "name": "棠梨煎雪",
  "singer": "银临",
  "lyricist": "商连",
  "composer": "银临",
  "album": "腐草为萤",
  "chorus": [71670, 161140, 210400]
}
```

`chorus` 单位为毫秒，可为 null。`modification` 不进入 JSON。打包时媒体 ID 从 config 内容读取，而不是直接使用目录名；两者应保持一致。应用创建页面要求 ID 只含字母或数字，这也是跨平台路径最安全的约束。

归档级 `ModInfo` 当前只有 `author`，默认值为“无名”，它与每首 `MusicInfo.author` 是不同层级。

## v5 二进制布局

所有 Int 为 4 字节大端序；字符串为 `Int byteLength + UTF-8 bytes`。

```text
Archive
├─ magic: Int = 1211
├─ version: Int = 5
├─ mediaCount: Int
├─ info: LengthString              # ModInfo JSON
└─ Media × mediaCount
   ├─ id: LengthString
   ├─ resourceCount: Int
   └─ Resource × resourceCount
      ├─ type: LengthString        # config/audio/...
      ├─ dataLength: Int           # 真实资源字节数，不含填充
      └─ chunkedData
```

资源负载以 65,536 字节为间隔插入填充：

```text
[最多 65536 个真实字节][1 个随机字节]
[最多 65536 个真实字节][1 个随机字节]
...
[不足 65536 的最后真实字节，不追加随机字节]
```

精确规则是：完整块数量 `times = dataLength / 65536`，每个完整块后写一个 `0..126` 的随机 byte；余数直接写入。因而负载占用为：

```text
storedLength = dataLength + floor(dataLength / 65536)
```

当长度恰好是 65,536 的整数倍时，最后一个完整块后仍有填充 byte。`Preview` 按同一公式跳过非 config 数据；`Release` 逐块复制并丢弃填充。

这些随机字节只是格式扰动，不是加密、水印或完整性保护。同一输入的两次归档会不同；资源遍历顺序也不应被当作稳定摘要来源。当前源码没有独立的规范摘要字段。

## API 用法

### 打包

```kotlin
output.write { sink ->
    ModFactory.Merge(
        mediaPaths = listOf(songFolder),
        sink = sink,
        info = ModInfo(author = "Alice")
    ).process(filters = ModResourceType.ALL) { index, total, name ->
        println("$index / $total: $name")
    }
}
```

资源长度必须大于 0，并被转换为 Int；单资源不应接近/超过 2 GiB。`Merge` 会读取 config 取得 ID，只收集扩展名为 `rachelres` 且 type 已知、位于 filters 中的文件。它不会强制 BASE 完整，调用方要先校验。

### 预览

```kotlin
val preview = input.read { source ->
    ModFactory.Preview(source).process()
}
```

预览验证 magic、精确版本、计数、资源 type/正长度，并完整解析 config；其他资源只记录类型和长度后跳过。它不会验证音频/图片内容，也没有内置总大小、媒体数或字符串长度上限。

### 解包

```kotlin
val released = input.read { source ->
    ModFactory.Release(source, libraryRoot).process { index, total, id ->
        println("$index / $total: $id")
    }
}
```

Release 为每个 ID 建目录，并把资源写为规范文件名。如果同名资源重复，后写内容可能覆盖前者；目标已有内容也可能被覆盖。应先释放到临时空目录，全部验证成功后再原子合并到正式曲库。

## 兼容性与安全

解析器要求 `version == 5`，没有内置 v1–v4 迁移。升级格式时应新增 reader 或先迁移旧文件，不能只增加 `VERSION`。

对不可信归档尤其注意：

- magic/version 不是签名，文件可被任意伪造；
- 长度、数量虽有基本正负检查，但没有合理上限；
- `Release` 直接用归档 ID 构造目标目录，reader 内部不净化路径；
- 没有摘要、校验和、压缩或加密；
- 损坏/恶意数据可能留下已创建的部分目录。

安全导入流程应先限制文件总大小，用 `Preview` 检查媒体数、ID 只含允许字符、资源集合和各长度，再重新打开同一个不可变文件释放到临时目录；解析真实媒体格式后才移动到曲库。失败时删除整个临时目录。

## MOD Manager

桌面工具提供预览、解包、合并/分别打包、资源过滤与部署：

```powershell
.\gradlew.bat :ylcs-app:mod-manager:modManagerRunDebug
```

发布任务：

```powershell
.\gradlew.bat :ylcs-app:mod-manager:modManagerPublish
```

工具认定可用源目录至少具有 config、audio、record、background、lyrics。协议层本身更宽松，所以自动化生产 MOD 时也应采用同一完整性检查，而不是只以 `Merge.process` 未抛错作为合格标准。
