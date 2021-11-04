#include <android.h>
#include <android/api-level.h>
#include <sys/mount.h>
#include <jni.h>
#include <nativehelper/scoped_utf_chars.h>
#include <cstdio>

void UmountApexAdbd() {
    static bool called = false;
    if (called) return;

    if (android::GetApiLevel() >= __ANDROID_API_R__) {
        called = true;

        umount2("/apex/com.android.adbd/bin", MNT_DETACH);
        if (android::Has32Bit() && !android::Has64Bit()) {
            umount2("/apex/com.android.adbd/lib", MNT_DETACH);
        }
        if (android::Has64Bit()) {
            umount2("/apex/com.android.adbd/lib64", MNT_DETACH);
        }
    }
}
