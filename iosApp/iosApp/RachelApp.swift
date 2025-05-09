import SwiftUI
import MMKV

@main
struct RachelApp: App {
    init() {
        MMKV.initialize(rootDir: nil, logLevel: MMKVLogLevel.none)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
