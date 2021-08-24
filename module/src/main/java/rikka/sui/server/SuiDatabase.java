package rikka.sui.server;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

import androidx.annotation.Nullable;

import java.io.File;

import rikka.sui.server.SuiConfig.PackageEntry;

public class SuiDatabase {

    private SuiDatabase() {
    }

    static {
        DATABASE_PATH = (new File("/data/adb/sui/sui.db")).getPath();
    }

    private static final String DATABASE_PATH;
    private static final String UID_CONFIG_TABLE = "uid_configs";
    private static SQLiteDatabase databaseInternal;

    private static SQLiteDatabase createDatabase(boolean allowRetry) {
        SQLiteDatabase database;
        try {
            database = SQLiteDatabase.openDatabase(DATABASE_PATH, (CursorFactory) null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            database.execSQL("CREATE TABLE IF NOT EXISTS uid_configs(uid INTEGER PRIMARY KEY, flags INTEGER);");
        } catch (Throwable e) {
            ServerConstants.LOGGER.e(e, "create database");
            if (allowRetry && (new File(DATABASE_PATH)).delete()) {
                ServerConstants.LOGGER.i("delete database and retry");
                database = createDatabase(false);
            } else {
                database = null;
            }
        }

        return database;
    }

    private static SQLiteDatabase getDatabase() {
        if (databaseInternal == null) {
            databaseInternal = createDatabase(true);
        }
        return databaseInternal;
    }

    @Nullable
    public static SuiConfig readConfig() {
        SQLiteDatabase database = getDatabase();
        if (database == null) {
            return null;
        }

        try (Cursor cursor = database.query(UID_CONFIG_TABLE, (String[]) null, (String) null, (String[]) null, (String) null, (String) null, (String) null, (String) null)) {
            if (cursor == null) {
                return null;
            }
            SuiConfig res = new SuiConfig();
            int cursorIndexOfUid = cursor.getColumnIndexOrThrow("uid");
            int cursorIndexOfFlags = cursor.getColumnIndexOrThrow("flags");
            if (cursor.moveToFirst()) {
                do {
                    res.packages.add(new PackageEntry(cursor.getInt(cursorIndexOfUid), cursor.getInt(cursorIndexOfFlags)));
                } while (cursor.moveToNext());
            }
            return res;
        }
    }

    public static void updateUid(int uid, int flags) {
        SQLiteDatabase database = getDatabase();
        if (database == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("uid", uid);
        values.put("flags", flags);
        String selection = "uid=?";
        String[] selectionArgs = new String[]{String.valueOf(uid)};
        if (database.update(UID_CONFIG_TABLE, values, selection, selectionArgs) <= 0) {
            database.insertWithOnConflict(UID_CONFIG_TABLE, (String) null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public static void removeUid(int uid) {
        SQLiteDatabase database = getDatabase();
        if (database == null) {
            return;
        }

        String selection = "uid=?";
        String[] selectionArgs = new String[]{String.valueOf(uid)};
        database.delete(UID_CONFIG_TABLE, selection, selectionArgs);
    }
}
