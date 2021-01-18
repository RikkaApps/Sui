/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rikka.sui.resource;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.BidiFormatter;

import androidx.annotation.NonNull;

public class Utils {

    /**
     * The maximum length of a safe label, in characters
     */
    public static final int MAX_SAFE_LABEL_LENGTH = 1000;

    public static final float DEFAULT_MAX_LABEL_SIZE_PX = 500f;

    /**
     * Get the label for an application, truncating if it is too long.
     *
     * @param applicationInfo the {@link ApplicationInfo} of the application
     * @param context         the {@code Context} to retrieve {@code PackageManager}
     * @return the label for the application
     */
    @NonNull
    public static String getAppLabel(@NonNull ApplicationInfo applicationInfo,
                                     @NonNull Context context) {
        return getAppLabel(applicationInfo, DEFAULT_MAX_LABEL_SIZE_PX, context);
    }

    /**
     * Get the label for an application with the ability to control truncating.
     *
     * @param applicationInfo the {@link ApplicationInfo} of the application
     * @param ellipsizeDip    see {@link TextUtilsCompat#makeSafeForPresentation}.
     * @param context         the {@code Context} to retrieve {@code PackageManager}
     * @return the label for the application
     */
    @NonNull
    private static String getAppLabel(@NonNull ApplicationInfo applicationInfo, float ellipsizeDip,
                                      @NonNull Context context) {
        String unsafeLabel = applicationInfo.loadLabel(context.getPackageManager()).toString();

        return BidiFormatter.getInstance().unicodeWrap(TextUtilsCompat.makeSafeForPresentation(
                unsafeLabel, MAX_SAFE_LABEL_LENGTH, ellipsizeDip,
                TextUtilsCompat.SAFE_STRING_FLAG_TRIM | TextUtilsCompat.SAFE_STRING_FLAG_FIRST_LINE)
                .toString());
    }
}
