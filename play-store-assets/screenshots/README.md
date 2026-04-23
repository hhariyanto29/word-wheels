# Screenshot capture guide

Play Store needs **minimum 2** phone screenshots, **maximum 8**. Dimensions
must be between 320 px and 3840 px per side, 16:9 or 9:16 ratio.

The simplest approach: install the release APK on a real Android device
or emulator, play through a level, and screenshot natural moments.

---

## Option A — Capture from a real device (best quality)

1. Install the debug APK on your phone:
   ```
   adb install android-native/app/build/outputs/apk/debug/app-debug.apk
   ```
2. Open the game, play to an interesting state (e.g. mid-level with
   2-3 words found, wheel highlighted, some hints used).
3. Press Power + Volume-Down to capture.
4. Pull the screenshots from the device:
   ```
   adb pull /sdcard/Pictures/Screenshots/ ./
   ```
5. Drop the best 4–8 shots into this folder.

---

## Option B — Capture from an Android emulator

1. In Android Studio → Device Manager → pick **Pixel 7** (1080×2400, 9:20) or
   **Pixel Fold** for a larger screen.
2. Install the APK via drag-and-drop onto the emulator window.
3. Play the game, then use the emulator's camera icon in the sidebar
   to save screenshots.

---

## Option C — Generate synthetic screenshots (fallback)

If you don't have a device or emulator yet, you can generate
programmatic mockups via Pillow. See `generate-mockups.py` in this
folder for a starter script. **Real device screenshots look much
better and convert better on Play Store** — treat mockups as a
temporary placeholder only.

---

## Suggested shot list (8 total slots on Play Store)

| Slot | Level | What to show | Why it helps the listing |
|---|---|---|---|
| 1 | Level 1 (CATS, 5×5) | Fresh start, tiles visible, empty grid | "Easy to pick up" |
| 2 | Level 2 or 3 | Mid-drag: selection line visible between 3 letters | Shows the core mechanic |
| 3 | Level 5 (CARES) | 2–3 words found, grid partially filled | Progress visible |
| 4 | Any | Hint button glowing, one cell revealed | "Stuck? Get a hint" |
| 5 | Level 7 (PLANETS) | Shows 8×8 grid — shows the game scales up | Depth/progression |
| 6 | Any complete level | Completion dialog visible | The payoff |
| 7 | Landscape orientation | Split layout (grid left, wheel right) | Responsive design |
| 8 | Status bar hidden, clean background | Hero shot | Store thumbnail |

---

## Specs Play Store will accept

| Attribute | Requirement |
|---|---|
| Format | JPEG or 24-bit PNG (no alpha) |
| Dimensions | ≥ 320 px, ≤ 3840 px on any side |
| Aspect ratio | Between 16:9 and 9:16 |
| Max file size | 8 MB each |

**Recommended:** 1080×1920 (phone portrait) or 1600×2560 (phone/tablet).
Native device screenshots from Pixel 7 (1080×2400) are automatically
valid.

---

## After capturing

Drop your screenshots into this folder, then in Play Console →
**Main store listing → Phone screenshots**, upload them in the
desired display order (Play shows them left-to-right as you drag them).
