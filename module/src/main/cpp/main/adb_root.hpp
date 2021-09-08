#include <sys/mount.h>
#include <private/ScopedFd.h>
#include <sys/mman.h>
#include <cinttypes>
#include <selinux.h>
#include <logging.h>
#include <misc.h>

struct attrs {
    uid_t uid;
    gid_t gid;
    mode_t mode;
    char *context;

    ~attrs() {
        if (context) {
            freecon(context);
        }
    }
};

static bool getattrs(const char *file, attrs *attrs) {
    struct stat statbuf{};
    if (stat(file, &statbuf) != 0) {
        PLOGE("stat %s", file);
        return false;
    }

    if (getfilecon_raw(file, &attrs->context) < 0) {
        PLOGE("getfilecon %s", file);
        return false;
    }

    attrs->uid = statbuf.st_uid;
    attrs->gid = statbuf.st_gid;
    attrs->mode = statbuf.st_mode;
    return true;
}

static bool setattrs(const char *file, const attrs *attrs) {
    uid_t uid = attrs->uid;
    gid_t gid = attrs->gid;
    mode_t mode = attrs->mode;
    const char *secontext = attrs->context;

    if (chmod(file, mode) != 0) {
        PLOGE("chmod %s", file);
        return false;
    }
    if (chown(file, uid, gid) != 0) {
        PLOGE("chmod %s", file);
        return false;
    }
    if (setfilecon_raw(file, secontext) != 0) {
        PLOGE("setfilecon %s", file);
        return false;
    }
    return true;
}

static bool setup_file(const char *source, const char *target, const attrs *attrs) {
    if (copyfile(source, target) != 0) {
        PLOGE("copyfile %s -> %s", source, target);
        return false;
    }

    if (!setattrs(target, attrs)) {
        return false;
    }

    return true;
}

static bool setup_adb_root(const char *root_path) {
    if (selinux_check_access("u:r:adbd:s0", "u:r:adbd:s0", "process", "setcurrent", nullptr) != 0) {
        LOGE("adbd adbd process setcurrent not allowed");
        return false;
    }

    if (selinux_check_access("u:r:adbd:s0", "u:r:magisk:s0", "process", "dyntransition", nullptr) != 0) {
        LOGE("adbd adbd process setcurrent not allowed");
        return false;
    }

    char my_file[PATH_MAX]{0}, my_backup[PATH_MAX]{0};
    strcpy(my_file, root_path);
    strcat(my_file, "/bin/adbd_wrapper");
    strcpy(my_backup, root_path);
    strcat(my_backup, "/bin/adbd_real");

    LOGI("Prepare adbd files");

    const char *file = nullptr, *backup = nullptr, *folder = nullptr;
    attrs file_attr{}, folder_attr{};

    bool isApex = false;
    if (android::GetApiLevel() >= 30) {
        if (access("/apex/com.android.adbd/bin/adbd", F_OK) == 0) {
            LOGI("Use adbd from /apex");
            isApex = true;
            file = "/apex/com.android.adbd/bin/adbd";
            backup = "/apex/com.android.adbd/bin/adbd_real";
            folder = "/apex/com.android.adbd/bin";

            if (copyfile(file, my_backup) != 0) {
                PLOGE("copyfile %s -> %s", file, my_backup);
                return false;
            }

            if (!getattrs(file, &file_attr)
                || !getattrs(folder, &folder_attr)) {
                return false;
            }
        } else {
            PLOGE("access /apex/com.android.adbd/bin/adbd");
            LOGW("Apex not exists on API 31+ device");
        }
    }

    if (!file) {
        if (access("/system/bin/adbd", F_OK) == 0) {
            LOGI("Use adbd from /system");
            file = "/system/bin/adbd";
            backup = "/system/bin/adbd_real";
            folder = "/system/bin";
        } else {
            PLOGE("access /system/bin/adbd");
            LOGW("No adbd");
            return false;
        }
    }

    LOGV("%s: uid=%d, gid=%d, mode=%3o, context=%s",
         file, file_attr.uid, file_attr.gid, file_attr.mode, file_attr.context);

    LOGV("%s: uid=%d, gid=%d, mode=%3o, context=%s",
         folder, folder_attr.uid, folder_attr.gid, folder_attr.mode, folder_attr.context);

    if (isApex) {
        LOGI("Mount /apex/com.android.adbd/bin tmpfs");

        if (mount("tmpfs", folder, "tmpfs", 0, "mode=755") != 0) {
            PLOGE("mount tmpfs -> %s", folder);
            return false;
        }
        if (!setattrs(folder, &folder_attr)
            || !setup_file(my_file, file, &file_attr)) {
            umount(folder);
            return false;
        }

        if (file_attr.context) {
            freecon(file_attr.context);
        }
        file_attr.context = strdup("u:object_r:magisk_file:s0");

        if (!setup_file(my_backup, backup, &file_attr)) {
            umount(folder);
            return false;
        }

        LOGI("Finished");
        return true;
    } else {
        // TODO copy files to MODDIR/system/bin
    }
}

static int adb_root_main(int argc, char **argv) {
    LOGI("Setup adb root support: %s", argv[1]);

    if (init_selinux()) {
        auto root_path = argv[1];
        return setup_adb_root(root_path) ? 0 : 1;
    } else {
        LOGW("Cannot load libselinux");
        return 1;
    }
}
