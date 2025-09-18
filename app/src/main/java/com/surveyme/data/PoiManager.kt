package com.surveyme.data

import android.content.Context
import com.surveyme.data.database.SurveyMeDatabase
import com.surveyme.data.repository.PoiRepository

object PoiManager {
    private var repository: PoiRepository? = null

    fun getRepository(context: Context): PoiRepository {
        if (repository == null) {
            val database = SurveyMeDatabase.getDatabase(context.applicationContext)
            repository = PoiRepository(database, context.applicationContext)
        }
        return repository!!
    }
}