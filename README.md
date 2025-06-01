<div align="center">
    <h1>银临茶舍跨平台 App</h1>
    <p>集资讯、听歌、美图、论坛、社交于一体的小银子聚集地 茶舍水群：828049503</p>
    <div>
        <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android"/>
        <img src="https://img.shields.io/badge/Platform-iOS%20%2F%20macOS-lightgrey.svg?logo=apple"/>
        <img src="https://img.shields.io/badge/Platform-Windows-blue.svg?logo=esotericsoftware"/>
        <img src="https://img.shields.io/badge/Platform-Linux-red.svg?logo=linux"/>
        <img src="https://img.shields.io/badge/Platform-WASM%20%2F%20JS-yellow.svg?logo=javascript"/>
        <img src="https://img.shields.io/badge/Platform-Server-orange.svg?logo=openjdk"/>
    </div>
    <div>
        <img src="https://img.shields.io/github/v/tag/rachel-ylcs/ylcs-kmp"/>
        <img src="https://img.shields.io/github/v/release/rachel-ylcs/ylcs-kmp"/>
        <img src="https://github.com/rachel-ylcs/ylcs-kmp/actions/workflows/build.yml/badge.svg"/>
    </div>
</div>

## 跨平台特性

| 名称      | 平台  | 运行环境   |
|---------|-----|--------|
| Android | 安卓  | Native |
| iOS     | 苹果  | Native |
| Windows | 桌面  | JVM    |
| Linux   | 桌面  | JVM    |
| macOS   | 桌面  | JVM    |
| Web     | 网页  | Wasm   |
| Server  | 服务器 | JVM    |

1. 各端 UI 共享，逻辑共享，资源共享，数据共享。
2. 桌面、网页、平板横屏适配、安卓、苹果竖屏适配。
3. 桌面、安卓、网页 Wasm 代码混淆，高性能低体积。
4. 服务端与客户端共享数据格式、序列化库、网络库

## 源代码集依赖

![项目依赖结构](docs/pics/structure.png)

## 工程目录

发布目录 `outputs`

### `compose UI multiplatform`

源代码目录 `ylcs-app`

1. androidMain
   - 运行：`IDEA - Android App`
   - 发布签名安装包：`ylcs-app:androidPublish`
2. iosMain
   - 进入iosApp目录，运行：`pod install`
   - Xcode打开`iosApp/iosApp.xcworkspace`，运行App
   - 发布签名安装包
3. desktopMain
   - 运行(Debug)：`ylcs-app:desktopRunDebug`
   - 运行(Release)：`ylcs-app:desktopRunRelease`
   - 检查模块完整性: `ylcs-app:desktopCheckModules`
   - 发布可执行文件：`ylcs-app:desktopPublish`
4. wasmJsMain
   - 浏览器运行：`ylcs-app:webRun`
   - 发布网页：`ylcs-app:webPublish`

### `iosApp`

源代码目录 `iosApp`, `ylcs-app/iosMain`

### `kotlin logic multiplatform`

### `server`

源代码目录 `ylcs-server`

- 运行： `IDEA - Ktor`
- 发布可执行文件：`ylcs-server:serverPublish`

### `shared`

共享数据与代码目录 `ylcs-shared`

### `music`

MOD核心实现 `ylcs-music`

### `modManager`

MOD管理器(桌面版) `ylcs-modManager`

## 部署

### 编译与运行环境

- IntelliJ IDEA 2025.1
- Xcode 16.3
- gradle 8.13-all
- JDK 21
- Clion 2025.1
- Windows(可编译Android/Windows/Web/Server)
- macOS(可编译Android/iOS/macOS/Web/Server)
- Linux(可编译Android/Linux/Web/Server)

### Common

git clone 本仓库，Gradle 同步，下载依赖，构建。

### Android

直接运行或发布。

### IOS

首先进入 iosApp 目录，执行 `pod install` 安装依赖。

然后使用 Xcode 直接运行或发布。

### Desktop

需要先进入 native 目录，执行 `build.bat`(Windows) 或 `build.sh`(Linux/macOS) 脚本，会自动生成动态链接库文件到 native/libs 目录。

打包时会自动将动态链接库复制到输出目录。

### Web

直接运行或发布

### Server

构建后运行会自动部署，将初始目录复制到当前目录下，可以直接运行或发布。

#### 分离环境

Redis 和 MySQL 配置可在 `resources` 中的 `config.properties` 配置
可选择本地调试（连接备用服务器）或生产环境部署（连接 `localhost`）

#### 更新

服务器 `resources` 下的如 `server.json` 类的文件只是初始配置文件，
当有变更时手动更新服务器文件，重启服务器即可，无需重新编译服务器代码。

**即每次更新只需要更换服务器 JAR 文件重启即可**

## 自研轻量级 C/S 数据共享框架

### 接口定义

所有 C/S 交互接口均定义在 `shared` 模块的 `love.yinlin.api.API` 类中

借助 Kotlin 超强的 DSL 能力，提供完美的框架语法糖

### 服务端资源引用 ServerRes

```kotlin
// 示例
object ServerRes : ResNode("public") {
	object Activity : ResNode(this, "activity") {
		fun activity(uniqueId: String) = ResNode(this, "${uniqueId}.webp")
	}

	object Assets : ResNode(this, "assets") {
		val DefaultAvatar = ResNode(this, "default_avatar.webp")

		val DefaultWall = ResNode(this, "default_wall.webp")
	}

	object Users : ResNode(this, "users") {
		class User(uid: Int) : ResNode(this, "$uid") {
			val avatar = ResNode(this, "avatar.webp")

			val wall = ResNode(this, "wall.webp")

			inner class Pics : ResNode(this, "pics") {
				fun pic(uniqueId: String) = ResNode(this, "${uniqueId}.webp")
			}
		}
	}

	val Server = ResNode(this, "server.json")
	val Update = ResNode(this, "update.json")
	val Photo = ResNode(this, "photo.json")
}

```

