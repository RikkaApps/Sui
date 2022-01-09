#include <sys/mount.h>
#include <private/ScopedFd.h>
#include <sys/mman.h>
#include <cinttypes>
#include <selinux.h>
#include <logging.h>
#include <misc.h>
#include <elf.h>
#include <link.h>
#include <private/ScopedReaddir.h>
#include <string_view>
#include <android/api-level.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <android.h>

using namespace std::literals::string_view_literals;

#define ERR_SELINUX 10
#define ERR_NO_ADBD 11
#define ERR_ADBD_IS_STATIC 12
#define ERR_OTHER 13

struct attrs {
    uid_t uid{};
    gid_t gid{};
    mode_t mode{};
    char *context{nullptr};

    ~attrs() {
        if (context) {
            freecon(context);
        }
    }
};

inline int getattrs(const char *file, attrs *attrs) {
    struct stat statbuf{};
    if (stat(file, &statbuf) != 0) {
        PLOGE("stat %s", file);
        return 1;
    }

    if (getfilecon_raw(file, &attrs->context) < 0) {
        PLOGE("getfilecon %s", file);
        return 1;
    }

    attrs->uid = statbuf.st_uid;
    attrs->gid = statbuf.st_gid;
    attrs->mode = statbuf.st_mode;
    return 0;
}

inline int setattrs(const char *file, const attrs *attrs) {
    uid_t uid = attrs->uid;
    gid_t gid = attrs->gid;
    mode_t mode = attrs->mode;
    const char *secontext = attrs->context;

    if (chmod(file, mode) != 0) {
        PLOGE("chmod %s", file);
        return 1;
    }
    if (chown(file, uid, gid) != 0) {
        PLOGE("chmod %s", file);
        return 1;
    }
    if (setfilecon_raw(file, secontext) != 0) {
        PLOGE("setfilecon %s", file);
        return 1;
    }
    return 0;
}

inline int setup_file(const char *source, const char *target, const attrs *attrs) {
    if (attrs && setattrs(source, attrs) != 0) {
        return 1;
    }
    if (int fd = open(target, O_RDWR | O_CREAT, 0700); fd == -1) {
        PLOGE("open %s", target);
        return 1;
    } else {
        close(fd);
    }
    if (mount(source, target, nullptr, MS_BIND, nullptr)) {
        PLOGE("mount %s -> %s", source, target);
        return 1;
    }
    return 0;
}

inline bool is_dynamically_linked(const char *path) {
    struct stat st;
    ScopedFd fd(open(path, O_RDONLY));

    if (fd.get() == -1) {
        PLOGE("open %s", path);
        return false;
    }
    if (fstat(fd.get(), &st) < 0) {
        PLOGE("fstat");
        return false;
    }

    auto data = static_cast<uint8_t *>(mmap(nullptr, st.st_size, PROT_READ, MAP_PRIVATE, fd.get(), 0));
    if (data == MAP_FAILED) {
        PLOGE("mmap");
        return false;
    }

    auto ehdr = (ElfW(Ehdr) *) data;
#ifdef __LP64__
    if (ehdr->e_ident[EI_CLASS] != ELFCLASS64) {
        LOGE("Not elf64");
        munmap(data, st.st_size);
        return false;
    }
#else
    if (ehdr->e_ident[EI_CLASS] != ELFCLASS32) {
        LOGE("Not elf32");
        munmap(data, st.st_size);
        return false;
    }
#endif

    bool is_dynamically_linked = false;

    auto phdr = (ElfW(Phdr) *) (data + ehdr->e_phoff);
    int phnum = ehdr->e_phnum;

    for (int i = 0; i < phnum; ++i) {
        if (phdr[i].p_type == PT_DYNAMIC) {
            is_dynamically_linked = true;
            break;
        }
    }

    munmap(data, st.st_size);
    return is_dynamically_linked;
}

