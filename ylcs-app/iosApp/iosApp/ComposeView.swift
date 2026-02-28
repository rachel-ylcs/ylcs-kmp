import SwiftUI
import UIKit
import portal

struct ComposeView: UIViewControllerRepresentable {
    let instance: IOSDelegateApplication

    func makeUIViewController(context: Context) -> UIViewController {
        return instance.buildUIViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}