import SwiftUI

/// Modal explaining the coin economy. Triggered from the coin badge in
/// `TopBar`. The "Buy a hint for HINT_COIN_COST points" line is the
/// point of the dialog — players who run out of free hints need to
/// know they can keep advancing by spending coins.
struct CoinInfoDialog: View {
    let coins: Int
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.78)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)

            VStack(alignment: .center, spacing: 0) {
                ZStack {
                    Circle()
                        .fill(LinearGradient(
                            colors: [Color(hex: 0xFFFFE070), Color(hex: 0xFFFFB020)],
                            startPoint: .topLeading, endPoint: .bottomTrailing,
                        ))
                        .overlay(Circle().stroke(Color(hex: 0xFFB37400), lineWidth: 2))
                    Text("★")
                        .font(.system(size: 34, weight: .black))
                        .foregroundColor(.white)
                }
                .frame(width: 56, height: 56)
                .padding(.top, 24)

                Text("Points")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.top, 10)

                Text("You have \(coins)")
                    .font(.system(size: 14))
                    .foregroundColor(.white.opacity(0.75))
                    .padding(.top, 2)

                VStack(alignment: .leading, spacing: 5) {
                    CoinInfoRow(glyph: "✏️", title: "Find words",
                                detail: "+2 points per word — grid answers and bonus words.")
                    CoinInfoRow(glyph: "🏆", title: "Finish a level",
                                detail: "+10 bonus when you complete the crossword.")
                    CoinInfoRow(glyph: "🎁", title: "Daily spin",
                                detail: "Spin the wheel once a day for a coin / hint reward.")
                    CoinInfoRow(glyph: "💡",
                                title: "Buy a hint for \(HINT_COIN_COST)",
                                detail: "When your free hints are gone, tap the lightbulb to spend \(HINT_COIN_COST) points and reveal a letter.")
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)

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

private struct CoinInfoRow: View {
    let glyph: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Text(glyph)
                .font(.system(size: 20))
                .frame(width: 30, alignment: .center)
                .padding(.top, 1)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.white)
                Text(detail)
                    .font(.system(size: 12))
                    .foregroundColor(.white.opacity(0.72))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 4)
    }
}