inline int setup_adb_root_apex(const char *root_path, const char *adbd_wrapper, const char *adbd_preload) {
    char versioned_adbd[PATH_MAX]{0};
    char path[PATH_MAX]{0};
    char source[PATH_MAX]{0};
    char target[PATH_MAX]{0};
    const char *adbd, *adbd_real, *bin_folder, *lib_folder;
    attrs file_attr{}, folder_attr{}, lib_attr{};

    adbd = "/apex/com.android.adbd/bin/adbd";
    adbd_real = "/apex/com.android.adbd/bin/adbd_real";
    bin_folder = "/apex/com.android.adbd/bin";
#ifdef __LP64__
    lib_folder = "/apex/com.android.adbd/lib64";
#else
    lib_folder = "/apex/com.android.adbd/lib";
#endif

    if (!is_dynamically_linked(adbd)) {
        LOGE("%s is not dynamically linked (or 32 bit elf on 64 bit machine)", adbd);
        return ERR_ADBD_IS_STATIC;
    } else {
        LOGI("%s is dynamically linked", adbd);
    }

    if (getattrs(adbd, &file_attr) != 0
        || getattrs(bin_folder, &folder_attr) != 0) {
        return ERR_OTHER;
    } else {
        LOGV("%s: uid=%d, gid=%d, mode=%3o, context=%s",
             adbd, file_attr.uid, file_attr.gid, file_attr.mode, file_attr.context);
        LOGV("%s: uid=%d, gid=%d, mode=%3o, context=%s",
             bin_folder, folder_attr.uid, folder_attr.gid, folder_attr.mode, folder_attr.context);
    }

    // Path of real of adbd in module folder
    char my_backup[PATH_MAX]{0};
    strcpy(my_backup, root_path);
    strcat(my_backup, "/bin/adbd_real");

    if (copyfile(adbd, my_backup) != 0) {
        PLOGE("copyfile %s -> %s", adbd, my_backup);
        return ERR_OTHER;
    }

    // Find /apex/com.android.adbd@version
    ScopedReaddir dir("/apex");
    if (dir.IsBad()) {
        PLOGE("opendir %s", "/apex");
        return ERR_OTHER;
    }

    auto apex = "/apex/"sv;
    strncpy(versioned_adbd, apex.data(), apex.length());

    bool found = false;
    uint64_t version = 0;
    auto adbd_prefix = "com.android.adbd@"sv;
    while (dirent *entry = dir.ReadEntry()) {
        std::string_view d_name{entry->d_name};

        if (d_name.length() <= adbd_prefix.length() ||
            d_name.substr(0, adbd_prefix.length()) != adbd_prefix)
            continue;

        const char *version_string = entry->d_name + adbd_prefix.length();
        int new_version = atoll(version_string);
        if (new_version >= version) {
            strncpy(versioned_adbd + apex.length(), d_name.data(), d_name.length());
            LOGI("Found versioned apex %s", versioned_adbd);
            found = true;
        }
    }

    if (!found) {
        LOGE("Cannot find versioned apex");
        return ERR_OTHER;
    }

    strcpy(path, versioned_adbd);
    strcat(path, "/lib");
#ifdef __LP64__
    strcat(path, "64");
#endif

    ScopedReaddir lib(path);
    if (lib.IsBad()) {
        PLOGE("opendir %s", path);
        return ERR_OTHER;
    }

    bool bin_mounted = false, lib_mounted = false;

    LOGI("Mount %s tmpfs", bin_folder);

    {
        if (mount("tmpfs", bin_folder, "tmpfs", 0, "mode=755") != 0) {
            PLOGE("mount tmpfs -> %s", bin_folder);
            goto failed;
        }

        bin_mounted = true;

        if (setattrs(bin_folder, &folder_attr) != 0) {
            goto failed;
        }

        // $MODDIR/bin/adbd_wrapper -> /apex/com.android.adbd/bin/adbd
        if (setup_file(adbd_wrapper, adbd, &file_attr) != 0) {
            LOGE("Failed to %s -> %s", adbd_wrapper, adbd);
            goto failed;
        }

        if (file_attr.context) {
            freecon(file_attr.context);
        }
        file_attr.context = strdup("u:object_r:magisk_file:s0");

        // $MODDIR/bin/adbd_real -> /apex/com.android.adbd/bin/adbd_real
        if (setup_file(my_backup, adbd_real, &file_attr) != 0) {
            LOGE("Failed to %s -> %s", my_backup, adbd_real);
            goto failed;
        }
    }

    LOGI("Mount %s tmpfs", lib_folder);

    {
        if (mount("tmpfs", lib_folder, "tmpfs", 0, "mode=755") != 0) {
            PLOGE("mount tmpfs -> %s", lib_folder);
            goto failed;
        }

        lib_mounted = true;

        if (lib.IsBad()) {
            goto failed;
        }

        while (dirent *entry = lib.ReadEntry()) {
            if (entry->d_name[0] == '.') continue;

            strcpy(source, versioned_adbd);
#ifdef __LP64__
            strcat(source, "/lib64/");
#else
            strcat(source, "/lib/");
#endif
            strcat(source, entry->d_name);

            if (getattrs(source, &lib_attr) != 0) {
                goto failed;
            }

            strcpy(target, lib_folder);
            strcat(target, "/");
            strcat(target, entry->d_name);

            if (setup_file(source, target, nullptr) != 0) {
                LOGE("Failed to %s -> %s", source, target);
                goto failed;
            }
        }

        strcpy(target, lib_folder);
        strcat(target, "/libsui_adbd_preload.so");

        if (setup_file(adbd_preload, target, &lib_attr) != 0) {
            LOGE("Failed to %s -> %s", adbd_preload, target);
            goto failed;
        }
    }

    LOGI("Finished");
    return EXIT_SUCCESS;

    failed:
    if (bin_mounted) {
        if (umount2(bin_folder, MNT_DETACH) != 0) {
            PLOGE("umount2 %s", bin_folder);
        } else {
            LOGW("Unmount %s", bin_folder);
        }
    }
    if (lib_mounted) {
        if (umount2(lib_folder, MNT_DETACH) != 0) {
            PLOGE("umount2 %s", lib_folder);
        } else {
            LOGW("Unmount %s", lib_folder);
        }
    }
    return ERR_OTHER;
}

