package rikka.sui.server.config

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import rikka.sui.server.ServerConstants.LOGGER
import java.io.File

object SuiDatabase {

    private val DATABASE_PATH = File("/data/adb/sui/sui.db").path
    private const val UID_CONFIG_TABLE = "uid_configs"

    private fun createDatabase(allowRetry: Boolean): SQLiteDatabase? = try {
        SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.NO_LOCALIZED_COLLATORS).apply {
            execSQL("CREATE TABLE IF NOT EXISTS $UID_CONFIG_TABLE(uid INTEGER PRIMARY KEY, flags INTEGER);")
        }
    } catch (e: Throwable) {
        LOGGER.e(e, "create database")

        if (allowRetry && File(DATABASE_PATH).delete()) {
            LOGGER.i("delete database and retry")
            createDatabase(false)
        } else {
            null
        }
    }

    private var databaseInternal: SQLiteDatabase? = null

    private val database: SQLiteDatabase?
        get() {
            if (databaseInternal != null) return databaseInternal
            databaseInternal = createDatabase(true)
            return databaseInternal
        }

    @JvmStatic
    fun readConfig(): Config? {
        return database?.query(UID_CONFIG_TABLE, null, null, null, null, null, null, null)?.use { cursor ->
            val res = Config()
            val cursorIndexOfUid = cursor.getColumnIndexOrThrow("uid")
            val cursorIndexOfFlags = cursor.getColumnIndexOrThrow("flags")
            if (cursor.moveToFirst()) {
                do {
                    res.packages.add(Config.PackageEntry(
                            cursor.getInt(cursorIndexOfUid),
                            cursor.getInt(cursorIndexOfFlags)
                    ))
                } while (cursor.moveToNext())
            }
            res

        }
    }

    @JvmStatic
    fun updateUid(uid: Int, flags: Int) {
        val values = ContentValues().apply {
            put("uid", uid)
            put("flags", flags)
        }
        val selection = "uid=?"
        val selectionArgs = arrayOf(uid.toString())

        if (database?.update(UID_CONFIG_TABLE, values, selection, selectionArgs) ?: 0 <= 0) {
            database?.insertWithOnConflict(UID_CONFIG_TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    @JvmStatic
    fun removeUid(uid: Int) {
        val selection = "uid=?"
        val selectionArgs = arrayOf(uid.toString())

        database?.delete(UID_CONFIG_TABLE, selection, selectionArgs)
    }
}