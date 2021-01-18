#pragma once

#include <jni.h>
#include "dex_file.h"

namespace Settings {
    void main(JNIEnv *env, const char *appDataDir, DexFile *dexFile, std::vector<File *> *files);
}
