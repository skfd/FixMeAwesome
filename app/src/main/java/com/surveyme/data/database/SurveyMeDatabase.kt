package com.surveyme.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.surveyme.data.model.Poi

@Database(
    entities = [Poi::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SurveyMeDatabase : RoomDatabase() {

    abstract fun poiDao(): PoiDao

    companion object {
        @Volatile
        private var INSTANCE: SurveyMeDatabase? = null

        fun getDatabase(context: Context): SurveyMeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SurveyMeDatabase::class.java,
                    "surveyme_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}