import SwiftUI
import UIKit
import ylcs_app

struct ComposeView: UIViewControllerRepresentable {
    let instance: IOSDelegateApplication

    func makeUIViewController(context: Context) -> UIViewController {
        return instance.buildUIViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}