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

工具仅构建 Windows x64、Linux x64、macOS arm64 的 Native Compose 应用，无需 JVM/JRE。

```powershell
.\gradlew.bat :ylcs-app:mod-manager:modManagerRunDebug
.\gradlew.bat :ylcs-app:mod-manager:composeReleaseRun
.\gradlew.bat :ylcs-app:mod-manager:composeDistribution
```

Gradle 的 Debug 和 Release 运行任务使用仓库 `work/modManager` 作为当前工作目录，对应 `C.root.work.modManager.asFile`；目录不存在时自动创建。`library`、`output` 和 `mod` 均位于该工作目录下。直接启动分发后的程序时，由启动方式决定当前工作目录，程序不内置仓库工作路径。

应用在 `init` 块中同步创建 `library` 和 `output`，完成后才进入窗口与首屏初始化，避免异步创建目录和读取曲库发生竞争。曲库读取失败会显示提示。

`composeDistribution` 在当前宿主生成 `ylcs-app/mod-manager/build/nativeApplication/<windows|linux|macos>/releaseExecutable/`，包含可执行程序、Compose 资源和 `icudtl.dat`。Windows 为 `ModManager.exe`，Linux 为 `ModManager.kexe`，macOS 为 `ModManager.app`。Debug 运行任务先准备相邻的 `debugExecutable/` 完整目录，再启动其中的程序。`build/bin/<target>/<binary>/` 仅为链接产物，不能直接复制它作为完整分发包。符号文件单独存放在 `build/nativeSymbols`，不进入应用分发目录。

发布由 CI 完成，不提供本地 Publish 任务。推送 `modManager*` 标签触发 `release_mod_manager.yml`，分别在 Windows、Linux、macOS 宿主执行 `packageWindowsReleaseExecutable`、`packageLinuxReleaseExecutable`、`packageMacosReleaseExecutable`，上传三个平台的应用 ZIP artifact。全量 `v*` 发布的 `release.yml` 复用相同工作流，将三个应用 ZIP 一并附加到 Release。ZIP 名称为 `ModManager-<mingw_x64|linux_x64|macos_arm64>-release.zip`，保留 Linux/macOS 可执行权限。当前该工作流没有上传独立符号 artifact；本地生成的 `build/nativeSymbols` 不会因此自动保留。

工具支持预览、解包、合并或分别打包、资源筛选和部署。源目录发现流程检查 config、audio、record、background 与 lyrics 等基础资源。

部署流程为每首媒体生成只包含 BASE 的 `base.rachel`，同时复制 DEPLOYMENT 资源作为旁路文件。该目录组织是工具部署策略，不增加 v5 归档字段。

## 管理器中的实际打包调用

MOD Manager 将业务筛选结果交给格式库，并使用 File 的写入作用域管理 Sink：

示例来源：[ScreenMain.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/mod-manager/src/nativeMain/kotlin/love/yinlin/screen/ScreenMain.kt#L125)。

```kotlin
File(app.outputPath, filename).write { sink ->
    ModFactory.Merge(paths, sink).process(filters = filters, onProcess = onProcess)
}
```

`filename`、`paths`、`filters` 和 `onProcess` 来自管理器的挂起打包方法。`Merge.process` 不接管外层输出文件的最终发布策略；处理中断时应处理不完整输出，不能把已创建文件等同于有效归档。

## 源码入口

- `ylcs-app/mod/src/commonMain/kotlin/love/yinlin/mod/ModFactory.kt`
- `ylcs-app/mod/src/commonMain/kotlin/love/yinlin/data/mod/ModResourceType.kt`
- `ylcs-app/mod/src/commonMain/kotlin/love/yinlin/data/music/MusicInfo.kt`
- `ylcs-app/mod-manager/src/nativeMain/kotlin/love/yinlin/screen/ScreenMain.kt`
