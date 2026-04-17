import SwiftUI

struct BottomButtons: View {
    let hintsLeft: Int
    let wordsTowardHint: Int
    let onHint: () -> Void
    let onSubmit: () -> Void
    let onBackspace: () -> Void

    var body: some View {
        HStack {
            HintButton(hintsLeft: hintsLeft, wordsTowardHint: wordsTowardHint, onHint: onHint)

            Spacer()

            Button(action: onSubmit) {
                Text("Submit")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24).padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 20).fill(GameColors.submitBg))
            }
            .buttonStyle(.plain)

            Spacer()

            Button(action: onBackspace) {
                ZStack {
                    Circle().fill(GameColors.hintBtnBg).frame(width: 54, height: 54)
                    Text("\u{232B}").font(.system(size: 22)).foregroundColor(.white)
                }
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 8)
    }
}

private struct HintButton: View {
    let hintsLeft: Int
    let wordsTowardHint: Int
    let onHint: () -> Void

    var body: some View {
        VStack(spacing: 4) {
            Button(action: onHint) {
                ZStack(alignment: .topTrailing) {
                    Circle().fill(GameColors.hintBtnBg).frame(width: 54, height: 54)
                    Text("\u{1F4A1}").font(.system(size: 22))
                        .frame(width: 54, height: 54)
                    // badge
                    ZStack {
                        Circle()
                            .fill(hintsLeft > 0 ? Color(hex: 0xFF32B450) : Color(hex: 0xFF787878))
                            .frame(width: 18, height: 18)
                        Text("\(hintsLeft)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .offset(x: 4, y: -4)
                }
            }
            .buttonStyle(.plain)

            // Progress bar toward next hint
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(Color(hex: 0xFF28283C))
                    .frame(width: 54, height: 6)
                let progress = CGFloat(min(wordsTowardHint, 10)) / 10.0
                if progress > 0 {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(Color(hex: 0xFF50DC78))
                        .frame(width: 54 * progress, height: 6)
                }
            }
        }
    }
}
