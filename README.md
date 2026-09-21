# RadioTune

Native Android internet radio player. Browse stations by country, keep listening
in the background, and fall asleep to a timer.

Small and dependency-light: ~2.2 MB release APK, Kotlin + Jetpack Compose,
Media3/ExoPlayer, no Hilt/Room/KSP.

## Features

- Browse stations by country, search, filter by genre/tag, and sort by popularity,
  bitrate, codec or name — paginated lists via the
  [Radio Browser](https://www.radio-browser.info/) API
- Background playback with a media notification and audio focus handling
- Now-playing track titles from ICY metadata, with automatic stream recovery
- A landscape retro-radio layout with working tuning and volume knobs
- Side-rail navigation and adaptive list columns in landscape
- Station artwork, and an animated cassette on the now-playing screen
- Four player styles (Classic, Minimal, Realism, Semi-realism) with live previews,
  plus a **Radio** style — a walnut cabinet with a speaker grille, cream dial and
  brass trim
- **Photo mode**: long-press the power knob in landscape to go fullscreen — no
  bars, no chrome, no cassette, just the radio body, kept awake for a photo
- Sleep timer with a gentle fade-out
- Favorites stored on device, with reorder, search and JSON export/import
- In-app update check against the latest GitHub release (no account, no token)

## Download

Get the latest APK from the
[Releases](https://github.com/Drzaln/radio-tune/releases/latest) page.

`minSdk 24` (Android 7.0+).

## Build

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # release APK (unsigned without a keystore)
```

Signed release build:

```bash
KEYSTORE_FILE=/path/release.keystore \
KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... \
./gradlew :app:assembleRelease -PversionName=1.2.3 -PversionCode=10203
```

Output: `app/build/outputs/apk/release/`.

## Releases

Pushing a `vX.Y.Z` tag triggers `.github/workflows/release.yml`, which derives
the version from the tag, builds a signed APK, and publishes it (plus a stable
`RadioTune-latest.apk`) as a GitHub Release. See [CHANGELOG.md](CHANGELOG.md).

Signing uses the repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, and `KEY_PASSWORD`. Without them the workflow falls back to a
throwaway keystore, which produces an installable APK that cannot update a
build signed with another key.

## License

No license declared yet.
