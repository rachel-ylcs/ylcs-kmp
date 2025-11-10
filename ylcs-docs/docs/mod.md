# Music / MOD 协议

## MOD打包形式

必需资源打包成 `base.rachel`，其他资源文件存储于基础包同目录

## MOD解压形式

目录内以 ```{ModType}.rachelres```的形式存储各资源文件, 相同```{ID}```的资源归属同一目录树

一个简单的目录树示例如下:

```
- music
      - 1077
          - config.rachelres
          - audio.rachelres
          - video.rachelres
          - record.rachelres
          - background.rachelres
          - ...
      - 2013
          - config.rachelres
          - lyrics.rachelres
          - ...
```

## MOD资源分类

### ★ MOD配置

|    类型    | 是否必需 |  实体类型  | 备注 |
|:--------:|:----:|:------:|:--:|
| `config` |  √   | `json` |    |

媒体配置记录了一个媒体的元信息, 各字段与要求如下表所示:

|   字段名    |        类型        |  含义  | 非空 |           备注           |
|:--------:|:----------------:|:----:|:--:|:----------------------:|
| version  |      String      |  版本  | √  |                        |
|  author  |      String      |  作者  | √  |                        |
|    id    |      String      | 唯一ID | √  |                        |
|   name   |      String      |  歌名  | √  |                        |
|  singer  |      String      |  歌手  | √  |                        |
| lyricist |      String      |  作词  | √  |                        |
| composer |      String      |  作曲  | √  |                        |
|  album   |      String      |  专辑  | √  |                        |
|  chorus  | List&lt;Long&gt; | 副歌点  | ×  | 单位为毫秒，如果为null歌曲就不能快进副歌 |

一个简单的配置示例如下所示:

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

### ★ 音源

|   类型    | 是否必需 |   实体类型    | 备注 |
|:-------:|:----:|:---------:|:--:|
| `audio` |  √   | `audio/*` |    |

媒体的音频文件, 支持mp3, flac等格式

### ★ 封面

|    类型    | 是否必需 |  实体类型  | 备注 |
|:--------:|:----:|:------:|:--:|
| `record` |  √   | `webp` |    |

媒体的CD图片封面，分辨率512*512

### ★ 壁纸

|      类型      | 是否必需 |  实体类型  | 备注 |
|:------------:|:----:|:------:|:--:|
| `background` |  √   | `webp` |    |

媒体的静态背景图片, 分辨率1080*1920

### ★ 逐行歌词

|    类型    | 是否必需 | 实体类型  | 备注 |
|:--------:|:----:|:-----:|:--:|
| `lyrics` |  √   | `lrc` |    |

媒体的逐行歌词, 以lrc歌词形式存储

### ★ 动画

|     类型      | 是否必需 |  实体类型  | 备注 |
|:-----------:|:----:|:------:|:--:|
| `animation` |  ×   | `webp` |    |

媒体的动态背景, 分辨率1080*1920

### ★ 视频

|   类型    | 是否必需 | 实体类型  | 备注 |
|:-------:|:----:|:-----:|:--:|
| `video` |  ×   | `mp4` |    |

媒体的PV视频，H264，分辨率1920*1080，比特率2Mbps，帧率30，推荐大小在20-50M间

### ★ 琴韵

|   类型    | 是否必需 |  实体类型  | 备注 |
|:-------:|:----:|:------:|:--:|
| `rhyme` |  ×   | `json` |    |

琴韵小游戏的支持文件，以及逐字动态歌词的支持文件

## MOD二进制构成

```kotlin
// 带长度的UTF-8字符串
class LengthString(
    val length: Int,                // 长度           [4 Bytes]
    val content: String,            // 内容           [length]
)

// 带长度的二进制数据
class LengthBinary(
    val length: Int,                // 长度           [4 Bytes]
    val content: ByteArray,         // 内容           [length]
)

// MOD元数据
class ModMetadata(
    val magic: Int,                 // 魔数           [4 Bytes]
    val version: Int,               // MOD版本        [4 Bytes]
    val mediaNum: Int,              // 媒体数         [4 Bytes]
    val info: LengthString,         // Json信息      [*]
)

// MOD资源类型(见解压形式)
enum class ModResourceType

// MOD资源
class ModResource(
    val type: ModResourceType,      // 资源类型       [4 Bytes]
    val name: LengthString,         // 资源名         [*]
    val data: LengthBinary,         // 资源数据       [*]
)

class ModMedia(
    val id: LengthString,            // 媒体ID
    val resources: List<ModResource> // 资源集
)

class Mod(
    val metadata: ModMetadata, // 元数据
    val medias: List<ModMedia>, // 媒体集
)
```

 - MOD以常规二进制形式存储
 - 数字为大端序
 - 文本为UTF-8, 长度前由前4字节的值指定
 - 资源二进制数据每interval=64KB间会插入一个随机字节
 - 意味着相同的MOD可能有不同的二进制数据表示, 所以提供独立的摘要算法
