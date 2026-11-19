---
description: MOD 归档格式与 MOD Manager 工具的维护说明。
---

# MOD 归档与 MOD Manager

<p class="rc-lead">工具章节记录的是格式契约与维护边界，而不是操作手册。格式一旦发布，reader、writer 与已有资源都需要共同演进。</p>

## MOD 归档

MOD 是 `ylcs-app/mod` 实现的音乐资源容器。归档扩展名为 `.rachel`，工作目录资源扩展名为 `.rachelres`。

```text title="工作目录"
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

| 资源类型 | 内容 | 基础资源 |
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

`BASE` 为前五项，`ALL` 为全部资源，`DEPLOYMENT` 为除音源外的全部资源。媒体格式、尺寸与编码约束属于应用约定，协议 reader 不执行完整媒体内容校验。

## 配置 JSON

```json title="config.rachelres"
{
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

`chorus` 为可空毫秒列表，`modification` 是带默认值的 `@Transient` 运行时字段，不进入 JSON。打包时媒体 ID 从配置内容读取，工作目录名称应与其保持一致。

## 二进制布局

整数使用大端，字符串使用字节长度前缀与 UTF-8 内容：

```text title="归档结构"
Archive
├─ magic
├─ version
├─ mediaCount
├─ info: LengthString
└─ Media × mediaCount
   ├─ id: LengthString
   ├─ resourceCount
   └─ Resource × resourceCount
      ├─ type: LengthString
      ├─ dataLength
      └─ payload
```

magic 与归档版本由 `ModFactory` 定义，reader 要求精确匹配。资源长度表示真实长度；写入时按固定块大小插入随机填充字节，因此：

```text title="存储长度"
storedLength = dataLength + padding(dataLength)
```

填充不提供加密或完整性校验，同一输入多次打包可能得到不同二进制内容。

!!! warning "预览器的配置边界"
    当前 `Preview` 的配置分支直接读取 `dataLength` 字节并解析 JSON，没有像解包分支逐块跳过填充。达到块边界的配置可能预览失败或导致后续读取错位。生产流程应保持配置小于该边界，不能把预览器视为任意大小配置的完整校验器。

## 打包

```kotlin title="打包"
output.write { sink ->
    ModFactory.Merge(
        mediaPaths = [songFolder],
        sink = sink,
        info = ModInfo(author = "示例作者"),
    ).process(filters = ModResourceType.ALL) { index, total, name ->
        println("$index / $total: $name")
    }
}
```

`Merge` 读取每个媒体目录中的配置，收集扩展名正确、type 已知且符合 filters 的资源。它不强制基础资源完整。资源长度转换为 Int 后需大于零，调用方应提前限制单资源大小。

## 预览与解包

```kotlin title="预览与释放"
val preview = input.read { source ->
    ModFactory.Preview(source).process()
}

val released = input.read { source ->
    ModFactory.Release(source, libraryRoot).process { index, total, id ->
        println("$index / $total: $id")
    }
}
```

`Preview` 返回归档元数据、媒体 ID、配置及资源类型和长度；`Release` 按完整块读取资源并丢弃填充，将文件写入 `<libraryRoot>/<id>/<type>.rachelres`。

!!! danger "导入边界"
    reader 不统一限制字符串长度、媒体数量、资源数量与总解包大小，也不验证重复 ID、资源覆盖或路径归属。导入方应限制安全 ID、文件总量与资源大小，并先在隔离临时目录完成验证。`Release` 直接使用归档 ID 构造目录，目标已有内容可能被覆盖。

## 兼容性

- reader 要求 magic 与版本精确匹配；
- 当前不包含旧版本自动迁移；
- 升级归档版本时应同步提供兼容 reader 或转换流程；
- 填充与长度规则属于格式协议，不能被业务层忽略。

## MOD Manager

工具仅构建 Windows x64、Linux x64、macOS arm64 的 Native Compose 应用，无需 JVM。

```powershell title="运行"
.\gradlew.bat :ylcs-app:mod-manager:modManagerRunDebug
.\gradlew.bat :ylcs-app:mod-manager:composeReleaseRun
.\gradlew.bat :ylcs-app:mod-manager:composeDistribution
```

Gradle 的 Debug 与 Release 运行任务使用仓库 `work/modManager` 作为当前工作目录。`library`、`output` 与 `mod` 均位于该目录下。直接启动分发后的程序时，由启动方式决定当前工作目录，程序不内置仓库工作路径。

| 任务 | 结果 |
| --- | --- |
| `modManagerRunDebug` | 运行 Debug 版本 |
| `composeReleaseRun` | 运行 Release 版本 |
| `composeDistribution` | 生成当前宿主的完整分发目录 |

`build/bin/<target>/<binary>/` 仅为链接产物，不能直接复制作为完整分发包。符号文件存放在 `build/nativeSymbols`，不进入应用分发目录。

工具支持预览、解包、合并或分别打包、资源筛选与部署。部署流程为每首媒体生成只包含 BASE 的 `base.rachel`，同时复制 DEPLOYMENT 资源作为旁路文件。

## 下一步

继续阅读[文档工程](docs.md)，了解本指南的构建、搜索、统计与发布方式。
