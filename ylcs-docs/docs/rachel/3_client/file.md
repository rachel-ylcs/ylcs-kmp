文件上传的主要方式是通过表单形式，框架在内部已经做好了完全的封装，使得你使用起来与发送普通请求并无二样。

对于上一个示例，假如我们还需要上传一个文件给服务器，那么便可以这样写：

## APIMap.kt

```kotlin
val ApiTestUploadFile by API.post.i<String, APIFile>().o<Int>()
```

文件与其他数据具有相同地位，其`APIFile`接口可以和其他参数一样填写在接口清单中或发送请求时。

## Test.kt

```kotlin
ApiTestUploadFile.request("Rachel", apiFile(Path("D:\\1.docx"))) { output ->
    println(output)
}
```

在发送请求时，我们可以通过`apiFile`函数来快速构造一个`APIFile`实例，这个函数可以接受类似于字符串、`ByteArray`、文件路径`Path`等等输入源。

与发送普通请求的参数类似，`APIFile?`文件参数也可以为空，代表用户可以选择是否上传文件。

想要了解更多关于文件上传的内容可以继续参看[服务端处理文件上传](../4_server/file.md)。