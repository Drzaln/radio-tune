package com.rizal.radiotune.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val name: String,
    val code: String,
    val stationCount: Int,
)
