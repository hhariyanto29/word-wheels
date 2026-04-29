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
                // Outer 84pt frame puts the badge fully OUTSIDE the
                // 64pt button corner so the digit isn't visually
                // swallowed by the gold disc behind it. White ring +
                // drop shadow on the badge make it pop independently.
                ZStack(alignment: .topTrailing) {
                    // Button anchored bottom-left of the outer frame
                    ZStack {
                        Circle()
                            .fill(hintsLeft > 0
                                  ? Color(hex: 0xFFFFB400)
                                  : Color(hex: 0xB4282828))
                            .frame(width: 64, height: 64)
                            .shadow(color: .black.opacity(0.4), radius: 4, x: 0, y: 3)
                        Text("\u{1F4A1}")
                            .font(.system(size: 28))
                    }
                    .frame(width: 84, height: 84, alignment: .bottomLeading)

                    // Count badge — anchored top-right of the outer
                    // 84pt frame. White ring keeps it readable
                    // regardless of what colour is behind it.
                    ZStack {
                        Circle()
                            .fill(hintsLeft > 0
                                  ? Color(hex: 0xFF32B450)
                                  : Color(hex: 0xFF787878))
                            .frame(width: 28, height: 28)
                            .overlay(Circle().stroke(Color.white, lineWidth: 2.5))
                            .shadow(color: .black.opacity(0.4), radius: 3, x: 0, y: 2)
                        Text("\(hintsLeft)")
                            .font(.system(size: 14, weight: .heavy))
                            .foregroundColor(.white)
                    }
                }
                .frame(width: 84, height: 84)
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
