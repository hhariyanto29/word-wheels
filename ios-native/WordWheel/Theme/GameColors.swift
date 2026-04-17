import SwiftUI

/// Mirror of the Android `GameColors` object in Theme.kt. Kept as SwiftUI
/// `Color` values so every view can reach them via `GameColors.bgTop` etc.
enum GameColors {
    static let bgTop          = Color(hex: 0xFF3C64C8)
    static let bgBottom       = Color(hex: 0xFF8CB4F0)
    static let cellFilled     = Color(hex: 0xFF193778)
    static let cellEmpty      = Color(hex: 0xFFC8D7F0)
    static let cellFoundText  = Color.white
    static let topBarBg       = Color(hex: 0xDC142850)
    static let gemGreen       = Color(hex: 0xFF32C850)
    static let badgeBlue      = Color(hex: 0xFF50A0E6)
    static let wheelBg        = Color(hex: 0xE6FFFFFF)
    static let tileSelectedBg = Color(hex: 0xFF50A0E6)
    static let lineColor      = Color(hex: 0xFF50A0E6)
    static let letterColor    = Color(hex: 0xFF1E1E1E)
    static let hintBtnBg      = Color(hex: 0xB4282828)
    static let shuffleIcon    = Color(hex: 0xFF96A0AF)
    static let submitBg       = Color(hex: 0xFF50A0E6)
    static let gridBackdrop   = Color(hex: 0xB40A193C)
    static let starYellow     = Color(hex: 0xFFFFDC50)
    static let completeBorder = Color(hex: 0xFF64A0FF)
    static let completeBg     = Color(hex: 0xF5142850)
}

extension Color {
    /// Creates a Color from a 0xAARRGGBB integer (matching Compose's Color(0xAARRGGBB)).
    init(hex: UInt64) {
        let a = Double((hex >> 24) & 0xFF) / 255.0
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
