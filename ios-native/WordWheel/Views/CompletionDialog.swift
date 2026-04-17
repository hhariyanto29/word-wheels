import SwiftUI

struct CompletionDialog: View {
    let isLastLevel: Bool
    let onNext: () -> Void

    var body: some View {
        GeometryReader { geo in
            let dialogWidth = min(min(geo.size.width, geo.size.height) - 48, 320)
            let clampedWidth = max(dialogWidth, 220)
            ZStack {
                Color.black.opacity(0.47).ignoresSafeArea()
                VStack(spacing: 0) {
                    Text(isLastLevel ? "All Levels Complete!" : "Level Complete!")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                    Spacer().frame(height: 12)
                    Text("+10 pts bonus!")
                        .font(.system(size: 15))
                        .foregroundColor(GameColors.starYellow)
                    Spacer().frame(height: 24)
                    Button(action: onNext) {
                        Text(isLastLevel ? "Play Again (Lv.1)" : "Next Level")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 24).padding(.vertical, 10)
                            .background(RoundedRectangle(cornerRadius: 19).fill(Color(hex: 0xFF28B446)))
                    }
                    .buttonStyle(.plain)
                }
                .padding(24)
                .frame(width: clampedWidth)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(GameColors.completeBg)
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(GameColors.completeBorder, lineWidth: 2))
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
