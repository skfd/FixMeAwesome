package com.surveyme.data.model

enum class PoiCategory(
    val displayName: String,
    val iconResource: String,
    val color: String,
    val defaultRadius: Int = 50  // meters
) {
    SHOP("Shop", "ic_shop", "#4CAF50", 30),
    RESTAURANT("Restaurant", "ic_restaurant", "#FF9800", 30),
    TOURIST_ATTRACTION("Tourist Attraction", "ic_attraction", "#9C27B0", 100),
    PUBLIC_TRANSPORT("Public Transport", "ic_transport", "#2196F3", 50),
    AMENITY("Amenity", "ic_amenity", "#00BCD4", 30),
    HISTORIC("Historic Site", "ic_historic", "#795548", 75),
    NATURAL("Natural Feature", "ic_nature", "#8BC34A", 100),
    INFRASTRUCTURE("Infrastructure", "ic_infrastructure", "#607D8B", 50),
    UNKNOWN("Unknown", "ic_unknown", "#9E9E9E", 50);

    companion object {
        fun fromString(value: String): PoiCategory {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}