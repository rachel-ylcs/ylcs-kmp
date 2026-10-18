# Music / MOD v5 文件格式

MOD 是 `ylcs-app/mod` 实现的音乐资源容器。当前 `ModFactory.VERSION` 为 `5`，归档扩展名为 `.rachel`，工作目录资源扩展名为 `.rachelres`。

## 工作目录与分发形式

```text
music/
└─ demo001/
   ├─ config.rachelres
   ├─ audio.rachelres
   ├─ record.rachelres
   ├─ background.rachelres
   ├─ lyrics.rachelres
   ├─ animation.rachelres
   ├─ video.rachelres
   ├─ rhyme.rachelres
   └─ accompaniment.rachelres
```

单项资源仍保存原始 JSON、音频、图片、文本或视频字节。扩展名表示资源槽位，归档格式本身不使用 ZIP。

| 资源 type | 内容约定 | 基础资源 |
| --- | --- | --- |
| `config` | `MusicInfo` JSON | 是 |
| `audio` | 音源 | 是 |
| `record` | 封面 | 是 |
| `background` | 背景图片 | 是 |
| `lyrics` | 逐行歌词 | 是 |
| `animation` | 动态背景 | 否 |
| `video` | 视频 | 否 |
| `rhyme` | 动态歌词配置 | 否 |
| `accompaniment` | 伴奏音频 | 否 |

`ModResourceType.BASE` 为前五项，`ALL` 为全部资源，`DEPLOYMENT` 为除音源外的全部资源。媒体格式、尺寸和编码约束属于应用约定，协议 reader 不执行完整媒体内容校验。

## 配置 JSON

`config.rachelres` 对应 `MusicInfo`。以下为虚构示例：

```json
{
  "version": "1.0",
  "author": "示例作者",
  "id": "demo001",
  "name": "示例曲目",
  "singer": "示例演唱者",
  "lyricist": "示例作词者",
  "composer": "示例作曲者",
  "album": "示例专辑",
  "chorus": [60000, 120000]
}
```

`chorus` 为可空毫秒列表，`modification` 为带默认值的 `@Transient` 运行时字段，不进入 JSON。打包时媒体 ID 从配置内容读取，工作目录名称应与其保持一致。

归档级 `ModInfo` 当前仅包含 `author`，默认值为“无名”，与单首媒体配置的作者字段分别保存。

## 二进制布局

整数为 4 字节大端 Int，字符串为字节长度前缀与 UTF-8 内容：

```text
Archive
├─ magic: Int = 1211
├─ version: Int = 5
├─ mediaCount: Int
├─ info: LengthString          # ModInfo JSON
└─ Media × mediaCount
   ├─ id: LengthString
   ├─ resourceCount: Int
   └─ Resource × resourceCount
      ├─ type: LengthString
      ├─ dataLength: Int
      └─ payload
```

`dataLength` 表示真实资源长度。每完整读取 65,536 个真实字节后写入一个取值为 `0..126` 的随机填充字节，剩余不足一块的数据直接写入：

```text
storedLength = dataLength + floor(dataLength / 65536)
```

长度恰好为完整块整数倍时，最后一块后仍有填充。填充不提供加密或完整性校验，同一输入多次打包可能得到不同二进制内容。

## 打包

```kotlin
output.write { sink ->
    ModFactory.Merge(
        mediaPaths = [songFolder],
        sink = sink,
        info = ModInfo(author = "示例作者")
    ).process(filters = ModResourceType.ALL) { index, total, name ->
        println("$index / $total: $name")
    }
}
```

`Merge` 读取每个媒体目录中的配置，收集扩展名正确、type 已知且符合 filters 的资源。它不强制基础资源完整。

资源长度转换为 Int 后需大于零，调用方应提前限制单资源大小，避免超出 Int 表示范围。进度 index 从零开始，回调通过 `Coroutines.main` 交付。

## 预览与解包

```kotlin
val preview = input.read { source ->
    ModFactory.Preview(source).process()
}

val released = input.read { source ->
    ModFactory.Release(source, libraryRoot).process { index, total, id ->
        println("$index / $total: $id")
    }
}
```

`Preview` 返回归档元数据、媒体 ID、配置及资源类型和长度，非配置负载按包含填充的长度跳过。`Release` 按完整块读取资源并丢弃填充，将文件写入 `<libraryRoot>/<id>/<type>.rachelres`。

当前 `Preview` 的配置分支直接读取 `dataLength` 字节并解析 JSON，未像解包分支逐块跳过填充。因此达到 65,536 字节的配置可能预览失败或导致后续读取错位。现有格式生产流程应保持配置小于该边界；不能将预览器视为任意大小配置的完整校验器。

## 兼容性与导入边界

reader 要求 magic 和版本精确匹配，媒体数与长度有部分合法性检查，但不统一限制字符串长度、媒体数量、资源数量和总解包大小，也未验证重复 ID、资源覆盖或路径归属。

`Release` 直接使用归档 ID 构造目录，目标已有内容可能被覆盖。导入方应限制安全 ID、文件总量及资源大小，并先在隔离临时目录完成验证，再纳入正式曲库。预览结果不代替媒体内容识别，也不提供数字签名或来源认证。

当前 reader 不包含旧版本自动迁移。升级版本号时应同步提供兼容 reader 或转换流程。

## MOD Manager

```powershell
.\gradlew.bat :ylcs-app:mod-manager:modManagerRunDebug
.\gradlew.bat :ylcs-app:mod-manager:modManagerPublish
```

工具支持预览、解包、合并或分别打包、资源筛选和部署。源目录发现流程检查 config、audio、record、background 与 lyrics 等基础资源。

部署流程为每首媒体生成只包含 BASE 的 `base.rachel`，同时复制 DEPLOYMENT 资源作为旁路文件。该目录组织是工具部署策略，不增加 v5 归档字段。

## 源码入口

- `ylcs-app/mod/src/commonMain/kotlin/love/yinlin/mod/ModFactory.kt`
- `ylcs-app/mod/src/commonMain/kotlin/love/yinlin/data/mod/ModResourceType.kt`
- `ylcs-app/mod/src/commonMain/kotlin/love/yinlin/data/music/MusicInfo.kt`
- `ylcs-app/mod-manager/src/desktopMain/kotlin/love/yinlin/screen/ScreenMain.kt`
