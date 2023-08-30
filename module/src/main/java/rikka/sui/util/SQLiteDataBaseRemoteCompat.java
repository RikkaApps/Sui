package rikka.sui.util;

import android.annotation.SuppressLint;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import java.io.File;

public class SQLiteDataBaseRemoteCompat {

    private static final String TAG = "SQLiteDataBaseRemoteCompat";

    @SuppressLint("WrongConstant")
    public static SQLiteDatabase openDatabase(String path, DatabaseErrorHandler errorHandler) {
        File file = new File(path);

        int openFlags = SQLiteDatabase.OPEN_READWRITE
                | SQLiteDatabase.CREATE_IF_NECESSARY
                | SQLiteDatabase.NO_LOCALIZED_COLLATORS
                | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            SQLiteDatabase.OpenParams.Builder params = new SQLiteDatabase.OpenParams.Builder()
                    .addOpenFlags(openFlags)
                    .setErrorHandler(sqLiteDatabase -> Log.w(TAG, "database corrupted"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.setSynchronousMode("NORMAL");
            }
            return SQLiteDatabase.openDatabase(file, params.build());
        } else {
            return SQLiteDatabase.openDatabase(
                    path, null,
                    openFlags,
                    errorHandler
            );
        }
    }
}
