# Play Store Submission Guide — Word Wheel

End-to-end walkthrough for submitting Word Wheel from "app entry created"
to "Production release rolled out". Designed to be followed in one sitting.

**Last updated:** 2026-04-30
**App package name:** `com.wordwheel.game`
**Target version:** 1.7.3 (versionCode 11)

---

## Pre-flight checklist

Before opening Play Console, confirm all of this is in place:

- [x] Production AAB available locally at `/tmp/aab-prod/app-release.aab` (4.2 MB)
- [x] AAB signed with production key — SHA-1 `57:52:28:8B:DB:86:65:9A:71:F7:A5:17:53:6C:F2:9E:7F:25:5B:49`
- [x] Upload key kept offline at `~/Documents/keystore-backup/wordwheel-upload.jks`
- [x] App entry exists in Play Console (`com.wordwheel.game`)
- [x] Google App Signing key generated (deployment cert downloaded)
- [x] Upload key slot is EMPTY in Play Console — first upload will register ours
- [x] Privacy policy live at `https://hhariyanto29.github.io/word-wheels/privacy-policy/`
- [x] Website live at `https://hhariyanto29.github.io/word-wheels/`
- [x] Listing copy ready in `play-store-assets/store-listing.md`
- [x] Content rating answers ready in `play-store-assets/content-rating.md`
- [x] Release notes ready in `play-store-assets/release-notes.md` (v1.7.3 entry)
- [x] App icon `play-store-assets/app-icon-512.png` (512×512)
- [x] Feature graphic `play-store-assets/feature-graphic-1024x500.png` (1024×500)
- [x] 3 phone screenshots in `play-store-assets/screenshots/` (1080×1920)

---

## Phase A — Set up the app shell

If you've already done these in Play Console, skip to Phase B.

### A1. Confirm the app entry

1. Open https://play.google.com/console
2. Pick the developer account → click the app **Word Wheel** in the list
3. The left sidebar will show: **Test and release**, **Monitor and improve**, **Grow**, **Monetize**, **Policy**, **Setup**

If the app isn't listed, click **Create app** and fill:
- App name: `Word Wheel`
- Default language: `English (United States) – en-US`
- App or game: **Game**
- Free or paid: **Free**
- Confirm declarations (developer guidelines + US export laws)

### A2. App access (do you need a login?)

`Policy → App content → App access` → **All functionality is available without special access**. Save.

### A3. Ads declaration

`Policy → App content → Ads` → **No, my app does not contain ads**. Save.

### A4. Content rating

`Policy → App content → Content ratings` → **Start questionnaire**

Email: your developer email
Category: **Games**

Then click through the IARC questionnaire. Use `play-store-assets/content-rating.md`
as the answer key — every answer is **No**. Submit.

You'll get rating certificates: ESRB **Everyone**, PEGI **3**, IARC **3+** etc.

### A5. Target audience and content

`Policy → App content → Target audience and content`

- Target age groups: tick `13+`, `15-17`, `18+` (NOT under 13 — keeps you out
  of the kids-app review track)
- Does your app appeal to children? **No**
- Does it contain ads? **No**

Save. Continue.

### A6. News app declaration

`Policy → App content → News app` → **My app is not a news app**. Save.

### A7. COVID-19 contact tracing

→ **My app is not a publicly available COVID-19 contact tracing or status app**. Save.

### A8. Data safety

`Policy → App content → Data safety` → **Start**

This form asks what user data the app collects. Word Wheel collects nothing,
so:

- Does your app collect or share any of the required user data types? **No**
- Is all of the user data collected by your app encrypted in transit?
  → Doesn't apply (no data collected). Pick the equivalent **Not collected** option.
- Do you provide a way for users to request that their data is deleted?
  → Doesn't apply.

Click through, save, **Submit**.

### A9. Government apps declaration

→ **My app is not a government app**. Save.

### A10. Financial features

→ **No financial features**. Save.

