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

package rikka.sui.server.config;

import androidx.annotation.Nullable;

import static rikka.sui.server.ServerConstants.LOGGER;

public class ConfigManager {

    public static Config load() {
        Config config = SuiDatabase.readConfig();
        if (config == null) {
            LOGGER.i("failed to read database, starting empty");
            return new Config();
        }
        return config;
    }

    private static ConfigManager instance;

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }


    private final Config config;

    private ConfigManager() {
        this.config = load();
    }

    private Config.PackageEntry findLocked(int uid) {
        for (Config.PackageEntry entry : config.packages) {
            if (uid == entry.uid) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public Config.PackageEntry find(int uid) {
        synchronized (this) {
            return findLocked(uid);
        }
    }

    public void update(int uid, int mask, int values) {
        synchronized (this) {
            Config.PackageEntry entry = findLocked(uid);
            if (entry == null) {
                entry = new Config.PackageEntry(uid, mask & values);
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

    public void remove(int uid) {
        synchronized (this) {
            Config.PackageEntry entry = findLocked(uid);
            if (entry == null) {
                return;
            }
            config.packages.remove(entry);
            SuiDatabase.removeUid(uid);
        }
    }

    public boolean isHidden(int uid) {
        Config.PackageEntry entry = find(uid);
        if (entry == null) {
            return false;
        }
        return (entry.flags & Config.FLAG_HIDDEN) != 0;
    }
}