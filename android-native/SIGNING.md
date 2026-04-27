# Production signing — upload key for Play Store

Word Wheel uses **Play App Signing**: Google holds the production
signing key and re-signs every release. Your job is to sign each
upload with a stable **upload key** that Play Console matches against
your enrollment record.

This doc covers:
1. Generating the upload keystore
2. Registering it as GitHub Secrets so CI can sign with it
3. What CI does with those secrets

If you skip this setup, CI silently falls back to the debug keystore.
That's fine for sideload testing but Play Console will warn you on
upload, and the cert subject (`CN=Android Debug`) is a giveaway.

---

## 1. Generate the upload keystore (do this ONCE, locally)

Run on your machine — never inside CI:

```bash
keytool -genkeypair \
  -alias wordwheel-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 9125 \
  -keystore wordwheel-upload.jks \
  -storepass <KEYSTORE_PASSWORD> \
  -keypass    <KEY_PASSWORD> \
  -dname "CN=Word Wheel, O=hhariyanto29, C=ID"
```

Pick strong, distinct passwords for `<KEYSTORE_PASSWORD>` and
`<KEY_PASSWORD>` (a password manager works). Validity is 25 years
(9125 days) — Play requires the upload key to outlast 22-Oct-2033.

**Critical:** back up `wordwheel-upload.jks` to two places at
minimum (1Password / encrypted drive / private repo). If you lose
this file AND the passwords, you can't ship updates without
contacting Google support to reset the upload key.

---

## 2. Encode the keystore for GitHub Secrets

GitHub Secrets are text, so the binary `.jks` needs to be base64-
encoded:

```bash
base64 -i wordwheel-upload.jks | tr -d '\n' > upload-keystore.base64.txt
```

Open `upload-keystore.base64.txt` in any text editor — copy the
whole contents.

---

## 3. Add four GitHub Secrets

Repo → **Settings → Secrets and variables → Actions → New repository secret**.

Add these four — names must match exactly:

| Secret name | Value |
|---|---|
| `UPLOAD_KEYSTORE_BASE64` | The base64 string from step 2 |
| `UPLOAD_KEYSTORE_PASSWORD` | The `<KEYSTORE_PASSWORD>` from step 1 |
| `UPLOAD_KEY_ALIAS` | `wordwheel-upload` |
| `UPLOAD_KEY_PASSWORD` | The `<KEY_PASSWORD>` from step 1 |

Once saved, GitHub redacts them — you can't view them again.

---

## 4. Verify CI signs with the upload key

Trigger any push to `main` or `claude/**`. In the Actions log for
**Build Android APK & AAB**:

- Step **Decode upload keystore** prints `✓ Upload keystore decoded
  and verified`. If it prints `ℹ No UPLOAD_KEYSTORE_BASE64
  secret...`, the secret name is wrong.
- Step **Verify APKs are signed** for the release APK should show a
  cert subject matching what you put in `-dname` (`CN=Word Wheel,
  O=hhariyanto29, C=ID`), not `CN=Android Debug`.
- The artifact `word-wheel-release-aab` is now suitable for direct
  upload to Play Console.

If you see the warning **"Release APK is signed with the DEBUG
keystore"**, one of the four secrets is missing or wrong.

---

## 5. First Play Console upload

1. Play Console → All apps → your app → **Production → Create new
   release**
2. **Enable Play App Signing** when prompted (one-time)
3. Upload `word-wheel-release-aab` from CI artifacts
4. Play Console matches the upload key fingerprint and accepts the
   build. Subsequent releases are matched to the same fingerprint.

Once enrolled, you can never change the upload key without going
through Google support — but the actual app signing key Google holds
is unaffected, so users still get updates seamlessly.

---

## Local builds (no secrets)

`./gradlew assembleRelease` on your machine works without any of the
above set up — the build sees no env vars and falls back to the debug
keystore. The resulting APK is fine for sideload but **never upload it
to Play Store** as a production release.