### A11. Health features

→ **No health features**. Save.

---

## Phase B — Main store listing

`Grow → Store presence → Main store listing`

### B1. App details

| Field | Value | Source |
|---|---|---|
| App name | `Word Wheel` | store-listing.md |
| Short description | (paste from store-listing.md) | store-listing.md §"Short description" |
| Full description | (paste from store-listing.md) | store-listing.md §"Full description" |

### B2. Graphics

Upload from `play-store-assets/`:

| Slot | File | Dimensions |
|---|---|---|
| App icon | `app-icon-512.png` | 512×512 |
| Feature graphic | `feature-graphic-1024x500.png` | 1024×500 |
| Phone screenshots | `screenshots/mockup-01-gameplay.png`<br>`screenshots/mockup-02-selection.png`<br>`screenshots/mockup-03-complete.png` | 1080×1920 each |

(7-inch and 10-inch tablet screenshots are optional — leave empty for first launch.)

### B3. Categorisation

| Field | Value |
|---|---|
| App category | **Games** |
| Game category | **Word** |
| Tags | Word, Puzzle, Brain Games, Casual, Offline (pick 5) |

### B4. Contact details

| Field | Value |
|---|---|
| Email | your public-facing email |
| Phone | optional, leave empty |
| Website | `https://hhariyanto29.github.io/word-wheels/` |
| Privacy policy URL | `https://hhariyanto29.github.io/word-wheels/privacy-policy/` |

### B5. External marketing

→ Leave default (opt-in or opt-out, your choice).

Click **Save** at the top of the page. The listing now shows `Active`.

---

## Phase C — Upload the AAB to Internal testing

We start with **Internal testing** because:
- Promotion to Production is one click later
- Internal testing is reviewed in minutes, not days
- You catch any policy/binary errors before the review queue starts

### C1. Set up Internal testing track

`Test and release → Testing → Internal testing → Create new release`

### C2. Confirm Play App Signing

If this is your first upload, Play Console will prompt:

> **Use Play App Signing** — Google manages your app signing key. Recommended.

→ **Continue / Use Play App Signing** (default).

(There's no need to download a "private key export" or upload an existing
app signing key. Just accept the default.)

### C3. Upload the AAB

Drag & drop `/tmp/aab-prod/app-release.aab` into the upload box.

While it processes (~30 seconds):

- Play Console verifies the AAB signature
- It registers your upload key SHA-1 (`57:52:28:8B:DB:86:65:9A:71:F7:A5:17:53:6C:F2:9E:7F:25:5B:49`)
  as the official upload key for this app
- It scans the AAB for policy violations (allow-list of permissions, etc.)

You'll see the AAB appear with version `1.7.3 (11)`.

### C4. Release name and notes

| Field | Value |
|---|---|
| Release name | auto-fills as `1.7.3 (11)` — leave it |
| Release notes (en-US) | paste from `play-store-assets/release-notes.md` §"v1.7.3" |

### C5. Save & review the release

Click **Next** → **Save** → **Review release**.

