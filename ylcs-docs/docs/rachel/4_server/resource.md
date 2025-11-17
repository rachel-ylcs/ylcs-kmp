对于服务器后端的静态资源我们该如何访问呢？

我们知道它们实际存储在引擎重写的`public`目录下，
但为了客户端和服务端都能够以同样的方式访问到同一个资源，我们这里仍然使用类似于接口清单的方式定义资源清单。

我们同样在公共模块中定义`ServerRes.kt`文件

```kotlin
object ServerRes : APIRes("public") {
    object Users : APIRes(this) {
        class User(uid: Int) : APIRes(this, "$uid") {
            val avatar = APIRes(this, "avatar.webp")

            inner class Pics : APIRes(this) {
                fun pic(uniqueId: String) = APIRes(this, "${uniqueId}.webp")
            }
        }
    }
}
```

得益于`kotlin`强大的DSL，我们可以使用如此简洁清晰的方式定义资源清单。

所有的资源以及其路径上的节点都被看作`APIRes`的子类，在这里我们将根节点定义为`ServerRes`，名称你可以任意取。

`ServerRes`提供给基类的构造参数就是实际的静态资源目录，注意我们这里使用了`public`，而服务端也需要给引擎重写资源目录。
前面提到过尽量在双端公共部分定义此名称。如果你现在定义了`ServerRes`，那么在服务端便可以如下修改静态目录了。

```kotlin
override val public = ServerRes.toString()
```

`APIRes`重写了`toString()`方法，使得它总是能返回自身实际代表的路径。而子路径取决于它的类名，例如`Users`的路径会被解析成`/public/users`。

!!! Warning
    注意，如果你使用了`APIRes`来管理静态资源，你需要在混淆配置中保留所有派生类的名称，因为它与子路径类名相关。
    混淆配置如下：`-keep class * extends love.yinlin.api.APIRes`

现在我们回头来看，例子中出现的所有定义方式。

1. 首先是嵌套对象`Users`，当我们访问下一层级的资源时便可以如此般定义，这里会被解析成`/public/users`。
注意`APIRes`的构造中使用的`this`实际上引用的是`ServerRes`，而并非`Users`自身。这里无需手动填补路径，因为框架会自动解析。

2. 然后是带参数的类`User`, 如果我们访问下一层级的资源它的路径名与参数有关，便可以将`object`改为`class`。
嵌套类定义时构造参数除了需要上一层级的`this`外还需要手动指定构造方式，因为此时框架无法解析。
例如当我们使用`ServerRes.Users.User(20)`时实际上解析到的路径是`/public/users/user/20`。

3. 接着是最主要的资源文件，例如`avatar`，此时叶子节点路径已经到头了，此时便可以直接在构造参数中指定资源名。
当我们使用`ServerRes.Users.User(20).avatar`访问的便是`/public/users/user/20/avatar.webp`对应的图片资源了。

4. 对于嵌套类中仍然包含嵌套类的情况，可以使用`inner`内部类来实现。
例如使用`ServerRes.Users.User(20).Pics().pic("test")`访问的是`/public/users/user/20/pics/test.webp`。

上述均为客户端与服务端共同支持的内容，在服务端引擎中还特别的为`APIFile`提供了若干拓展函数。例如复制文件、删除文件、批量拷贝等，详情可见文件上传章节。