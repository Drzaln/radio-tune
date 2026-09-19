# AGENTS.md

Guidance for agents working in this repository.

## Project

**RadioTune** — native Android internet radio player.

- Browse stations by country (Radio Browser API), search, paginated lists
- Background playback via Media3 `MediaSessionService` + media notification
- Sleep timer with fade-out, enforced in the service
- Favorites persisted locally with DataStore

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

There is no unit-test suite. Verify changes by building and, when relevant,
by installing on an emulator/device and exercising the flow.

## Architecture map

```
data/
  remote/        RadioBrowserApi (Retrofit), NetworkFactory, DTOs
  repository/    RadioRepository (network + in-memory cache), FavoritesRepository
  local/         DataStore preferences
  model/         Station, Country
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

## CI / release

`.github/workflows/release.yml` triggers on `v*` tags and on manual
`workflow_dispatch` (with a `version` input).

- Version is derived from the tag: `v1.2.3` → `versionName=1.2.3`,
  `versionCode=10203`. Gradle also accepts `-PversionName` / `-PversionCode`.
- Signs with repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`. Falls back to a throwaway keystore (with a warning) when
  `KEYSTORE_BASE64` is absent.
- Publishes a GitHub Release with the APK attached.

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
