#include <sys/mount.h>
#include <private/ScopedFd.h>
#include <sys/mman.h>
#include <cinttypes>
#include <selinux.h>
#include <logging.h>
#include <misc.h>
#include <elf.h>
#include <link.h>

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
    if (setattrs(source, attrs) != 0) {
        return 1;
    }
    if (int fd = open(target, O_RDWR | O_CREAT, attrs->mode); fd == -1) {
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

inline int setup_adb_root_apex(const char *root_path, const char *adbd_wrapper) {
    const char *file, *backup, *folder;
    attrs file_attr{}, folder_attr{};

    file = "/apex/com.android.adbd/bin/adbd";
    backup = "/apex/com.android.adbd/bin/adbd_real";
    folder = "/apex/com.android.adbd/bin";

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

    // Path of real of adbd in module folder
    char my_backup[PATH_MAX]{0};
    strcpy(my_backup, root_path);
    strcat(my_backup, "/bin/adbd_real");

    if (copyfile(file, my_backup) != 0) {
        PLOGE("copyfile %s -> %s", file, my_backup);
        return ERR_OTHER;
    }

    LOGI("Mount /apex/com.android.adbd/bin tmpfs");

    if (mount("tmpfs", folder, "tmpfs", 0, "mode=755") != 0) {
        PLOGE("mount tmpfs -> %s", folder);
        return ERR_OTHER;
    }

    // $MODDIR/bin/adbd_wrapper -> /apex/com.android.adbd/bin/adbd
    if (setup_file(adbd_wrapper, file, &file_attr) != 0) {
        umount(folder);
        LOGE("Failed to %s -> %s", adbd_wrapper, file);
        return ERR_OTHER;
    }

    if (file_attr.context) {
        freecon(file_attr.context);
    }
    file_attr.context = strdup("u:object_r:magisk_file:s0");

    // $MODDIR/bin/adbd_real -> /apex/com.android.adbd/bin/adbd_real
    if (setup_file(my_backup, backup, &file_attr) != 0) {
        umount(folder);
        LOGE("Failed to %s -> %s", my_backup, backup);
        return ERR_OTHER;
    }

    LOGI("Finished");
    return EXIT_SUCCESS;
}

inline int setup_adb_root_non_apex(const char *root_path, const char *adbd_wrapper) {
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

    // mkdir $MODDIR/system/bin
    char module_system_bin[PATH_MAX]{0};
    strcpy(module_system_bin, root_path);
    strcat(module_system_bin, "/system/bin");
    if (mkdir(module_system_bin, folder_attr.mode) == -1 && errno != EEXIST) {
        PLOGE("mkdir %s", module_system_bin);
        return ERR_OTHER;
    }

    // $MODDIR/system/bin/adbd_wrapper -> $MODDIR/system/bin/adbd
    char path[PATH_MAX]{0};
    strcpy(path, module_system_bin);
    strcat(path, "/adbd");
    if (copyfile(adbd_wrapper, path) != 0) {
        PLOGE("copyfile %s -> %s", adbd_wrapper, path);
        return ERR_OTHER;
    }
    if (setattrs(path, &file_attr) != 0) {
        unlink(path);
        return ERR_OTHER;
    }

    // /system/bin/adbd -> $MODDIR/system/bin/adbd_real
    strcpy(path, module_system_bin);
    strcat(path, "/adbd_real");
    if (copyfile(file, path) != 0) {
        PLOGE("copyfile %s -> %s", file, path);
        return ERR_OTHER;
    }
    if (file_attr.context) {
        freecon(file_attr.context);
    }
    file_attr.context = strdup("u:object_r:magisk_file:s0");
    if (setattrs(path, &file_attr) != 0) {
        unlink(path);
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

    // Path of adbd_wrapper in module folder
    char my_file[PATH_MAX]{0};
    strcpy(my_file, root_path);
    strcat(my_file, "/bin/adbd_wrapper");

    if (android::GetApiLevel() >= 30) {
        if (access("/apex/com.android.adbd/bin/adbd", F_OK) != 0) {
            PLOGE("access /apex/com.android.adbd/bin/adbd");
            LOGW("Apex not exists on API 30+ device");
        } else {
            LOGI("Use adbd from /apex");
            return setup_adb_root_apex(root_path, my_file);
        }
    }

    if (access("/system/bin/adbd", F_OK) != 0) {
        PLOGE("access /system/bin/adbd");
        LOGW("No adbd");
        return ERR_NO_ADBD;
    }

    LOGI("Use adbd from /system");
    return setup_adb_root_non_apex(root_path, my_file);
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