inline int setup_adb_root_non_apex(const char *root_path, const char *adbd_wrapper, const char *adbd_preload) {
    const char *file, *folder;
    attrs file_attr{}, folder_attr{};

    file = "/system/bin/adbd";
    folder = "/system/bin";

    if (!is_dynamically_linked(file)) {
        LOGE("%s is not dynamically linked", file);
        return ERR_ADBD_IS_STATIC;
    } else {
        LOGI("%s is dynamically linked", file);
    }

    if (getattrs(file, &file_attr) != 0
        || getattrs(folder, &folder_attr) != 0) {
        return ERR_OTHER;
    } else {
        LOGV("%s: uid=%d, gid=%d, mode=%3o, context=%s",
             file, file_attr.uid, file_attr.gid, file_attr.mode, file_attr.context);
        LOGV("%s: uid=%d, gid=%d, mode=%3o, context=%s",
             folder, folder_attr.uid, folder_attr.gid, folder_attr.mode, folder_attr.context);
    }

    LOGI("Copy files to MODDIR/system");

    char module_folder[PATH_MAX]{0};
    char target[PATH_MAX]{0};

    // mkdir $MODDIR/system/bin
    strcpy(module_folder, root_path);
    strcat(module_folder, "/system/bin");
    if (mkdir(module_folder, folder_attr.mode) == -1 && errno != EEXIST) {
        PLOGE("mkdir %s", module_folder);
        return ERR_OTHER;
    }

    // $MODDIR/system/bin/adbd_wrapper -> $MODDIR/system/bin/adbd
    strcpy(target, module_folder);
    strcat(target, "/adbd");
    if (copyfile(adbd_wrapper, target) != 0) {
        PLOGE("copyfile %s -> %s", adbd_wrapper, target);
        return ERR_OTHER;
    }
    if (setattrs(target, &file_attr) != 0) {
        unlink(target);
        return ERR_OTHER;
    }

    // /system/bin/adbd -> $MODDIR/system/bin/adbd_real
    strcpy(target, module_folder);
    strcat(target, "/adbd_real");
    if (copyfile(file, target) != 0) {
        PLOGE("copyfile %s -> %s", file, target);
        return ERR_OTHER;
    }
    if (file_attr.context) {
        freecon(file_attr.context);
    }
    file_attr.context = strdup("u:object_r:magisk_file:s0");
    if (setattrs(target, &file_attr) != 0) {
        unlink(target);
        return ERR_OTHER;
    }

    // mkdir $MODDIR/system/lib(64)
    strcpy(module_folder, root_path);
    strcat(module_folder, "/system/lib");
#ifdef __LP64__
    strcat(module_folder, "64");
#endif
    if (mkdir(module_folder, folder_attr.mode) == -1 && errno != EEXIST) {
        PLOGE("mkdir %s", module_folder);
        return ERR_OTHER;
    }

    // $MODDIR/lib/libadbd_preload.so -> $MODDIR/system/lib(64)/libsui_adbd_preload.so
    strcpy(target, module_folder);
    strcat(target, "/libsui_adbd_preload.so");
    if (copyfile(adbd_preload, target) != 0) {
        PLOGE("copyfile %s -> %s", adbd_preload, target);
        return ERR_OTHER;
    }
    if (setattrs(target, &file_attr) != 0) {
        unlink(target);
        return ERR_OTHER;
    }

    LOGI("Finished");
    return EXIT_SUCCESS;
}

