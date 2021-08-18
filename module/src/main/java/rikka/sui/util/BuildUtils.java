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

import android.os.Build;

public class BuildUtils {

    private static final int SDK = Build.VERSION.SDK_INT;

    private static final int PREVIEW_SDK = SDK >= 23 ? Build.VERSION.PREVIEW_SDK_INT : 0;

    public static boolean atLeast31() {
        return SDK >= 31;
    }

    public static boolean atLeast30() {
        return SDK >= 30;
    }

    public static boolean atLeast29() {
        return SDK >= 29;
    }

    public static boolean atLeast28() {
        return SDK >= 28;
    }

    public static boolean atLeast26() {
        return SDK >= 26;
    }

    public static boolean atLeast24() {
        return SDK >= 24;
    }

    public static boolean atLeast23() {
        return SDK >= 23;
    }
}