服务端配置或公共文件的配置信息可以按树状object层级依次写出。

路径只需要提供名称，而不需要像其他框架般补全完整路径。

**例 1** 访问 UID 为 8 的用户的头像
```kotlin
ServerRes.Users.User(8).avatar
```

**例 2** 访问 UID 为 8 的用户的图片(图片 ID 是 123)
```kotlin
ServerRes.Users.User(8).Pics().pic(123)
```

### C/S 接口

C/S 接口的定义与数据模型在客户端（Android / iOS / Desktop / Web）与服务端中共享。

#### 公共接口

```kotlin

object API : APINode(null, "") {
	object User : APINode(this, "user") {
		object Profile : APINode(this, "profile") {
			object GetProfile : APIPost<String, UserProfile>(this, "getProfile")

			object GetPublicProfile : APIPost<Int, UserPublicProfile>(this, "getPublicProfile")

			object UpdateName : APIPostRequest<UpdateName.Request>(this, "updateName") {
				@Serializable
				data class Request(val token: String, val name: String)
			}

			object UpdateAvatar : APIFormRequest<String, UpdateAvatar.Files>(this, "updateAvatar") {
				@Serializable
				data class Files(val avatar: APIFile)
			}

			object Signin : APIPostRequest<String>(this, "signin")
		}

		object Topic : APINode(this, "topic") {
			object GetTopics : APIPost<GetTopics.Request, List<love.yinlin.data.rachel.Topic>>(this, "getTopics") {
				@Serializable
				data class Request(val uid: Int, val isTop: Boolean = true, val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SendTopic : APIForm<SendTopic.Request, SendTopic.Response, SendTopic.Files>(this, "sendTopic") {
				@Serializable
				data class Request(val token: String, val title: String, val content: String, val section: Int)
				@Serializable
				data class Files(val pics: APIFiles)
				@Serializable
				data class Response(val tid: Int, val pic: String?)
			}
		}
	}
}

```

与 ServerRes 相同，接口定义也是以树状层级写出。

接口路径不需要全写，只需在对应路径内填写名称。

路径中目录定义 object 继承自 APINode，接口定义 object 继承自 APIRoute

其中为方便简写，在三种方式（Get / Post / Form）与三种结构（自定义 Request 与 Response，简单 Request，简单 Response）中自由组合。

形成数十种别名，如 APIGetRequest，APIPostForm 等。

对于无 Body 的 Request，可以省略 Request 泛型即使用 APIResponse。

对简单 Request 可以直接使用 Int、String 作为 Reques t泛型，Response 同理。

复杂 Request 或 Response 可在 object 内自定义结构并声明 `@Serializable` 序列化。

数据模型可附带默认值。

#### 服务端

在 ServerAPI 中定义

```kotlin

api(API.User.Topic.SendTopic) { (token, title, content, section), (pics) ->
     VN.throwEmpty(title, content)
     VN.throwSection(section)
     val ngp = NineGridProcessor(pics)
     val uid = AN.throwExpireToken(token)
     val privilege = DB.throwGetUser(uid, "privilege")["privilege"].Int
     if (!UserPrivilege.topic(privilege) ||
         (section == Comment.Section.NOTIFICATION && !UserPrivilege.vipTopic(privilege)))
         return@api "无权限".failedData
     val tid = DB.throwInsertSQLGeneratedKey("""
         INSERT INTO topic(uid, title, content, pics, section, rawSection) VALUES(?, ?, ?, ?, ?, ?)
     """, uid, title, content, ngp.jsonString, section, section).toInt()
     // 复制主题图片
     val userPics = ServerRes.Users.User(uid).Pics()
     val pic = ngp.copy { userPics.pic(it) }
     Data.Success(API.User.Topic.SendTopic.Response(tid, pic), "发表成功")
}

```

使用 api 函数可直接引用公共接口的路径 object 作为参数，其参数，Body，表单的文件均自动识别名称及类型。

编译期泛型能保证所有的接口数据传递均是类型安全的，如果任何一个参数不匹配在编译前就会报错。

#### 客户端

在 ClientAPI 中定义

```kotlin

val result = ClientAPI.request(
   route = API.User.Mail.GetMails,
   data = API.User.Mail.GetMails.Request(
       token = config.userToken,
       offset = offset
   )
)

```

客户端可直接使用 request 函数并引用公共接口的路径 object，其参数，data 等均自动识别名称及类型。

且接受服务器的响应 result 也与公共接口中定义的完全一致，自动识别响应数据模型。

编译期泛型能保证所有的接口数据传递均是类型安全的，如果任何一个参数不匹配在编译前就会报错。

## Music模组协议

### MOD 核心模块 music

与 MOD 相关的模块为 music 和 modManager

music 模块是应用 MOD 存储协议及序列化与反序列化的核心实现, 被多平台客户端和 MOD 管理器调用

具体协议可参看 [Music 模组协议文档](docs/markdown/music.md)

### MOD 管理器 modManager

MOD 管理器是方便桌面客户端创建, 解析, 预览 MOD 的简易工具

![MOD管理器](docs/pics/mod_manager.png)
