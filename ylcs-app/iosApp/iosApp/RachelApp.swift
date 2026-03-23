import SwiftUI
import MMKV
import ylcs_app

@main
struct RachelApp: App {
    let instance: IOSDelegateApplication

    init() {
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
