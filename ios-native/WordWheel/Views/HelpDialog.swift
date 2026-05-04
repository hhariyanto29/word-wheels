import SwiftUI

/// "How to Play" modal. Shown automatically on first launch (gated by
/// `GameStorage.seenHelp`) and via the `?` icon in the TopBar afterwards.
struct HelpDialog: View {
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.78)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(alignment: .center, spacing: 0) {
                Text("How to Play")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.top, 24)

                VStack(alignment: .leading, spacing: 6) {
                    HelpRow(glyph: "✋", title: "Drag to spell",
                            detail: "Drag your finger across the wheel letters to build a word. Release to submit.")
                    HelpRow(glyph: "✓", title: "Fill the grid",
                            detail: "Words you find that fit the crossword fill in. Solve them all to clear the level.")
                    HelpRow(glyph: "★", title: "Bonus words",
                            detail: "Extra valid words still earn points — they appear in the row below the wheel.")
                    HelpRow(glyph: "💡", title: "Hints",
                            detail: "Tap the hint button to reveal one grid letter. Earn +1 hint every 10 words you find.")
                    HelpRow(glyph: "🔄", title: "Shuffle",
                            detail: "Tap the centre of the wheel to shuffle the tiles when you're stuck.")
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                Button(action: onDismiss) {
                    Text("Got it")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 36).padding(.vertical, 12)
                        .background(RoundedRectangle(cornerRadius: 24).fill(GameColors.submitBg))
                }
                .buttonStyle(.plain)
                .padding(.vertical, 22)
            }
            .frame(maxWidth: 420)
            .padding(.horizontal, 12)
            .background(
                RoundedRectangle(cornerRadius: 20).fill(GameColors.completeBg)
            )
            .shadow(color: .black.opacity(0.5), radius: 18)
            .padding(.horizontal, 24)
        }
    }
}

private struct HelpRow: View {
    let glyph: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(glyph)
                .font(.system(size: 22))
                .frame(width: 32, alignment: .center)
                .padding(.top, 2)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                Text(detail)
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.75))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
    }
}
