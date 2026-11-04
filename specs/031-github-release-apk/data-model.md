# Data Model: Publish the APK to GitHub Releases

No app data. The things that exist only in the repository and on GitHub:

| Item | Where | Notes |
|---|---|---|
| Version | `appVersion` in `app/build.gradle.kts` | Must equal the tag without the `v` |
| Release notes | `## [X.Y.Z]` section of `CHANGELOG.md` | Copied unchanged |
| Signing key | repository secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Set once by the maintainer; never in the repository or logs |
| Release | GitHub release named `vX.Y.Z` | One per tag; file `PetruVideo-X.Y.Z.apk` and `PetruVideo-X.Y.Z.apk.sha256` |
