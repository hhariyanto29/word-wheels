import SwiftUI

/// Pre-game landing screen. Shows the logo + a big "LEVEL X" button
/// that resumes wherever the player left off, plus a streak/coins
/// stat row across the top and a settings cog top-right.
struct HomeScreen: View {
    let levelNum: Int
    let coins: Int
    let streak: Int
    let spinAvailable: Bool
    let onResume: () -> Void
    let onSpinClick: () -> Void
    let onSettingsClick: () -> Void

    var body: some View {
        ZStack {
            Image("GameBackground")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .ignoresSafeArea()
                .clipped()
            LinearGradient(
                colors: [
                    Color(.sRGB, red: 0, green: 0, blue: 0.08, opacity: 0.4),
                    Color(.sRGB, red: 0, green: 0, blue: 0.16, opacity: 0.33),
                    Color(.sRGB, red: 0, green: 0, blue: 0.2,  opacity: 0.5),
                ],
                startPoint: .top, endPoint: .bottom,
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Top stat bar
                HStack(spacing: 8) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(GameColors.gemGreen)
                            .frame(width: 18, height: 18)
                        Text("\(coins)")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(RoundedRectangle(cornerRadius: 20).fill(GameColors.topBarBg))

                    if streak > 0 {
                        Text("🔥 \(streak)")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .background(RoundedRectangle(cornerRadius: 20)
                                .fill(Color(red: 1, green: 0.44, blue: 0.16, opacity: 0.4)))
                    }

                    Spacer()

                    Button(action: onSettingsClick) {
                        Text("⚙")
                            .font(.system(size: 22))
                            .foregroundColor(.white)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(Color.white.opacity(0.25)))
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)

                Spacer()

                Text("WORD")
                    .font(.system(size: 44, weight: .heavy))
                    .foregroundColor(.white)
                Text("WHEEL")
                    .font(.system(size: 56, weight: .black))
                    .foregroundColor(GameColors.starYellow)

                Spacer()

                Button(action: onResume) {
                    Text("LEVEL \(levelNum)")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 60).padding(.vertical, 18)
                        .background(RoundedRectangle(cornerRadius: 36)
                            .fill(Color(hex: 0xFF32C850)))
                }
                .buttonStyle(.plain)

                Text("Tap to continue")
                    .font(.system(size: 13))
                    .foregroundColor(Color.white.opacity(0.8))
                    .padding(.top, 10)

                Spacer()

                if spinAvailable {
                    Button(action: onSpinClick) {
                        Text("🎁  Daily spin available")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 28).padding(.vertical, 12)
                            .background(RoundedRectangle(cornerRadius: 28)
                                .fill(Color(hex: 0xFFFFB400)))
                    }
                    .buttonStyle(.plain)
                    .padding(.bottom, 28)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
