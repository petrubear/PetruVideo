# Quickstart: Publish the APK to GitHub Releases

## One-time setup (maintainer)

1. Create the release key (keep a safe copy; losing it means users must reinstall): `keytool -genkeypair -v -keystore petruvideo-release.jks -alias petruvideo -keyalg RSA -keysize 4096 -validity 10000`.
2. Add four repository secrets (Settings, Secrets and variables, Actions): `RELEASE_KEYSTORE_BASE64` (`base64 -i petruvideo-release.jks | pbcopy`), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

## Local checks

- `scripts/test-changelog-notes.sh` (the CHANGELOG reader)
- Local signed release build with a throwaway key: set the four `RELEASE_*` variables, run `./gradlew :app:assembleRelease`, then `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
- Install that file on a phone or emulator, open it, play a project, export a video

## On GitHub

1. Push the tag `v0.5.1` (or start the "Release" workflow by hand with a tag): the release appears with the APK, the checksum and the notes.
2. Start the workflow again for the same tag: still one release.
3. Start it with a tag that does not exist, or with a tag that does not match the app version: it stops with a clear message and no release.
4. Remove one secret and start it: it stops naming the missing setting.

## Expected outcome

The Releases page has one release per tag with a file that installs over the previous one.
