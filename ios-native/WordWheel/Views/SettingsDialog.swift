import SwiftUI

/// Settings panel — currently a single SFX toggle. Designed to grow
/// (music, vibration…) by adding rows without restructuring.
struct SettingsDialog: View {
    @ObservedObject var sound: SoundManager
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.78)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            VStack(spacing: 0) {
                Text("Settings")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.bottom, 16)

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Sound effects")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.white)
                        Text("Plays a chime when a word lands.")
                            .font(.system(size: 13))
                            .foregroundColor(.white.opacity(0.6))
                    }
                    Spacer()
                    Toggle("", isOn: $sound.sfxEnabled)
                        .labelsHidden()
                        .tint(Color(hex: 0xFF32C850))
                }
                .padding(.vertical, 8)

                // Future: music toggle goes here as another HStack.

                Spacer().frame(height: 20)

                Button(action: onDismiss) {
                    Text("Done")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 36).padding(.vertical, 12)
                        .background(RoundedRectangle(cornerRadius: 24)
                            .fill(Color(hex: 0xFF50A0E6)))
                }
                .buttonStyle(.plain)
            }
            .padding(24)
            .frame(maxWidth: 360)
            .background(RoundedRectangle(cornerRadius: 20).fill(GameColors.completeBg))
            .shadow(color: .black.opacity(0.5), radius: 18, x: 0, y: 8)
            .padding(.horizontal, 24)
        }
    }
}
