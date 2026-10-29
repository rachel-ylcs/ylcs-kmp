# 客户端资源

`APIRes` 使用共享类型描述资源层级。客户端通过 `.url` 生成地址，服务端可使用相同路径组织公开资源。

## 定义层级

```kotlin
object PublicRes : APIRes("public") {
    object Assets : APIRes(this) {
        val Logo = APIRes(this, "logo.webp")
    }

    object Users : APIRes(this) {
        class User(uid: Int) : APIRes(this, "$uid") {
            val avatar = APIRes(this, "avatar.webp")
        }
    }
}
```

对应路径：

```text
PublicRes.Assets.Logo          → public/assets/logo.webp
PublicRes.Users.User(42).avatar → public/users/42/avatar.webp
```

仅传父节点时，路径段由当前类的简单名称转为小写。显式提供名称时按名称拼接。资源层级中的类型重命名可能改变公开路径。

## 生成地址

```kotlin
ClientEngine.init("https://api.example.com")
val avatarUrl = PublicRes.Users.User(42).avatar.url
```

结果为 `https://api.example.com/public/users/42/avatar.webp`。`.url` 是 `love.yinlin.cs` 中的扩展，直接拼接基础地址和资源路径，不执行存在性检查、查询编码、鉴权或缓存版本管理。

## 动态名称

`APIRes(parent, name)` 不对动态段进行编码或路径净化。资源 ID 应使用受控格式：

```kotlin
require(id.matches(Regex("[A-Za-z0-9_-]+")))
val resource = APIRes(PublicRes.Users, id)
```

任意用户文件名和外部 URL 不应直接作为动态路径段。

## 缓存与访问控制

网络图片模块负责实际下载和缓存，`APIRes` 仅提供路径。可变资源应明确版本参数、内容摘要名称或服务端缓存策略，避免内容更新后继续显示旧缓存。

公开静态目录不包含业务鉴权。需要权限检查的内容应通过受控 API 或其他授权路由访问，不能只依赖资源路径难以猜测。

`APIRes` 实现 `APIFile` 表示路径集合，但不是客户端上传数据包装。上传需使用 `apiFile(...)`，见[文件上传](file.md)。

## 业务中的头像与版本

About 界面的贡献者头像直接使用共享资源路径，并将缓存版本单独传给图片组件：

示例来源：[UIAbout.kt](https://github.com/rachel-ylcs/ylcs-kmp/blob/main/ylcs-app/app/account/src/commonMain/kotlin/love/yinlin/compose/ui/common/UIAbout.kt#L72)。

```kotlin
WebImage(
    source = ServerRes.Users.User(contributor.uid).avatar.url,
    version = DateEx.TodayLong.toString(),
    modifier = Modifier.size(Theme.size.image8),
    options = ImageOptions.Circular
)
```

`contributor` 是外层列表项，`DateEx.TodayLong` 是该界面的按日更新策略。`version` 参与图片缓存键，不会由 `APIRes.url` 拼到网络地址上；同一资源需要即时更新时应使用适合业务的版本来源。

## 源码入口

- `ylcs-module/cs/core/src/commonMain/kotlin/love/yinlin/cs/APIRes.kt`
- `ylcs-module/cs/client-engine/src/commonMain/kotlin/love/yinlin/cs/ResRequest.kt`
