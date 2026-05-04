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
        .padding(.vertical, 2)
    }
}

private struct HintButton: View {
    let hintsLeft: Int
    let wordsTowardHint: Int
    let onHint: () -> Void

    var body: some View {
        VStack(spacing: 3) {
            Button(action: onHint) {
                // Outer 78pt frame keeps the count badge sitting tight
                // against the 64pt gold disc — the previous 84pt left
                // a 10pt gap that read as "detached badge".
                ZStack(alignment: .topTrailing) {
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
                    .frame(width: 78, height: 78, alignment: .bottomLeading)

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
                .frame(width: 78, height: 78)
            }
            .buttonStyle(.plain)

            // Progress toward the next free hint (every 10 words found).
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color(hex: 0xFF28283C))
                    .frame(width: 72, height: 5)
                let progress = CGFloat(min(wordsTowardHint, 10)) / 10.0
                if progress > 0 {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color(hex: 0xFF50DC78))
                        .frame(width: 72 * progress, height: 5)
                }
            }
        }
    }
}