Play Console will show:
- Roll-out percentage (Internal testing has no rollout — it's all-or-nothing for testers)
- Any pre-launch warnings (typical: missing translations, permissions list, etc.)

Fix any **Errors** (red). **Warnings** (yellow) can usually stay.

### C6. Add internal testers (one-time setup)

`Test and release → Testing → Internal testing → Testers tab`

- Click **Create email list**
- Name: `Internal testers`
- Add your own email + 1-2 trusted friends/family
- Save

Back in the **Release** tab, link the email list to the track and
**Start rollout to Internal testing**.

### C7. Install on your device

After ~15-30 minutes:

1. Internal testers receive an opt-in URL via email
2. Click the link, accept the invitation
3. Open Play Store on the same Google account
4. Search "Word Wheel" → install

Confirm the app launches, levels load, sounds play, daily spin works,
home screen looks right.

If you find a critical bug → fix in code → push → CI builds new AAB →
upload again to Internal track. There is no review delay between
internal builds.

---

## Phase D — Promote to Production

Once internal testing passes (give yourself a day, run through 3-4 levels):

### D1. Open the Internal release

`Test and release → Testing → Internal testing` → click on the active
release → **... → Promote release → Production**

This copies the same AAB and release notes to the Production track.

### D2. Production rollout setup

| Field | Value |
|---|---|
| Countries | **Add countries / regions** → click **Select all 174 territories** |
| Roll-out percentage | **20%** (gradual) or **100%** (all at once) — see note below |
| Release notes | already inherited from internal release |

> **Gradual rollout (recommended)** — start at 20% so if there's a critical
> bug crashing Android 8 devices you only hit 20% of users before halting.
> Bump to 50%, then 100% over a few days as crash-free rate stays high.
> For first launch with low download volume, **100%** is fine too.

### D3. Submit for review

Click **Next** → **Save** → **Send for review**.

Pre-launch tasks Play Console will run:

1. **Bot checks** (binary scan, policy scan, age rating consistency) — ~10 min
2. **Human review** for first-time apps — typically **3-7 days** for
   first launch, faster for updates

While waiting:
- You'll see status `In review` then `Available on Google Play` once approved
- Play sends an email to your developer email at each stage

### D4. After approval

Listing goes live at:

```
https://play.google.com/store/apps/details?id=com.wordwheel.game
```

Share the URL anywhere — social media, friends, your GitHub README.

---

## Phase E — Future updates (cheat sheet)

Once you're approved and live, every future update is much faster:

```
1. Bump versionCode + versionName in android-native/app/build.gradle.kts
2. git push origin main
3. Wait for CI to publish a new AAB artifact (~3 min)
4. Download AAB from GitHub Actions → Run → Artifacts
5. Play Console → Production → Create new release → drag AAB
6. Paste new release notes
7. Submit for review

Update reviews are usually approved in 4-24 hours.
```

---

## Troubleshooting

### "You uploaded an APK or Android App Bundle that was signed in debug mode"
→ The CI didn't have signing secrets set, so Gradle fell back to debug
keystore. Re-check the four `UPLOAD_*` GitHub Secrets are present and
re-run the workflow.

### "Your Android App Bundle is signed with the wrong key"
→ The first AAB you uploaded was signed with a different key. The fix is
to submit an **upload key reset** request:
https://support.google.com/googleplay/android-developer/contact/key
Attach `~/Documents/keystore-backup/upload_certificate.pem`.

### "Action required: provide a privacy policy URL"
→ Paste `https://hhariyanto29.github.io/word-wheels/privacy-policy/` into
**Main store listing → Privacy policy URL** AND **Data safety form → Privacy
policy URL**. The two fields are separate.

### Pre-launch report flags 64-bit warnings
→ This shouldn't happen for our build (Compose pulls in 64-bit deps by
default), but if it does, add `ndk { abiFilters += listOf("arm64-v8a", "x86_64") }`
to `defaultConfig`.

### "App not installed as package appears to be invalid"
→ Specifically affects Xiaomi/MIUI, Samsung Secure Folder, older
file-manager installers when the AAB is v2/v3 signed but not v1. Our
build already enables all three (`enableV1Signing = true`, etc.) so this
should not happen.

---

## Useful links

| Purpose | URL |
|---|---|
| Play Console | https://play.google.com/console |
| Public listing (after approval) | https://play.google.com/store/apps/details?id=com.wordwheel.game |
| Privacy policy | https://hhariyanto29.github.io/word-wheels/privacy-policy/ |
| Website | https://hhariyanto29.github.io/word-wheels/ |
| Source repo | https://github.com/hhariyanto29/word-wheels |
| Upload key reset form | https://support.google.com/googleplay/android-developer/contact/key |
| Play Store review FAQ | https://support.google.com/googleplay/android-developer/answer/9859348 |
