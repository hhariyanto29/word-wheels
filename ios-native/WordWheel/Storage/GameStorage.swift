import Foundation

/// Persisted form of the player's progress. Designed to round-trip
/// through `UserDefaults` via `Codable`/`JSONEncoder`, so each field
/// must be a plain value type (no SwiftUI state wrappers).
///
/// Mirrors the Android `GameSnapshot` schema one-for-one so both
/// platforms save and restore the same player state.
struct GameSnapshot: Codable {
    static let schemaVersion = 1

    var v: Int = schemaVersion
    var levelNum: Int
    var coins: Int
    var hintsLeft: Int
    var wordsTowardHint: Int
    /// Current tile order after any shuffles — stored as a string so
    /// it survives JSON encoding (Character isn't Codable natively).
    var tiles: String
    var found: [String]
    var bonusFound: [String]
    var revealed: [RevealedCell]

    struct RevealedCell: Codable {
        var row: Int
        var col: Int
        var ch: String  // single-character string
    }
}

/// Persists game progress to `UserDefaults`. JSON payload is < 1 KB so
/// synchronous writes are fine — UserDefaults itself batches its disk
/// flush.
final class GameStorage {
    private let defaults: UserDefaults
    private let key = "snapshot"
    private let suite = "word_wheel_game"

    init(defaults: UserDefaults? = nil) {
        // Store under a dedicated suite so app-level defaults (like
        // accessibility settings) stay separate from game state.
        self.defaults = defaults
            ?? UserDefaults(suiteName: "word_wheel_game")
            ?? UserDefaults.standard
    }

    func save(_ snapshot: GameSnapshot) {
        do {
            let data = try JSONEncoder().encode(snapshot)
            defaults.set(data, forKey: key)
        } catch {
            // Non-fatal — log and move on. Next successful save wins.
            print("[WordWheel] Failed to encode snapshot: \(error)")
        }
    }

    func load() -> GameSnapshot? {
        guard let data = defaults.data(forKey: key) else { return nil }
        do {
            let decoded = try JSONDecoder().decode(GameSnapshot.self, from: data)
            // Ignore snapshots from a future schema we don't understand.
            if decoded.v > GameSnapshot.schemaVersion { return nil }
            return decoded
        } catch {
            // Corrupt save — discard and start fresh rather than crash.
            print("[WordWheel] Failed to decode snapshot, discarding: \(error)")
            defaults.removeObject(forKey: key)
            return nil
        }
    }

    func clear() {
        defaults.removeObject(forKey: key)
    }
}
