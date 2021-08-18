/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

package rikka.sui.server

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
    fun readConfig(): SuiConfig? {
        return database?.query(UID_CONFIG_TABLE, null, null, null, null, null, null, null)?.use { cursor ->
            val res = SuiConfig()
            val cursorIndexOfUid = cursor.getColumnIndexOrThrow("uid")
            val cursorIndexOfFlags = cursor.getColumnIndexOrThrow("flags")
            if (cursor.moveToFirst()) {
                do {
                    res.packages.add(
                        SuiConfig.PackageEntry(
                            cursor.getInt(cursorIndexOfUid),
                            cursor.getInt(cursorIndexOfFlags)
                        )
                    )
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
