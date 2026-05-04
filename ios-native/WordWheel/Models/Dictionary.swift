import Foundation

/// In-memory word dictionary used to recognise bonus words beyond the
/// per-level curated `bonusWords` list. Source file ships in the bundle
/// (Resources/dictionary.txt) — see tools/dictionary/build_dictionary.py
/// for how it's produced.
///
/// Loaded once via `load()` (call from a background queue at app start).
/// Until `isLoaded` flips true, `contains` returns false; that means
/// dictionary-recognised words count as "no match" during the brief
/// load window.
enum WordDictionary {
    private static var words: Set<String>?
    private static let lock = NSLock()

    static var isLoaded: Bool {
        lock.lock(); defer { lock.unlock() }
        return words != nil
    }

    static var size: Int {
        lock.lock(); defer { lock.unlock() }
        return words?.count ?? 0
    }

    /// Reads Resources/dictionary.txt into a `Set<String>`. Call from a
    /// background queue (DispatchQueue.global(qos: .utility)) — reading
    /// 200k+ lines on the main thread will jank app startup.
    static func load() {
        lock.lock(); defer { lock.unlock() }
        if words != nil { return }

        guard let url = Bundle.main.url(forResource: "dictionary", withExtension: "txt") else {
            words = []
            return
        }
        var set = Set<String>()
        set.reserveCapacity(320_000)
        if let data = try? Data(contentsOf: url),
           let text = String(data: data, encoding: .utf8) {
            text.enumerateLines { line, _ in
                let trimmed = line.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty {
                    set.insert(trimmed)
                }
            }
        }
        words = set
    }

    static func contains(_ word: String) -> Bool {
        let w = word.lowercased()
        lock.lock(); defer { lock.unlock() }
        return words?.contains(w) ?? false
    }
}
