#pragma once

#include <jni.h>
#include "dex_file.h"

namespace SystemServer {
    void main(JNIEnv *env, DexFile *dexFile);
}
