import SwiftUI

struct TopBar: View {
    let coins: Int
    let found: Int
    let total: Int
    let level: Int
    var streak: Int = 0

    var body: some View {
        HStack(spacing: 0) {
            // Coins
            Circle()
                .fill(GameColors.gemGreen)
                .frame(width: 20, height: 20)
            Spacer().frame(width: 8)
            Text("\(coins)")
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(.white)

            Spacer().frame(width: 14)

            // Words badge
            Text("W  \(found)/\(total)")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 10).padding(.vertical, 4)
                .background(RoundedRectangle(cornerRadius: 14).fill(GameColors.badgeBlue))

            // Streak badge — only when there's an active streak
            if streak > 0 {
                Spacer().frame(width: 8)
                Text("🔥 \(streak)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .background(RoundedRectangle(cornerRadius: 14)
                        .fill(Color(red: 1, green: 0.44, blue: 0.16, opacity: 0.25)))
            }

            Spacer()

            // Level badge
            Text("Lv.\(level)")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 12).padding(.vertical, 4)
                .background(RoundedRectangle(cornerRadius: 14).fill(Color.white.opacity(0.157)))
        }
        .padding(.horizontal, 14).padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 22).fill(GameColors.topBarBg))
    }
}
