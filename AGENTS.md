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
kotlinx.serialization · DataStore · Coil 3 (station favicons) · manual DI
(`AppContainer`), no Hilt/Room/KSP.

- `minSdk 24`, `targetSdk 36`, `compileSdk 36`
- AGP 8.13.2 / Gradle 8.13. Do **not** bump AndroidX to versions requiring
  AGP 9.1+ / compileSdk 37 (core-ktx 1.18+, compose BOM 2026.x, lifecycle 2.10+,
  media3 1.11+). Keep the versions in `gradle/libs.versions.toml`.
- Release APK is ~2.4 MB. Keep it small: XML vectors for icons (the cassette is
  drawn with `Canvas`, no assets), Coil for station favicons, no extra DI/KSP/Room
  libraries, R8 + resource shrinking on.
- Do **not** bump Coil above `3.4.0`: 3.5+ pulls Compose Multiplatform 1.11/1.12,
  which requires AGP 9.1+ / compileSdk 37.

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
- Player artwork: `ui/components/CassettePlayer.kt` — Canvas cassette whose reels
  spin on play (freeze in place on pause); the station logo is a small circular
  badge in the label's top-left corner. Favicons
  load through Coil in `StationAvatar` (`AsyncImage` over the vector fallback).
- Player styles: `ui/theme/PlayerStyle.kt` — `PlayerStyle` (CLASSIC, MINIMAL,
  REALISM, SEMI_REALISM) → `PlayerSkin` (backdrop, surfaces, content/muted/accent
  colours, control + panel shapes, border width, shadow, status-bar contrast) and
  `CassetteLook` (everything the cassette draws: geometry fractions, detail
  switches, and the optional `shellMid`/`sheen`/`bevel`/`glass`/`specular`/
  `occlusion` shading plus the `steppedShell` moulded-rim renderer).
  All styles share the default `CassetteLook` silhouette — a large chamfered
  sticker label with the tape window cut into it, ribbed edges and a moulded
  bottom plate — and differ only by palette, geometry overrides and finishes.
  `CassettePlayer` takes a
  `CassetteLook`; it must not read `MaterialTheme` directly. A style owns the whole
  now-playing screen — backdrop, top bar, play/favourite/sleep/stop and the sleep
  dialog — but nothing else: lists, mini player and nav stay on the Material theme.
  Styles with fixed palettes intentionally ignore dynamic colour on that screen.
- `CassettePlayer` draws in two layers on purpose: a static shell Canvas and a reels
  Canvas that reads the animated angle. Keep it split — otherwise every style's
  shading is re-rendered on each animation frame.
- Style preference: `SettingsRepository` (stores the enum name) → `PlayerViewModel`
  → picker dialog with live animated previews on the player screen.
- Landscape now-playing: `ui/player/RadioLandscape.kt` — cassette on the left and a
  working retro radio chassis on the right (power, play/pause, favourite, sleep
  cycling, tuning scan, volume). Orientation is decided in `PlayerScreen` with
  `maxWidth > maxHeight`. Both layouts share one `PlayerActions` bundle.
- Power is a real toggle: `PlayerController.stop()` keeps `lastStation` so the
  radio can be switched back on; `PlayerViewModel.scan()` plays a random station
  from the same country (`RadioRepository.randomStation`, which bypasses the list
  cache), and volume goes through `PlayerController.setVolume`.

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

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:46cd31e7 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/core-concepts/sync-concepts.md for details and anti-patterns.

## Agent Context Profiles

The managed Beads block is task-tracking guidance, not permission to override repository, user, or orchestrator instructions.

- **Conservative (default)**: Use `bd` for task tracking. Do not run git commits, git pushes, or Dolt remote sync unless explicitly asked. At handoff, report changed files, validation, and suggested next commands.
- **Minimal**: Keep tool instruction files as pointers to `bd prime`; use the same conservative git policy unless active instructions say otherwise.
- **Team-maintainer**: Only when the repository explicitly opts in, agents may close beads, run quality gates, commit, and push as part of session close. A current "do not commit" or "do not push" instruction still wins.

## Session Completion

This protocol applies when ending a Beads implementation workflow. It is subordinate to explicit user, repository, and orchestrator instructions.

1. **File issues for remaining work** - Create beads for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Handle git/sync by active profile**:
   ```bash
   # Conservative/minimal/default: report status and proposed commands; wait for approval.
   git status

   # Team-maintainer opt-in only, unless current instructions forbid it:
   git pull --rebase
   bd dolt push
   git push
   git status
   ```
5. **Hand off** - Summarize changes, validation, issue status, and any blocked sync/commit/push step

**Critical rules:**
- Explicit user or orchestrator instructions override this Beads block.
- Do not commit or push without clear authority from the active profile or the current user request.
- If a required sync or push is blocked, stop and report the exact command and error.
<!-- END BEADS INTEGRATION -->

<!-- BEGIN BEADS CODEX SETUP: generated by bd setup codex -->
## Beads Issue Tracker

Use Beads (`bd`) for durable task tracking in repositories that include it. Use the `beads` skill at `.agents/skills/beads/SKILL.md` (project install) or `~/.agents/skills/beads/SKILL.md` (global install) for Beads workflow guidance, then use the `bd` CLI for issue operations.

### Quick Reference

```bash
bd ready                # Find available work
bd show <id>            # View issue details
bd update <id> --claim  # Claim work
bd close <id>           # Complete work
bd prime                # Refresh Beads context
```

### Rules

- Use `bd` for all task tracking; do not create markdown TODO lists.
- Run `bd prime` when Beads context is missing or stale. Codex 0.129.0+ can load Beads context automatically through native hooks; use `/hooks` to inspect or toggle them.
- Keep persistent project memory in Beads via `bd remember`; do not create ad hoc memory files.

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/core-concepts/sync-concepts.md for details and anti-patterns.
<!-- END BEADS CODEX SETUP -->
