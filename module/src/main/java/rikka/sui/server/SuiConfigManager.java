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

package rikka.sui.server;

import androidx.annotation.Nullable;

import java.util.List;

import rikka.shizuku.server.ConfigManager;

public class SuiConfigManager extends ConfigManager {

    public static SuiConfig load() {
        SuiConfig config = SuiDatabase.readConfig();
        if (config == null) {
            LOGGER.i("failed to read database, starting empty");
            return new SuiConfig();
        }
        return config;
    }

    private static SuiConfigManager instance;

    public static SuiConfigManager getInstance() {
        if (instance == null) {
            instance = new SuiConfigManager();
        }
        return instance;
    }


    private final SuiConfig config;

    public SuiConfigManager() {
        this.config = load();
    }

    private SuiConfig.PackageEntry findLocked(int uid) {
        for (SuiConfig.PackageEntry entry : config.packages) {
            if (uid == entry.uid) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public SuiConfig.PackageEntry find(int uid) {
        synchronized (this) {
            return findLocked(uid);
        }
    }

    @Override
    public void update(int uid, List<String> packages, int mask, int values) {
        update(uid, mask, values);
    }

    public void update(int uid, int mask, int values) {
        synchronized (this) {
            SuiConfig.PackageEntry entry = findLocked(uid);
            if (entry == null) {
                entry = new SuiConfig.PackageEntry(uid, mask & values);
                config.packages.add(entry);
            } else {
                int newValue = (entry.flags & ~mask) | (mask & values);
                if (newValue == entry.flags) {
                    return;
                }
                entry.flags = newValue;
            }
            SuiDatabase.updateUid(uid, entry.flags);
        }
    }

    @Override
    public void remove(int uid) {
        synchronized (this) {
            SuiConfig.PackageEntry entry = findLocked(uid);
            if (entry == null) {
                return;
            }
            config.packages.remove(entry);
            SuiDatabase.removeUid(uid);
        }
    }

    public boolean isHidden(int uid) {
        SuiConfig.PackageEntry entry = find(uid);
        if (entry == null) {
            return false;
        }
        return (entry.flags & SuiConfig.FLAG_HIDDEN) != 0;
    }
}
