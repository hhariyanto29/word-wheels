import SwiftUI

/// Bottom action bar.
///
/// Drag-on-the-wheel commits the selected word automatically when the
/// gesture ends (see `LetterWheel`). That makes Submit and Backspace
/// buttons redundant — only the Hint button stays.
struct BottomButtons: View {
    let hintsLeft: Int
    let wordsTowardHint: Int
    let onHint: () -> Void

    var body: some View {
        HStack {
            HintButton(hintsLeft: hintsLeft,
                       wordsTowardHint: wordsTowardHint,
                       onHint: onHint)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }
}

private struct HintButton: View {
    let hintsLeft: Int
    let wordsTowardHint: Int
    let onHint: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            Button(action: onHint) {
                ZStack(alignment: .topTrailing) {
                    Circle()
                        .fill(hintsLeft > 0
                              ? Color(hex: 0xFFFFB400)
                              : Color(hex: 0xB4282828))
                        .frame(width: 64, height: 64)
                    Text("\u{1F4A1}")
                        .font(.system(size: 28))
                        .frame(width: 64, height: 64)
                    // count badge
                    ZStack {
                        Circle()
                            .fill(hintsLeft > 0
                                  ? Color(hex: 0xFF32B450)
                                  : Color(hex: 0xFF787878))
                            .frame(width: 22, height: 22)
                        Text("\(hintsLeft)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .offset(x: 6, y: -4)
                }
            }
            .buttonStyle(.plain)

            // Progress toward the next free hint (every 10 words found).
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color(hex: 0xFF28283C))
                    .frame(width: 80, height: 6)
                let progress = CGFloat(min(wordsTowardHint, 10)) / 10.0
                if progress > 0 {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color(hex: 0xFF50DC78))
                        .frame(width: 80 * progress, height: 6)
                }
            }
        }
    }
}