static int setup_adb_root(const char *root_path) {
    if (selinux_check_access("u:r:adbd:s0", "u:r:adbd:s0", "process", "setcurrent", nullptr) != 0) {
        PLOGE("adbd adbd process setcurrent not allowed");
        return ERR_SELINUX;
    }

    if (selinux_check_access("u:r:adbd:s0", "u:r:magisk:s0", "process", "dyntransition", nullptr) != 0) {
        PLOGE("adbd magisk process dyntransition not allowed");
        return ERR_SELINUX;
    }

    char adbd_wrapper[PATH_MAX]{0};
    strcpy(adbd_wrapper, root_path);
    strcat(adbd_wrapper, "/bin/adbd_wrapper");

    char adbd_preload[PATH_MAX]{0};
    strcpy(adbd_preload, root_path);
    strcat(adbd_preload, "/lib/libadbd_preload.so");

    if (android::GetApiLevel() >= __ANDROID_API_R__) {
        if (access("/apex/com.android.adbd/bin/adbd", F_OK) != 0) {
            PLOGE("access /apex/com.android.adbd/bin/adbd");
            LOGW("Apex not exists on API 31+ device");
        } else {
            LOGI("Use adbd from /apex");
            return setup_adb_root_apex(root_path, adbd_wrapper, adbd_preload);
        }
    }

    if (access("/system/bin/adbd", F_OK) != 0) {
        PLOGE("access /system/bin/adbd");
        LOGW("No adbd");
        return ERR_NO_ADBD;
    }

    LOGI("Use adbd from /system");
    return setup_adb_root_non_apex(root_path, adbd_wrapper, adbd_preload);
}

inline int adb_root_main(int argc, char **argv) {
    LOGI("Setup adb root support: %s", argv[1]);

    if (init_selinux()) {
        auto root_path = argv[1];
        return setup_adb_root(root_path);
    } else {
        LOGW("Cannot load libselinux");
        return 1;
    }
}
