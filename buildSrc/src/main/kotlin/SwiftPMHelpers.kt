import org.gradle.api.Project

// TODO: 我拉了你最新的代码，只是更新一些依赖版本后iOS的编译就挂了
// 看起来还是因为MMKV链接的时候没有带上UIKit, 但不知道你之前为什么没问题, 等你后面再看看吧
fun Project.patchMMKVSwiftPackage() {
    tasks.named("fetchSyntheticImportProjectPackages") {
        doLast {
            val packageFile = layout.buildDirectory.file("kotlin/swiftPMCheckout/checkouts/MMKV/Package.swift").get().asFile
            if (!packageFile.isFile) return@doLast

            packageFile.setWritable(true, true)

            val originalBlock = """            linkerSettings: [
                .linkedLibrary("z"),
                .linkedLibrary("c++")
            ]"""
            val patchedBlock = """            linkerSettings: [
                .linkedLibrary("z"),
                .linkedLibrary("c++"),
                .linkedFramework("UIKit")
            ]"""

            val contents = packageFile.readText()
            if (contents.contains(originalBlock) && !contents.contains(".linkedFramework(\"UIKit\")")) {
                packageFile.writeText(contents.replace(originalBlock, patchedBlock))
            }
        }
    }
}