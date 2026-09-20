# Changelog

Notable changes to RadioTune. Versions follow the git tags (`vX.Y.Z`); the
Android `versionCode` is derived as `major * 10000 + minor * 100 + patch`.

## [1.2.1] - 2026-09-19

### Changed

- Redrew the now-playing cassette in classic Compact Cassette proportions:
  hairline outlines, a proper label sticker, thinner spool rings with a small
  3-slot hub, a tape path running under the reels, and the head opening at the
  bottom edge. The chunky tape bar and corner screws are gone.

## [1.2.0] - 2026-09-19

### Added

- Animated cassette on the now-playing screen: the reels spin while playing,
  slow down while buffering, and freeze in place when paused.
- Station artwork (favicons) in the station lists, the mini player, and the
  cassette label, falling back to the vector radio icon.
- Directional navigation transitions: drill-downs slide in from the right, the
  player slides up as a modal, and the bottom-nav tabs cross-fade.

### Changed

- Navigation no longer uses the navigation-compose 700 ms default cross-fade.

## [1.1.2] - 2026-09-19

### Fixed

- HLS (`.m3u8`) stations — BBC Radio 4, France Inter, RTL and friends — now play.
  `media3-exoplayer` does not bundle HLS, so `media3-exoplayer-hls` is now a
  dependency.
- The Radio Browser `UNKNOWN` codec placeholder is no longer rendered as a
  quality label on HLS stations.
- `.pls`/`.m3u` playlist entries are resolved to their first stream URL before
  playback.

## [1.1.1] - 2026-09-19

### Changed

- Release builds are now signed with the repository's release keystore
  (configured through Actions secrets), so in-app updates install over an
  existing build. First release with a stable, reused signing key.

## [1.1.0] - 2026-09-19

### Added

- In-app update check against the latest GitHub release. The newest tag is read
  from GitHub's `/releases/latest` web redirect (no `api.github.com`, no token),
  the APK is downloaded from the release, and the system installer is opened.
- Update prompt on launch, throttled to once every 12 hours, plus a manual
  check action in the Countries top bar.
- Release workflow now also publishes a stable `RadioTune-latest.apk` asset for
  the updater, alongside the versioned `RadioTune-X.Y.Z.apk`.

## [1.0.0] - 2026-09-19

### Added

- Browse radio stations by country, search, and paginated station lists powered
  by the Radio Browser API.
- Background playback through a Media3 `MediaSessionService` with a media
  notification, audio focus handling, and pause on headphone disconnect.
- Sleep timer with a fade-out, enforced in the player service.
- Favorites persisted locally with DataStore.
- Release workflow that builds a signed APK from a version tag and publishes it
  as a GitHub Release.
