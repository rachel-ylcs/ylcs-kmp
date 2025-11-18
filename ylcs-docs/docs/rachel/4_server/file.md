文件上传和普通的`HTTP`请求类似，也是通过在公共模块中定义接口清单。

```kotlin
val ApiProfileUpdateAvatar by API.form.i<String, APIFile>().o()
```

你可以将文件作为一种参数类型与其他数据类型放在一起传递，这同时适用于客户端与服务端。

!!! Tip
    因为`APIFile`是一个接口，用于约束编译器检查参数类型安全，但运行时它在客户端和服务端有着不同的实现。
    在客户端的实现为`ClientFile`，其可以通过`ByteArray`、`File`、`Channel`等方式构造，在服务端的实现为`ServerRes`(见资源章节)。

在此例中，定义了实际路径为`/profile/updateAvatar`的接口，其中需要传递一个字符串参数和**一份文件**，不需要响应返回值。

### APIFile

注意所说的**一份文件**，这里的`APIFile`它可以指代一个或多个文件，通常它的方法不区分具体的个数。
它取决于你的业务逻辑保证，即调用时你可以对一个`APIFile`对象调用适用于单文件的方法或者多文件的方法，只是调用错了类型会抛出异常而已。

下面来看如何写客户端上传文件的响应请求：

```kotlin
ApiProfileUpdateAvatar.response { uid, avatar ->
    avatar.copy(ServerRes.Users.User(uid).avatar)
}
```

我们可以看出，文件和数据类型具有相同的地位，与常规`HTTP`请求写起来并无二样。

我们可以直接从`lambda`的参数处获取`String`类型的`uid`，以及`APIFile`类型的`avatar`。

此时客户端上传的文件会被存储为临时文件，`avatar`对应的便是这份文件。

此时我们接口由业务保证了上传一定是单张图片，所以我们可以直接使用`APIFile`的拓展函数`copy`将这个临时文件拷贝到我们用户头像所在的静态目录下（参见资源章节）。