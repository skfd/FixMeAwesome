package com.surveyme.data.model

import com.google.gson.annotations.SerializedName

data class OverpassResponse(
    @SerializedName("version") val version: Double,
    @SerializedName("generator") val generator: String,
    @SerializedName("elements") val elements: List<Element>
)

data class Element(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: Long,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("center") val center: Center?,
    @SerializedName("tags") val tags: Map<String, String>?
)

data class Center(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double
)
