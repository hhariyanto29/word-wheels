# Word Wheel — iOS Native

Pure SwiftUI port of the game (companion to `android-native/`). All gameplay,
levels, sound effects, and the HOPEWELL background image match the Android
build byte-for-byte.

## Requirements

- macOS with Xcode 15.4+
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) for project generation:
  `brew install xcodegen`

## Generate project & open in Xcode

```bash
cd ios-native
xcodegen generate
open WordWheel.xcodeproj
```

`WordWheel.xcodeproj` is not checked in — `project.yml` is the source of
truth, so the project file is always regenerable and git diffs stay clean.

## Build targets

- **iOS 16.0+** (iPhone + iPad, portrait & landscape)
- Universal binary via Xcode's standard "Any iOS Device" destination
- No Swift packages, no CocoaPods — pure Apple SDK

## Build & run

- **Simulator:** open in Xcode, pick any iPhone simulator, hit ⌘R
- **Physical device:** requires signing — set `DEVELOPMENT_TEAM` in
  `project.yml` to your team ID, regenerate, then build
- **CI:** GitHub Actions builds a simulator `.app` bundle on every push
  (see `.github/workflows/ios.yml`)

## Project structure

```
ios-native/
├── project.yml                    # XcodeGen spec
└── WordWheel/
    ├── WordWheelApp.swift         # @main entry
    ├── Info.plist
    ├── Models/
    │   ├── Level.swift            # 10 puzzles + bonus words
    │   └── GameState.swift        # ObservableObject with scoring/hints
    ├── Audio/
    │   └── SoundManager.swift     # AVAudioPlayer pool (3× concurrency)
    ├── Theme/
    │   └── GameColors.swift
    ├── Views/
    │   ├── GameScreen.swift       # root, portrait + landscape layouts
    │   ├── LetterWheel.swift      # drag-select wheel
    │   ├── CrosswordGrid.swift
    │   ├── TopBar.swift
    │   ├── BottomButtons.swift
    │   └── CompletionDialog.swift
    ├── Resources/
    │   └── Sounds/                # 8 WAV files (synced with Android)
    └── Assets.xcassets/
        ├── AppIcon.appiconset/
        ├── AccentColor.colorset/
        └── GameBackground.imageset/
            └── game_background.jpg
```

## Parity with Android

| Area | iOS | Android |
|---|---|---|
| UI framework | SwiftUI | Jetpack Compose |
| State | `@StateObject` + `@Published` | `mutableStateOf` / `mutableStateListOf` |
| Audio | `AVAudioPlayer` pool | `SoundPool` |
| Image | `UIImage` asset catalog | `res/drawable` |
| Gestures | `DragGesture` + `onTapGesture` | `detectDragGestures` + `detectTapGestures` |
| Canvas | SwiftUI `Path` / `Circle` | Compose `Canvas` |

Both builds read the same `assets/` and logic files so there's one source of
truth for levels, words, and sound filenames.
