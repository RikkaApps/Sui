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

package rikka.sui.systemserver.launcherapps;

import android.content.pm.ILauncherApps;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;

public class LauncherAppsFuncs {

    public static class StartShortcut {
        public String callingPackage;
        public String packageName;
        public String featureId;
        public String id;
        public Rect sourceBounds;
        public Bundle startActivityOptions;
        public int userId;

        private final ILauncherApps original;
        private boolean consumed;
        public boolean result;

        public StartShortcut(ILauncherApps original, String callingPackage, String packageName, String featureId, String id, Rect sourceBounds, Bundle startActivityOptions, int userId) {
            this.callingPackage = callingPackage;
            this.packageName = packageName;
            this.featureId = featureId;
            this.id = id;
            this.sourceBounds = sourceBounds;
            this.startActivityOptions = startActivityOptions;
            this.userId = userId;
            this.original = original;
        }

        public void consume(boolean result) {
            this.result = result;
            consumed = true;
        }

        public void call() throws RemoteException {
            if (consumed) {
                return;
            }

            consumed = true;

            if (Build.VERSION.SDK_INT >= 30) {
                result = original.startShortcut(callingPackage, packageName, featureId, id, sourceBounds, startActivityOptions, userId);
            } else if (Build.VERSION.SDK_INT >= 26) {
                result = original.startShortcut(callingPackage, packageName, id, sourceBounds, startActivityOptions, userId);
            } else {
                result = false;
            }
        }
    }
}
