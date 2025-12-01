import UIKit
import SwiftUI
import ylcs_app

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.all) // Let compose handle safe area
                .onOpenURL { url in
                    MainViewControllerKt.onOpenUri(uri: url)
                }
    }
}
