#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include "sui_main.hpp"
#include "adb_root.hpp"
#include "uninstall_main.hpp"

using main_func = int (*)(int, char **);

static main_func applet_func[] = {sui_main, adb_root_main, uninstall_main, nullptr };

static const char* applet_names[] = {"sui", "adb_root", "uninstall", nullptr };

int main(int argc, char **argv) {
    auto uid = getuid();
    if (uid != 0) {
        exit(EXIT_FAILURE);
    }

    auto base = basename(argv[0]);
    for (int i = 0; applet_names[i]; ++i) {
        if (strcmp(base, applet_names[i]) == 0) {
            return applet_func[i](argc, argv);
        }
    }
    return 1;
}
