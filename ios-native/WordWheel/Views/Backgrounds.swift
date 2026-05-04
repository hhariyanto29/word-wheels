import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// Per-level country background. One image per 10-level range — see the
/// map below. Files live in `WordWheel/Resources/Backgrounds/` and are
/// auto-bundled by XcodeGen via the `resources: WordWheel/Resources`
/// entry in project.yml.
private let backgroundByRange: [String] = [
    "bg_lv_001_010_vietnam",
    "bg_lv_011_020_brunei",
    "bg_lv_021_030_malaysia",
    "bg_lv_031_040_myanmar",
    "bg_lv_041_050_papua_nugini",
    "bg_lv_051_060_filipina",
    "bg_lv_061_070_singapore",
    "bg_lv_071_080_thailand",
    "bg_lv_081_090_indonesia",
    "bg_lv_091_100_bali",
]

private func backgroundResourceName(level: Int) -> String? {
    let idx = (level - 1) / 10
    guard idx >= 0 && idx < backgroundByRange.count else { return nil }
    return backgroundByRange[idx]
}

/// Renders the appropriate per-level background. Falls back to the
/// legacy "GameBackground" asset (Egypt) when `level` is outside
/// 1...100 — the spare ships under that name in Assets.xcassets.
///
/// The image is loaded via `UIImage(named:)`, which searches both the
/// asset catalog AND the main bundle, so files in `Resources/Backgrounds/`
/// are found by their basename (no extension, no path).
struct GameBackgroundImage: View {
    let level: Int

    var body: some View {
        #if canImport(UIKit)
        if let name = backgroundResourceName(level: level),
           let ui = UIImage(named: name) {
            Image(uiImage: ui)
                .resizable()
                .aspectRatio(contentMode: .fill)
        } else {
            Image("GameBackground")
                .resizable()
                .aspectRatio(contentMode: .fill)
        }
        #else
        // macOS / non-UIKit fallback for local typecheck only — production
        // path is the UIKit branch above on iOS.
        Image(backgroundResourceName(level: level) ?? "GameBackground")
            .resizable()
            .aspectRatio(contentMode: .fill)
        #endif
    }
}
