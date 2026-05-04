import SwiftUI

@main
struct WordWheelApp: App {
    @StateObject private var soundManager = SoundManager()

    init() {
        // Kick off dictionary load off the main queue so the ~200 ms file
        // read doesn't delay the first frame. Until it finishes,
        // dictionary-recognised words won't count — splash + initial render
        // take longer than that in practice.
        DispatchQueue.global(qos: .utility).async {
            WordDictionary.load()
        }
    }

    var body: some Scene {
        WindowGroup {
            GameScreen()
                .environmentObject(soundManager)
                .preferredColorScheme(.dark)
                .statusBarHidden(true)
        }
    }
}
