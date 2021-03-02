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

package rikka.sui.util;

import android.util.Log;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Logger {

    private final String TAG;
    private final java.util.logging.Logger LOGGER;

    public Logger(String TAG) {
        this.TAG = TAG;
        this.LOGGER = null;
    }

    public Logger(String TAG, String file) {
        this.TAG = TAG;
        this.LOGGER = java.util.logging.Logger.getLogger(TAG);
        try {
            FileHandler fh = new FileHandler(file);
            fh.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isLoggable(String tag, int level) {
        return true;
    }

    public void v(String msg) {
        if (isLoggable(TAG, Log.VERBOSE)) {
            println(Log.VERBOSE, msg);
        }
    }

    public void v(String fmt, Object... args) {
        if (isLoggable(TAG, Log.VERBOSE)) {
            println(Log.VERBOSE, String.format(Locale.ENGLISH, fmt, args));
        }
    }

    public void v(String msg, Throwable tr) {
        if (isLoggable(TAG, Log.VERBOSE)) {
            println(Log.VERBOSE, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    public void d(String msg) {
        if (isLoggable(TAG, Log.DEBUG)) {
            println(Log.DEBUG, msg);
        }
    }

    public void d(String fmt, Object... args) {
        if (isLoggable(TAG, Log.DEBUG)) {
            println(Log.DEBUG, String.format(Locale.ENGLISH, fmt, args));
        }
    }

    public void d(String msg, Throwable tr) {
        if (isLoggable(TAG, Log.DEBUG)) {
            println(Log.DEBUG, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    public void i(String msg) {
        if (isLoggable(TAG, Log.INFO)) {
            println(Log.INFO, msg);
        }
    }

    public void i(String fmt, Object... args) {
        if (isLoggable(TAG, Log.INFO)) {
            println(Log.INFO, String.format(Locale.ENGLISH, fmt, args));
        }
    }

    public void i(String msg, Throwable tr) {
        if (isLoggable(TAG, Log.INFO)) {
            println(Log.INFO, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    public void w(String msg) {
        if (isLoggable(TAG, Log.WARN)) {
            println(Log.WARN, msg);
        }
    }

    public void w(String fmt, Object... args) {
        if (isLoggable(TAG, Log.WARN)) {
            println(Log.WARN, String.format(Locale.ENGLISH, fmt, args));
        }
    }

    public void w(Throwable tr, String fmt, Object... args) {
        if (isLoggable(TAG, Log.WARN)) {
            println(Log.WARN, String.format(Locale.ENGLISH, fmt, args) + '\n' + Log.getStackTraceString(tr));
        }
    }

    public void w(String msg, Throwable tr) {
        if (isLoggable(TAG, Log.WARN)) {
            println(Log.WARN, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    public void e(String msg) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(Log.ERROR, msg);
        }
    }

    public void e(String fmt, Object... args) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(Log.ERROR, String.format(Locale.ENGLISH, fmt, args));
        }
    }

    public void e(String msg, Throwable tr) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(Log.ERROR, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    public void e(Throwable tr, String fmt, Object... args) {
        if (isLoggable(TAG, Log.ERROR)) {
            println(Log.ERROR, String.format(Locale.ENGLISH, fmt, args) + '\n' + Log.getStackTraceString(tr));
        }
    }

    public int println(int priority, String msg) {
        if (LOGGER != null) {
            LOGGER.info(msg);
        }
        return Log.println(priority, TAG, msg);
    }
}
