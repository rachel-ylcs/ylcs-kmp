import SwiftUI
import MMKV
import portal

@main
struct RachelApp: App {
    let instance: IOSDelegateApplication

    init() {
        MMKV.initialize(rootDir: nil, logLevel: MMKVLogLevel.none)
        let appInstance = IOSDelegateApplication()
        self.instance = appInstance
        appInstance.run()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView(instance: instance)
                .ignoresSafeArea(.all) // Let compose handle safe area
                .onOpenURL { url in
                    instance.onIOSDeepLink(uri: url)
                }
        }
    }
}
