import SwiftUI

/// Difficulty buckets shown to the player at level milestones (10, 20,
/// 40, 60, 80). Mirrors the Android `Difficulty` enum.
enum Difficulty {
    case easy
    case medium
    case hard
    case expert
    case master
    case legend

    var displayName: String {
        switch self {
        case .easy:   return "Easy"
        case .medium: return "Medium"
        case .hard:   return "Hard"
        case .expert: return "Expert"
        case .master: return "Master"
        case .legend: return "Legend"
        }
    }

    var tagline: String {
        switch self {
        case .easy:   return "A gentle warm-up."
        case .medium: return "Things start mixing up."
        case .hard:   return "Crosswords get bigger."
        case .expert: return "Multi-word grids and rare letters."
        case .master: return "8-letter spines from here on."
        case .legend: return "Push through to 100."
        }
    }

    var accent: Color {
        switch self {
        case .easy:   return Color(hex: 0xFF32C850)
        case .medium: return Color(hex: 0xFF50A0E6)
        case .hard:   return Color(hex: 0xFFFFB400)
        case .expert: return Color(hex: 0xFFE65050)
        case .master: return Color(hex: 0xFFB450E6)
        case .legend: return Color(hex: 0xFFFFDC50)
        }
    }

    /// The tier the player just unlocked by completing `level`. Returns
    /// nil when no milestone fires.
    static func milestone(for completedLevel: Int) -> Difficulty? {
        switch completedLevel {
        case 10: return .medium
        case 20: return .hard
        case 40: return .expert
        case 60: return .master
        case 80: return .legend
        default: return nil
        }
    }
}
