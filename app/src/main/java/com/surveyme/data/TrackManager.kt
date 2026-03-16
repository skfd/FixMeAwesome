package com.surveyme.data

import android.content.Context
import com.surveyme.data.database.SurveyMeDatabase
import com.surveyme.data.repository.TrackRepository

object TrackManager {
    private var repository: TrackRepository? = null

    fun getRepository(context: Context): TrackRepository {
        if (repository == null) {
            val database = SurveyMeDatabase.getDatabase(context.applicationContext)
            repository = TrackRepository(database.trackDao())
        }
        return repository!!
    }
}
