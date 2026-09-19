package com.rizal.radiotune.data.update

/**
 * Update discovery uses GitHub's public web endpoints only — no `api.github.com`,
 * so there is no token and no rate limit. `LATEST_RELEASE_URL` redirects to the
 * newest published tag, which gives us the version without parsing any page.
 */
object UpdateConfig {

    const val OWNER = "Drzaln"
    const val REPO = "radio-tune"

    const val LATEST_RELEASE_URL = "https://github.com/Drzaln/radio-tune/releases/latest"
    private const val RELEASES_BASE = "https://github.com/Drzaln/radio-tune/releases"

    /** Stable asset name uploaded by the release workflow for every version. */
    const val STABLE_ASSET_NAME = "RadioTune-latest.apk"

    /** Fallback asset name pattern, e.g. `RadioTune-1.2.3.apk` from older releases. */
    fun versionedAssetName(versionName: String) = "RadioTune-$versionName.apk"

    fun releasePageUrl(tag: String) = "$RELEASES_BASE/tag/$tag"

    fun downloadUrl(tag: String, assetName: String) = "$RELEASES_BASE/download/$tag/$assetName"

    fun latestDownloadUrl(assetName: String) = "$RELEASES_BASE/latest/download/$assetName"
}
