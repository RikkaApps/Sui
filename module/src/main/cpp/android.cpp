#include <sys/system_properties.h>

namespace android {

    static int apiLevel = 0;
    static int previewApiLevel = 0;

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
}