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

#pragma once

#define ROOT_PATH "/data/adb/sui"
#define FALLBACK_DEX_DIR "/data/system/sui"
#define MANAGER_APPLICATION_ID "com.android.systemui"
#define SETTINGS_APPLICATION_ID "com.android.settings"

#define DEX_NAME "sui.dex"
#define DEX_PATH ROOT_PATH "/" DEX_NAME
#define RES_PATH ROOT_PATH "/res"
#define SYSTEM_PROCESS_CLASSNAME "rikka/sui/systemserver/SystemProcess"
#define MANAGER_PROCESS_CLASSNAME "rikka/sui/manager/ManagerProcess"
#define SETTINGS_PROCESS_CLASSNAME "rikka/sui/settings/SettingsProcess"
