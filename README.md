<div align="center">
    <h1>银临茶舍跨平台APP</h1>
    <p>集资讯、听歌、美图、论坛、社交于一体的小银子聚集地 水群：828049503</p>
    <div>
        <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg?logo=android"/>
		<img src="https://img.shields.io/badge/Platform-iOS%20%2F%20macOS-lightgrey.svg?logo=apple"/>
        <img src="https://img.shields.io/badge/Platform-Windows-blue.svg?logo=esotericsoftware"/>
        <img src="https://img.shields.io/badge/Platform-Linux-red.svg?logo=linux"/>
        <img src="https://img.shields.io/badge/Platform-WASM%20%2F%20JS-yellow.svg?logo=javascript"/>
        <img src="https://img.shields.io/badge/Platform-Server-orange.svg?logo=openjdk"/>
    </div>
    <br>
</div>

## 跨平台特性

| 名称      | 平台  | 运行环境   |
|---------|-----|--------|
| Android | 安卓  | Native |
| IOS     | 苹果  | Native |
| Windows | 桌面  | JVM    |
| Linux   | 桌面  | JVM    |
| Mac     | 桌面  | JVM    |
| Web     | 网页  | Wasm   |
| Server  | 服务器 | JVM    |

1. 各端UI共享，逻辑共享，资源共享，数据共享。
2. 桌面、网页、平板横屏适配、安卓、苹果竖屏适配。
3. 桌面、安卓、网页Wasm代码混淆，高性能低体积。
4. 服务端与客户端共享数据格式、序列化库、网络库

## 工程目录

发布目录 `outputs`

### `compose UI multiplatform`

源代码目录 `composeApp`


1. androidMain
   - 运行：`IDEA - Android App`
   - 发布签名安装包：`composeApp:androidPublish`
2. iosArm64Main
   - 运行
   - 发布签名安装包
3. desktopMain
   - 运行(Debug)：`composeApp:desktopRunDebug`
   - 运行(Release)：`composeApp:desktopRunRelease`
   - 检查模块完整性: `composeApp:desktopCheckModules`
   - 发布可执行文件：`composeApp:desktopPublish`
4. wasmJsMain
   - 浏览器运行：`composeApp:webRun`
   - 发布网页：`composeApp:webPublish`


### `kotlin logic multiplatform`

源代码目录 `shared`


### `iosApp`

源代码目录 `iosApp`, `composeApp/iosArm64Main`


### `server`

源代码目录 `server`

- 运行： `IDEA - Ktor`
- 发布可执行文件：`server:serverPublish`

### `shared`

共享数据与代码目录 `shared`

## 部署

### 编译与运行环境

- IntelliJ IDEA 2025.1 EAP (Ultimate Edition)
- Clion 2025.1
- Windows(可编译Android/Windows/Web/Server)
- Mac(可编译Android/IOS/Mac/Web/Server)
- Linux(可编译Android/Linux/Web/Server)

### Common

git clone 源码，gradle同步，下载依赖，构建。

### Android

直接运行或发布。

### IOS

待补充

### Desktop

需要先使用Clion编译项目中的C++库，会自动生成动态链接库文件到libs目录。

打包时会自动将动态链接库复制到输出目录。

### Web

直接运行或发布

### Server

构建后运行会自动部署，将初始目录复制到当前目录下，可以直接运行或发布。

#### 分离环境

redis和mysql配置可在resources中的config.properties配置
可选择本地调试（连接备用服务器）或生产环境部署（连接localhost）

#### 更新

服务器resources下的如server.json类的文件只是初始配置文件，
当有变更时手动更新服务器文件，重启服务器即可，无需重新编译服务器代码。

**即每次更新只需要更换服务器jar文件重启即可**


## 自研轻量级 C/S 数据共享框架

### 接口定义

所有 C/S 交互接口均定义在 shared 模块的 love.yinlin.api.API类中

借助kotlin超强的DSL能力，提供完美的框架语法糖

### 服务端资源引用 ServerRes


``` kotlin
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

**[例 1]** 访问uid为8的用户的头像
``` kotlin
ServerRes.Users.User(8).avatar
```

**[例 2]** 访问uid为8的用户的图片(图片id是123)
``` kotlin
ServerRes.Users.User(8).Pics().pic(123)
```

### C/S 接口

C/S 接口的定义与数据模型在客户端（Android，IOS，Desktop，Web）与服务端中共享。

#### 公共接口

``` kotlin

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

与ServerRes相同，接口定义也是以树状层级写出。

接口路径不需要全写，只需在对应路径内填写名称。

路径中目录定义object继承自APINode，接口定义object继承自APIRoute

其中为方便简写，在三种方式（Get，Post，Form）与三种结构（自定义Request与Response，简单Request，简单Response）中自由组合。

形成数十种别名，如APIGetRequest，APIPostForm等。

对于无body的Request，可以省略Request泛型即使用APIResponse。

对简单Request可以直接使用Int、String作为Request泛型。 而Response同理。

复杂Request或Response可在object内自定义结构并声明@Serializable序列化。

数据模型可附带默认值。

#### 服务端

在ServerAPI中定义

``` kotlin

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

使用api函数可直接引用公共接口的路径object作为参数，其参数，body，表单的文件均自动识别名称及类型。

编译期泛型能保证所有的接口数据传递均是类型安全的，如果任何一个参数不匹配在编译前就会报错。

#### 客户端

在ClientAPI中定义

``` kotlin

val result = ClientAPI.request(
   route = API.User.Mail.GetMails,
   data = API.User.Mail.GetMails.Request(
       token = config.userToken,
       offset = offset
   )
)

```

客户端可直接使用request函数并引用公共接口的路径object，其参数，data等均自动识别名称及类型。

且接受服务器的响应result也与公共接口中定义的完全一致，自动识别响应数据模型。

编译期泛型能保证所有的接口数据传递均是类型安全的，如果任何一个参数不匹配在编译前就会报错。