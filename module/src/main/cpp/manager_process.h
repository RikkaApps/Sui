#pragma once

#include <jni.h>
#include "dex_file.h"

namespace Manager {
    void main(JNIEnv *env, DexFile *dexFile, const char *appDataDir);
}
