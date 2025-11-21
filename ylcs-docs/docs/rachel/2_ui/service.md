### Service Startup

```kotlin

// All services are automatically registered.
// Services can be accessed from any location globally
// Services can set priorities to resolve dependency relationships
// Services can be set to start synchronously or asynchronously
class MyApplication : PlatformApplication<MyApplication>() {
    // bind os service, then you can use os module functions and variables
    val os by service(::StartupOS)
    // bind picker service, then you can open native system dialog, pick the file, pick a photo...
    val picker by service(::StartupPicker)
    // bind config service, then you can read or write application config in disk
    val config by service(::StartupAppConfig)
    @Composable
    override fun Content() {
        PrimaryButton(text = os.storage.dataPath) {
            picker.pickPhoto()
        }
    }
}

```