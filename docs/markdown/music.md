# Music - MOD 协议

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

## MOD解压形式

目录内以 ```{ModType}-{Name}```的形式存储各资源文件, 相同```{ID}```的资源归属同一目录树

一个简单的目录树示例如下:

```
- music
      - 0
          - 0-config
          - 10-flac
          - 10-mp3
          - 20-record
          - 21-background
      - 1
          - 0-config
          - 10-flac
```

若某个媒体类型拥有默认名, 则媒体资源中有且仅有一个同名的资源

否则可以有若干个不同资源名的同类型资源文件

### ★ 媒体配置

| ModResourceType | 是否必需 | 实体类型 | 默认名    | 备注 |
|-----------------|------|------|--------|----|
| 0               | √    | json | config |    |

媒体配置记录了一个媒体的元信息, 各字段与要求如下表所示:

| 字段名      | 类型               | 含义      | 非空 | 备注                     |
|----------|------------------|---------|----|------------------------|
| version  | String           | 歌曲版本    | √  |                        |
| author   | String           | 作者      | √  |                        |
| id       | String           | 唯一ID    | √  |                        |
| name     | String           | 歌名      | √  |                        |
| singer   | String           | 歌手      | √  |                        |
| lyricist | String           | 作词      | √  |                        |
| composer | String           | 作曲      | √  |                        |
| album    | String           | 所属专辑    | √  |                        |
| chorus   | Array&lt;Int&gt; | 副歌点     | ×  | 单位为毫秒，如果为null歌曲就不能快进副歌 |

一个简单的配置示例如下所示:

```json
{
    "version": "1.0",
    "author": "官方",
    "id": "1",
    "name": "棠梨煎雪",
    "singer": "银临",
    "lyricist": "商连",
    "composer": "银临",
    "album": "腐草为萤",
    "chorus": [71670, 161140, 210400]
}
```

### ★ 音源

| ModResourceType | 是否必需 | 实体类型      | 默认名 | 备注 |
|-----------------|------|-----------|-----|----|
| 10              | √    | audio / * |     |    |

媒体的音频文件, 默认以flac格式提供

### ★ 封面

| ModResourceType | 是否必需 | 实体类型 | 默认名    | 备注 |
|-----------------|------|------|--------|----|
| 20              | √    | webp | record |    |

媒体的CD图片封面, 以webp图片形式存储

### ★ 壁纸

| ModResourceType | 是否必需 | 实体类型 | 默认名        | 备注 |
|-----------------|------|------|------------|----|
| 21              | √    | webp | background |    |

媒体的静态背景, 以webp图片形式存储

### ★ 逐行歌词

| ModResourceType | 是否必需 | 实体类型 | 默认名 | 备注 |
|-----------------|------|------|-----|----|
| 30              | √    | lrc  |     |    |

媒体的逐行歌词, 以lrc歌词形式存储

### ★ 动画

| ModResourceType | 是否必需 | 实体类型 | 默认名       | 备注 |
|-----------------|------|------|-----------|----|
| 22              | ×    | webp | animation |    |

媒体的动态背景, 以webp图片形式存储

### ★ 视频

| ModResourceType | 是否必需 | 实体类型 | 默认名 | 备注 |
|-----------------|------|------|-----|----|
| 40              | ×    | mp4  |     |    |

媒体的PV视频, 以mp4视频形式存储

### ★ PAG歌词

| ModResourceType | 是否必需 | 实体类型 | 默认名 | 备注 |
|-----------------|------|------|-----|----|
| 31              | ×    | pag  |     |    |

基于PAG引擎的动态歌词, 以pag二进制形式存储