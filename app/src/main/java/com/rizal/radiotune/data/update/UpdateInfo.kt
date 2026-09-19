package com.rizal.radiotune.data.update

data class UpdateInfo(
    val tag: String,
    val versionName: String,
    val versionCode: Int,
    val releasePageUrl: String,
    /** Candidate download URLs, tried in order. */
    val downloadUrls: List<String>,
)
