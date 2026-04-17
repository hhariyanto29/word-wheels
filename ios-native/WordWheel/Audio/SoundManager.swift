import AVFoundation
import Combine

enum Sfx: String, CaseIterable {
    case select     = "sfx_select"
    case wordFound  = "sfx_word_found"
    case wrong      = "sfx_wrong"
    case bonus      = "sfx_bonus"
    case hint       = "sfx_hint"
    case shuffle    = "sfx_shuffle"
    case complete   = "sfx_complete"
    case backspace  = "sfx_backspace"
}

/// Preloads short WAV clips and plays them via a small AVAudioPlayer pool so
/// rapid-fire events (drag-select, backspace spam) don't cut each other off.
/// Designed to be a drop-in equivalent of the Android SoundPool wrapper.
final class SoundManager: ObservableObject {
    private var players: [Sfx: [AVAudioPlayer]] = [:]
    private let poolPerSfx = 3  // concurrent streams per effect

    init() {
        configureAudioSession()
        preload()
    }

    private func configureAudioSession() {
        // .ambient respects the user's silent-switch and doesn't stop
        // background music — the expected behaviour for a casual game.
        try? AVAudioSession.sharedInstance()
            .setCategory(.ambient, mode: .default, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
    }

    private func preload() {
        for sfx in Sfx.allCases {
            guard let url = Bundle.main.url(
                forResource: sfx.rawValue,
                withExtension: "wav"
            ) else { continue }
            var pool: [AVAudioPlayer] = []
            for _ in 0..<poolPerSfx {
                if let p = try? AVAudioPlayer(contentsOf: url) {
                    p.prepareToPlay()
                    pool.append(p)
                }
            }
            players[sfx] = pool
        }
    }

    func play(_ sfx: Sfx, volume: Float = 1.0) {
        guard let pool = players[sfx], !pool.isEmpty else { return }
        // Pick any player that isn't currently playing; fall back to the
        // first slot if all are busy (cuts off the oldest sound).
        let player = pool.first(where: { !$0.isPlaying }) ?? pool[0]
        player.currentTime = 0
        player.volume = volume
        player.play()
    }
}
