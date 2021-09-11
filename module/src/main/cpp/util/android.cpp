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

#include <sys/system_properties.h>

namespace android {

    static int apiLevel = 0;
    static int previewApiLevel = 0;
    static int8_t has32Bit = -1;
    static int8_t has64Bit = -1;

    int GetApiLevel() {
        if (apiLevel > 0) return apiLevel;

        char buf[PROP_VALUE_MAX + 1];
        if (__system_property_get("ro.build.version.sdk", buf) > 0)
            apiLevel = atoi(buf);

        return apiLevel;
    }

    int GetPreviewApiLevel() {
        if (previewApiLevel > 0) return previewApiLevel;

        char buf[PROP_VALUE_MAX + 1];
        if (__system_property_get("ro.build.version.preview_sdk", buf) > 0)
            previewApiLevel = atoi(buf);

        return previewApiLevel;
    }

    bool Has32Bit() {
        if (has32Bit != -1) return has32Bit == 1;

        char buf[PROP_VALUE_MAX + 1];
        has32Bit = __system_property_get("ro.product.cpu.abilist32", buf) > 0 ? 1 : 0;
        return has32Bit;
    }

    bool Has64Bit() {
        if (has64Bit != -1) return has64Bit == 1;

        char buf[PROP_VALUE_MAX + 1];
        has64Bit = __system_property_get("ro.product.cpu.abilist64", buf) > 0 ? 1 : 0;
        return has64Bit;
    }
}
