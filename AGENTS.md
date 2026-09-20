# AGENTS.md

Guidance for agents working in this repository.

## Project

**RadioTune** — native Android internet radio player.

- Browse stations by country (Radio Browser API), search, paginated lists
- Background playback via Media3 `MediaSessionService` + media notification
- Sleep timer with fade-out, enforced in the service
- Favorites persisted locally with DataStore
- In-app update check against the latest GitHub release (no API token)

## Stack

Kotlin 2.4.20 · Jetpack Compose (Material3) · Media3 1.8.1 · Retrofit + OkHttp +
kotlinx.serialization · DataStore · manual DI (`AppContainer`), no Hilt/Room/KSP.

- `minSdk 24`, `targetSdk 36`, `compileSdk 36`
- AGP 8.13.2 / Gradle 8.13. Do **not** bump AndroidX to versions requiring
  AGP 9.1+ / compileSdk 37 (core-ktx 1.18+, compose BOM 2026.x, lifecycle 2.10+,
  media3 1.11+). Keep the versions in `gradle/libs.versions.toml`.
- Release APK is ~2.2 MB. Keep it small: XML vectors only, no image loader,
  no extra DI/KSP libraries, R8 + resource shrinking on.

## Build & verify

```bash
./gradlew :app:assembleDebug                 # debug APK
./gradlew :app:assembleRelease               # unsigned locally if no keystore
./gradlew :app:compileDebugKotlin            # fast compile check
```

Signed release build (used by CI):

```bash
KEYSTORE_FILE=/path/release.keystore \
KEYSTORE_PASSWORD=... KEY_ALIAS=... KEY_PASSWORD=... \
./gradlew :app:assembleRelease -PversionName=1.2.3 -PversionCode=10203
```

Output: `app/build/outputs/apk/release/app-release.apk`.

There is no unit-test suite. Verify changes by building only.

**Never test with an emulator or device.** Do not boot an AVD, install an APK,
or drive the UI with `adb`. Compile checks (`assembleDebug` / `compileDebugKotlin`)
plus CI are the verification path.

## Architecture map

```
data/
  remote/        RadioBrowserApi (Retrofit), NetworkFactory, DTOs
  repository/    RadioRepository (network + in-memory cache), FavoritesRepository
  local/         DataStore preferences
  model/         Station, Country
  update/        UpdateChecker, ApkDownloader, ApkInstaller, UpdateRepository
playback/
  RadioPlayerService   MediaSessionService + ExoPlayer + sleep timer
  PlayerController     app-scoped MediaController bridge, exposes PlayerUiState
  PlayerCommands       custom session commands (set/cancel sleep timer)
  StationMediaItem     Station <-> MediaItem (station embedded in metadata extras)
di/AppContainer        manual dependency container
ui/
  AppRoot              Scaffold, bottom nav, mini player, NavHost, routes
  AppViewModelProvider single ViewModelProvider.Factory
  countries/ stations/ favorites/ player/ components/ theme/
```

Key files and tuning points:

- API host: `data/remote/NetworkFactory.kt` → `RemoteConfig.BASE_URL`
- Cache TTL: `data/repository/RadioRepository.kt` (companion constants)
- Sleep presets: `ui/player/PlayerScreen.kt` → `SleepTimerOptions`
- Radio Browser click/stream resolution: `RadioRepository.registerClick`
- Update source + asset names: `data/update/UpdateConfig.kt`
- Update check throttle: `data/update/UpdateRepository.kt` (`CHECK_INTERVAL_MS`, 12h)
- Playlist (.pls/.m3u) resolution: `RadioRepository.resolvePlayableUrl`

## Playback notes

Many Radio Browser entries are HLS (`.m3u8`), and `media3-exoplayer` does **not**
bundle HLS — the `media3-exoplayer-hls` dependency is required. Without it those
stations fail to play. Radio Browser also reports `codec = "UNKNOWN"` for exactly
those streams; `Station.qualityLabel` hides it instead of rendering "UNKNOWN".

Entries whose `url_resolved` is a `.pls`/`.m3u` playlist are resolved to their
first stream URL before playback (`RadioRepository.resolvePlayableUrl`), since
ExoPlayer cannot open those playlist containers.

## In-app updates

No `api.github.com` and no token. `UpdateChecker` issues a `HEAD`-style GET to
`https://github.com/<owner>/<repo>/releases/latest` with redirects disabled and
reads the `Location` header (`/releases/tag/vX.Y.Z`) to learn the version. The
APK is fetched from `.../releases/latest/download/RadioTune-latest.apk`, falling
back to the versioned `RadioTune-<version>.apk` for older releases.

The version check maps to the workflow's scheme: `1.2.3` → `10203`.

Install hand-off uses `FileProvider` (`${applicationId}.fileprovider`,
`res/xml/file_paths.xml`) plus `REQUEST_INSTALL_PACKAGES`. The user still confirms
the install, and enables "install unknown apps" the first time. An update prompt
appears on launch (throttled); the Countries top bar has a manual check button.

## CI / release

`.github/workflows/release.yml` triggers on `v*` tags and on manual
`workflow_dispatch` (with a `version` input).

- Version is derived from the tag: `v1.2.3` → `versionName=1.2.3`,
  `versionCode=10203`. Gradle also accepts `-PversionName` / `-PversionCode`.
- Signs with repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`. Falls back to a throwaway keystore (with a warning) when
  `KEYSTORE_BASE64` is absent.
- Publishes a GitHub Release with two assets: the versioned
  `RadioTune-X.Y.Z.apk` and the stable `RadioTune-latest.apk` used by the
  in-app updater.

Do not poll the GitHub API for run status; the user checks the Actions tab.

## Ship it

When the user says **"ship it"**, do all of the following, in order:

1. **Bump version** in `app/build.gradle.kts` (`versionCode` / `versionName`
   defaults) — keep them in sync with the new tag.
2. **Update docs** — `README.md` (if present) and `CHANGELOG.md` (create or
   append a version section).
3. **Reindex** the repository with the codebase-memory MCP tools.
4. **Create the release tag** `vX.Y.Z` (annotated, `-m "RadioTune X.Y.Z"`).
5. **Push** `main` and the tag to `origin`.

Do not query the GitHub Actions API afterwards.

## Conventions

- No code comments unless they explain non-obvious intent.
- Follow existing package layout and Compose/Material3 patterns.
- Prefer editing existing files; keep dependencies and APK size minimal.
- Never commit keystores, secrets, or `local.properties` (see `.gitignore`).
