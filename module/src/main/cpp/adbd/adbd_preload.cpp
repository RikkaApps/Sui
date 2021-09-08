#include <cstdlib>

extern "C" {
__attribute__((constructor))
void constructor() {
    auto ld_preload = getenv("SUI_LD_PRELOAD_BACKUP");
    if (ld_preload) {
        setenv("LD_PRELOAD", ld_preload, 1);
    } else {
        unsetenv("LD_PRELOAD");
    }
}

__attribute__((visibility("default"))) __attribute__((used))
int __android_log_is_debuggable() { // NOLINT(bugprone-reserved-identifier)
    return 1;
}
}
