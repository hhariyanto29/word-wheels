# Play Store submission assets

Everything you need to submit Word Wheel to the Google Play Store. Most
items here are text/images — copy/paste them into the matching fields
in Play Console.

## File map

| File | What it's for | Play Console field |
|---|---|---|
| `app-icon-512.png` | 512×512 solid-bg icon | Main store listing → App icon |
| `feature-graphic-1024x500.png` | Banner shown at the top of the store page | Main store listing → Feature graphic |
| `store-listing.md` | App name, short & long description, categories, contact | Main store listing → App name, descriptions |
| `content-rating.md` | Pre-filled answers for the IARC questionnaire | Policy → App content → Content ratings |
| `release-notes.md` | What's-new text for each release | Release → Production → Release notes |
| `screenshots/README.md` | How to capture phone screenshots | — |
| `screenshots/generate-mockups.py` | Fallback — generates placeholder screenshots | Main store listing → Phone screenshots |
| `screenshots/mockup-*.png` | Generated placeholders (1080×1920) | Main store listing → Phone screenshots |

## End-to-end submission checklist

### Pre-requisites
- [ ] **Google Play Developer account** — $25 one-time, done at https://play.google.com/console/signup
- [ ] **GitHub Pages enabled** — Repo Settings → Pages → Source: Deploy from branch → `main` + `/docs`. Verify https://hhariyanto29.github.io/word-wheels/privacy-policy/ loads.
- [ ] **Release keystore** — `keytool -genkey -v -keystore wordwheel-release.jks -alias wordwheel -keyalg RSA -keysize 2048 -validity 10000`. Back up somewhere safe (1Password, encrypted drive). **Losing it = can't ship updates, ever.**

### Create the Play Console app
- [ ] Play Console → All apps → **Create app**
  - App name: `Word Wheel`
  - Package name: `com.wordwheel.game`
  - Default language: English (US)
  - App or game: **Game**
  - Free or paid: **Free** (can't change later)
- [ ] Accept declarations

### App content (Policy tab — all of these are required)
- [ ] **Privacy policy** — paste GitHub Pages URL
- [ ] **App access** — "All functionality available without special access"
- [ ] **Ads** — "No, my app does not contain ads"
- [ ] **Content ratings** — fill questionnaire per `content-rating.md`
- [ ] **Target audience** — 13+ / General audience, no appeal to children
- [ ] **Data safety** — declare **No data collected, no data shared**
- [ ] **Government apps** — No
- [ ] **News apps** — No

### Main store listing
- [ ] App name / short description / full description — from `store-listing.md`
- [ ] App icon — upload `app-icon-512.png`
- [ ] Feature graphic — upload `feature-graphic-1024x500.png`
- [ ] Phone screenshots — minimum 2, upload from `screenshots/` folder
- [ ] (Optional) Tablet screenshots — if you have a tablet to capture from
- [ ] App category — Games / **Word**
- [ ] Tags — Word, Puzzle, Offline, Brain, Casual

### Build & upload
- [ ] Download latest **`word-wheel-release-aab`** from GitHub Actions
- [ ] Play Console → **Production** → Create new release
- [ ] Enable **Play App Signing** (Google manages the signing key)
- [ ] Upload the AAB
- [ ] Paste release notes from `release-notes.md`
- [ ] **Save draft** → **Review release**

### Pre-launch
- [ ] (Recommended) Set up **Internal testing track** first
  - Upload the same AAB there
  - Add tester email(s)
  - Verify install via the opt-in link on a real device
- [ ] Fix any warnings Play Console flags in the Review step
- [ ] **Start rollout to production** → phased rollout 20% → 50% → 100%

### Post-launch
- [ ] First review typically takes 1–7 days
- [ ] Monitor Play Console → Overview for any policy violations
- [ ] Set up **Play Console crash reports** (Quality → Android vitals)

## Quick reference

- Bundle ID: `com.wordwheel.game`
- Min SDK: 26 (Android 8.0 Oreo)
- Target SDK: 34 (Android 14)
- Current version: 1.3 (versionCode 4)
- Release AAB size: ~3.7 MB (download ≈ 1.9 MB per-device after Play shrinks it)

## Notes on signing

The current CI signs everything (debug + release APK + release AAB)
with the **debug keystore**. This is fine for sideloading, but Play
Store requires a consistent upload key. Two paths:

1. **Play App Signing** (recommended first-time): upload the debug-signed
   AAB for your first release. Play Console will ask you to "opt in to
   Play App Signing". Google then takes over signing key management,
   and you upload subsequent AABs signed with an **upload key** that
   Google generates for you. This is the safer path for indie devs —
   you can't lose the key because Google holds it.

2. **Self-managed keystore**: generate your own `.jks`, configure
   `android-native/app/build.gradle.kts` to use it, store credentials
   in GitHub Secrets, update the CI workflow to sign the release with
   those. More control, more responsibility.

If you want path 2, ping me and I'll wire up the Secrets-based signing
flow in the workflow.
