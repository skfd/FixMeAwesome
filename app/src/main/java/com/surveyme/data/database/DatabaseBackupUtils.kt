package com.surveyme.data.database

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

object DatabaseBackupUtils {

    fun backupDatabase(context: Context, backupDir: File): Boolean {
        try {
            val dbFile = context.getDatabasePath("surveyme_database")
            if (!dbFile.exists()) {
                Timber.w("Database file does not exist to backup")
                return false
            }

            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val backupFile = File(backupDir, "surveyme_database_backup.db")
            
            val src: FileChannel = FileInputStream(dbFile).channel
            val dst: FileChannel = FileOutputStream(backupFile).channel
            dst.transferFrom(src, 0, src.size())
            src.close()
            dst.close()

            Timber.i("Database backed up successfully to ${backupFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error backing up database")
            return false
        }
    }
}
