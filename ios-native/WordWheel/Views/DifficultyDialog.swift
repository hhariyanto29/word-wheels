import SwiftUI

/// Modal banner shown after the player completes a milestone level.
struct DifficultyDialog: View {
    let tier: Difficulty
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.78)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(spacing: 12) {
                Text("DIFFICULTY UNLOCKED")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white.opacity(0.6))

                Text(tier.displayName.uppercased())
                    .font(.system(size: 40, weight: .heavy))
                    .foregroundColor(tier.accent)

                Text(tier.tagline)
                    .font(.system(size: 16))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)

                Spacer().frame(height: 8)

                Button(action: onDismiss) {
                    Text("CONTINUE")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 12)
                        .background(RoundedRectangle(cornerRadius: 24).fill(tier.accent))
                }
                .buttonStyle(.plain)
            }
            .padding(28)
            .frame(maxWidth: 340)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(GameColors.completeBg)
                    .overlay(RoundedRectangle(cornerRadius: 20)
                        .stroke(tier.accent, lineWidth: 3))
            )
            .shadow(color: .black.opacity(0.5), radius: 18, x: 0, y: 8)
            .padding(.horizontal, 24)
        }
    }
}
