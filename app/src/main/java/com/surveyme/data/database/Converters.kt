package com.surveyme.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.surveyme.data.model.PoiCategory
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromPoiCategory(category: PoiCategory): String {
        return category.name
    }

    @TypeConverter
    fun toPoiCategory(categoryString: String): PoiCategory {
        return PoiCategory.fromString(categoryString)
    }

    @TypeConverter
    fun fromStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromMapToString(map: Map<String, String>): String {
        return gson.toJson(map)
    }
}