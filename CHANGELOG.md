# Changelog

Notable changes to RadioTune. Versions follow the git tags (`vX.Y.Z`); the
Android `versionCode` is derived as `major * 10000 + minor * 100 + patch`.

## [1.7.2] - 2026-09-19

### Fixed

- The landscape **SCAN** knob only worked once. Station list responses are cached
  for ten minutes, and the random-station query matched that rule, so every scan
  after the first was served the same cached response. The one-shot tuning request
  now bypasses the HTTP cache.

## [1.7.1] - 2026-09-19

### Fixed

- The landscape player screen was double-padded: the shell applied safe-drawing
  insets and `PlayerScreen` applied them again. The shell now insets only the list
  routes, and the landscape player uses the same 16 dp content padding as the
  lists.
- The landscape navigation rail no longer sits under the status bar.

### Changed

- List screens collapse their app bar while scrolling; the search field goes with
  it unless the user is searching (focused or with a query).
- The landscape cassette is slightly smaller with a wider controls pane, so the
  two sides balance.

## [1.7.0] - 2026-09-19

### Added

- Landscape browsing: navigation moves to a side rail and the mini player docks at
  the bottom of the content, so the lists keep the full height. Countries,
  stations and favourites now lay out in adaptive columns — one in portrait, two
  or three in landscape.

### Changed

- Landscape now-playing polish: the redundant station name left the top bar (back
  on the left, style on the right), the cassette is smaller and centred, and the
  volume knob now turns — the pointer follows your finger around the dial, whose
  gap moved to the bottom where a real knob has it. The tuning knob reads SCAN.

## [1.6.0] - 2026-09-19

### Added

- **Landscape now-playing screen**: the cassette on the left and a working retro
  radio chassis on the right — power, play/pause, favourite, a cycling sleep
  control, a tuning knob that scans to another station in the same country, and a
  volume knob (vertical drag). The dial window shows the station and a needle at a
  stable pseudo-frequency.

### Changed

- Power is now a real toggle: stopping keeps the last station, so the radio can be
  switched back on from either orientation.

## [1.5.1] - 2026-09-19

### Changed

- The station logo on the cassette is now a small circular badge in the label's
  top-left corner instead of a large badge across the top, and the tape window
  and spools moved back to their previous position.

## [1.5.0] - 2026-09-19

### Changed

- All player styles now share the semi-realism cassette silhouette: a large
  chamfered sticker label with the tape window cut into it, ribbed edges and a
  moulded bottom plate. Classic, Minimal and Realism differ only by palette and
  finishes — warm cream, flat theme colours and dark moulded plastic.
- The station logo is now a rounded badge centred at the top of the label rather
  than filling it, and it also shows on the printed-band style. The tape window
  and spools moved down slightly to make room; on semi-realism the logo lands in
  the cream band and the window in the orange one.

## [1.4.2] - 2026-09-19

### Added

- **Semi-realism** player style: navy ribbed shell with a large chamfered sticker
  label printed in colour bands, the tape window cut into the label, cross-head
  screws, a moulded bottom plate and a worn speckle finish.

### Removed

- **Brutalist** player style.

### Changed

- Player styles are now fully data-driven: cassette geometry (label size and
  chamfer, window, spools, hub) and the detail switches are per-style fields on
  `CassetteLook`, so adding a style no longer touches the renderer.
- A preference naming a style that no longer exists falls back to Classic instead
  of failing.

## [1.4.1] - 2026-09-19

### Changed

- **Realism** style reworked to look like real hardware: dark moulded plastic lit
  from the top-left, a stepped shell (body, beveled rim, raised face plate), a
  broad specular highlight, and occlusion shading around the label, inside the
  tape window and inside the recessed spool wells.

### Fixed

- The tape is now visible through the tape window. It was drawn before the
  window's opaque recess, which painted over it in every style with a window
  (Classic, Brutalist and Realism).

## [1.4.0] - 2026-09-19

### Added

- **Realism** player style: moulded grey plastic with a three-stop gradient, a
  moulded inner lip, a specular streak across the shell, a reflection on the tape
  window and a highlight along the wound tape edge — on a neutral studio backdrop
  with graphite controls.

### Changed

- `CassettePlayer` now draws in two layers (static shell + animated reels) so the
  detailed shading of the heavier styles is not re-rendered on every frame.

## [1.3.0] - 2026-09-19

### Added

- Selectable player styles: **Classic** (retro cassette deck), **Minimal** and
  **Brutalist**. A style skins the whole now-playing screen — backdrop, top bar,
  play/favourite/sleep and stop controls, the sleep dialog and the cassette —
  while lists, mini player and navigation stay on the Material theme.
- Style picker with live animated previews, opened from the palette action on
  the now-playing screen. The choice is remembered.

### Changed

- The cassette is now drawn from a style-driven `CassetteLook` instead of
  reading the Material theme directly.

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
