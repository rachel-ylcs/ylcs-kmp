// swift-tools-version: 5.9
import PackageDescription

// 空实现包, 仅用于给合成 dylib 链接注入 -framework UIKit
let package = Package(
    name: "Dummy",
    platforms: [
        .iOS(.v13)
    ],
    products: [
        .library(name: "Dummy", targets: ["Dummy"])
    ],
    targets: [
        .target(
            name: "Dummy",
            linkerSettings: [
                .linkedFramework("UIKit")
            ]
        )
    ]
)
