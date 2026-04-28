import SwiftUI

/// Layered coin glyph. Two concentric circles + a centred star give a
/// more "game-y" point-counter than the plain green disc that came
/// before. Drawn entirely in SwiftUI so it scales cleanly on every
/// density without an asset bundle.
private struct CoinIcon: View {
    let size: CGFloat

    var body: some View {
        ZStack {
            Circle()
                .fill(LinearGradient(
                    colors: [Color(hex: 0xFFFFE070), Color(hex: 0xFFFFAA00)],
                    startPoint: .topLeading, endPoint: .bottomTrailing,
                ))
                .overlay(Circle().stroke(Color(hex: 0xFF8B5A00), lineWidth: 2))
            Text("★")
                .font(.system(size: size * 0.6, weight: .black))
                .foregroundColor(Color(hex: 0xFF8B5A00))
        }
        .frame(width: size, height: size)
    }
}

struct TopBar: View {
    let coins: Int
    let found: Int
    let total: Int
    let level: Int
    var streak: Int = 0

    /// The displayed (animated) coin count. Tracks `coins` lazily so a
    /// jump from 200→225 visibly counts up instead of snapping.
    @State private var displayedCoins: Int = 0
    /// Set briefly when coins jump by a meaningful amount (>=5) so we
    /// can pulse the icon. Single +2 word rewards skip this to avoid
    /// constant scaling noise.
    @State private var pulseActive: Bool = false

    var body: some View {
        HStack(spacing: 0) {
            CoinIcon(size: 22)
                .scaleEffect(pulseActive ? 1.25 : 1.0)
                .animation(.easeOut(duration: 0.22), value: pulseActive)
            Spacer().frame(width: 8)
            Text("\(displayedCoins)")
                .font(.system(size: 17, weight: .bold))
                .foregroundColor(.white)
                .contentTransition(.numericText())

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
        .onAppear { displayedCoins = coins }
        .onChange(of: coins) { newValue in
            let delta = newValue - displayedCoins
            // Count-up tween — ~600ms feels good for spin rewards while
            // small +2 word adds still settle quickly.
            withAnimation(.easeOut(duration: 0.6)) {
                displayedCoins = newValue
            }
            // Pulse only on chunky jumps (spin reward, level bonus).
            if delta >= 5 {
                pulseActive = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.24) {
                    pulseActive = false
                }
            }
        }
    }
}
