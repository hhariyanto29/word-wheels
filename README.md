# Word Wheel Crossword

A word puzzle game built with Rust and [egui](https://github.com/emilk/egui). Spin the letter wheel, form words, and fill in the crossword grid across 10 progressive levels.

## How to Play

1. **Drag or tap** letters on the wheel to form a word
2. **Release** to auto-submit, or tap **Submit**
3. Found words fill in the crossword grid
4. Find all grid words to complete the level
5. Bonus words earn extra points but don't appear on the grid

### Scoring

- Every word found (grid or bonus): **+2 points**
- Level completion bonus: **+10 points**

### Hints

- Start with **5 hints** per game
- Hints reveal a random unrevealed letter on the grid (free to use)
- Earn **+1 hint** for every **10 words** found (tracked by the progress bar)
- Hint count and progress carry across levels

## Requirements

- [Rust](https://www.rust-lang.org/tools/install) (1.70+)
- macOS, Windows, or Linux

## Run (Desktop)

```bash
cargo run
```

## Build (Desktop)

```bash
# Debug
cargo build

# Release (optimized)
cargo build --release
```

The release binary will be at `target/release/word_wheel_crossword`.

## Build (Android APK)

Android builds are automated via GitHub Actions. On every push or PR to `main`, the workflow builds a debug APK.

**To download the APK:**
1. Go to the [Actions](../../actions) tab
2. Click the latest "Build Android APK" workflow run
3. Scroll to **Artifacts** and download `word-wheel-debug-apk`
4. Sideload the APK on your Android device (enable "Install from unknown sources")

**To build locally** (requires Android SDK/NDK):
```bash
# Install targets and tools
rustup target add aarch64-linux-android
cargo install cargo-ndk

# Build native library
cargo ndk -t arm64-v8a --platform 26 \
  -o android/app/src/main/jniLibs \
  build --release --lib --no-default-features --features android

# Build APK (requires Gradle + Android SDK)
cd android && ./gradlew assembleDebug
```

## Project Structure

```
src/
  lib.rs          # Game logic, UI rendering, Android entry point
  main.rs         # Desktop entry point (thin wrapper)
assets/
  background.jpg  # Background image
android/          # Android Gradle project
  app/
    build.gradle
    src/main/
      AndroidManifest.xml
.github/
  workflows/
    android.yml   # CI/CD for APK builds
check_levels.py   # QA script to validate crossword grids
```

## Levels

10 levels with increasing difficulty. Each level uses a "spine + branches" crossword pattern where a main word runs across and branch words extend downward, all sharing intersecting letters.

| Level | Letters | Grid Words | Bonus Words |
|-------|---------|-----------|-------------|
| 1     | C A T S | 3         | 4           |
| 2     | S P I N E | 4       | 8           |
| 3     | H A S T E | 4       | 8           |
| 4     | W A R M S | 4       | 8           |
| 5     | C A R E S | 4       | 9           |
| 6     | G R I N D S | 5     | 7           |
| 7     | P L A N E T S | 5   | 8           |
| 8     | C R A N E S D | 6   | 7           |
| 9     | S T O R E D | 5     | 8           |
| 10    | T R A I N S E | 6   | 8           |

## QA Validation

Run the grid validation script to verify all crossword layouts:

```bash
python3 check_levels.py
```

This checks that every contiguous sequence of filled cells in rows and columns forms a valid English word.

## License

MIT
