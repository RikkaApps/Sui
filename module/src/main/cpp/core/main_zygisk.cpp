#include <zygisk.hpp>
#include <cstring>
#include <cerrno>
#include <logging.h>
#include <cstdio>
#include <sys/socket.h>
#include <bit>
#include <unistd.h>
#include <config.h>
#include <dex_file.h>
#include <memory.h>
#include <sys/mman.h>
#include <misc.h>
#include <nativehelper/scoped_utf_chars.h>
#include <fcntl.h>
#include <cinttypes>
#include "system_server.h"
#include "main.h"
#include "settings_process.h"
#include "manager_process.h"

namespace {
    inline constexpr auto kProcessNameMax = 256;

    enum Identity : int {

        IGNORE = 0,
        SYSTEM_SERVER = 1,
        SYSTEM_UI = 2,
        SETTINGS = 3,
    };

    ssize_t xsendmsg(int sockfd, const struct msghdr *msg, int flags) {
        int sent = sendmsg(sockfd, msg, flags);
        if (sent < 0) {
            PLOGE("sendmsg");
        }
        return sent;
    }

    ssize_t xrecvmsg(int sockfd, struct msghdr *msg, int flags) {
        int rec = recvmsg(sockfd, msg, flags);
        if (rec < 0) {
            PLOGE("recvmsg");
        }
        return rec;
    }

    int send_fds(int sockfd, void *cmsgbuf, size_t bufsz, const int *fds, int cnt) {
        iovec iov = {
                .iov_base = &cnt,
                .iov_len  = sizeof(cnt),
        };
        msghdr msg = {
                .msg_iov        = &iov,
                .msg_iovlen     = 1,
        };

        if (cnt) {
            msg.msg_control = cmsgbuf;
            msg.msg_controllen = bufsz;
            cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
            cmsg->cmsg_len = CMSG_LEN(sizeof(int) * cnt);
            cmsg->cmsg_level = SOL_SOCKET;
            cmsg->cmsg_type = SCM_RIGHTS;

            memcpy(CMSG_DATA(cmsg), fds, sizeof(int) * cnt);
        }

        return xsendmsg(sockfd, &msg, 0);
    }

    int send_fd(int sockfd, int fd) {
        if (fd < 0) {
            return send_fds(sockfd, nullptr, 0, nullptr, 0);
        }
        char cmsgbuf[CMSG_SPACE(sizeof(int))];
        return send_fds(sockfd, cmsgbuf, sizeof(cmsgbuf), &fd, 1);
    }

    void *recv_fds(int sockfd, char *cmsgbuf, size_t bufsz, int cnt) {
        iovec iov = {
                .iov_base = &cnt,
                .iov_len  = sizeof(cnt),
        };
        msghdr msg = {
                .msg_iov        = &iov,
                .msg_iovlen     = 1,
                .msg_control    = cmsgbuf,
                .msg_controllen = bufsz
        };

        xrecvmsg(sockfd, &msg, MSG_WAITALL);
        cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);

        if (msg.msg_controllen != bufsz ||
            cmsg == nullptr ||
            cmsg->cmsg_len != CMSG_LEN(sizeof(int) * cnt) ||
            cmsg->cmsg_level != SOL_SOCKET ||
            cmsg->cmsg_type != SCM_RIGHTS) {
            return nullptr;
        }

        return CMSG_DATA(cmsg);
    }

    int recv_fd(int sockfd) {
        char cmsgbuf[CMSG_SPACE(sizeof(int))];

        void *data = recv_fds(sockfd, cmsgbuf, sizeof(cmsgbuf), 1);
        if (data == nullptr)
            return -1;

        int result;
        memcpy(&result, data, sizeof(int));
        return result;
    }

    int read_int(int fd) {
        int val;
        if (read_full(fd, &val, sizeof(val)) != 0)
            return -1;
        return val;
    }

    void write_int(int fd, int val) {
        if (fd < 0) return;
        write_full(fd, &val, sizeof(val));
    }
}

class ZygiskModule : public zygisk::ModuleBase {

public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        api_ = api;
        env_ = env;
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        char process_name[kProcessNameMax]{0};
        char app_data_dir[PATH_MAX]{0};

        if (args->nice_name) {
            ScopedUtfChars niceName{env_, args->nice_name};
            strcpy(process_name, niceName.c_str());
        }

#ifdef DEBUG
        if (args->app_data_dir) {
            ScopedUtfChars appDataDir{env_, args->app_data_dir};
            strcpy(app_data_dir, appDataDir.c_str());
        }
#endif
        LOGD("preAppSpecialize: %s %s", process_name, app_data_dir);

        InitCompanion(false, args->uid, process_name);

        if (whoami == Identity::IGNORE) {
            api_->setOption(zygisk::Option::DLCLOSE_MODULE_LIBRARY);
        }

        UmountApexAdbd();
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override {
        LOGD("postAppSpecialize");

        if (whoami == Identity::IGNORE) {
            return;
        }

        char app_data_dir[PATH_MAX]{0};

        if (args->app_data_dir) {
            ScopedUtfChars appDataDir{env_, args->app_data_dir};
            strcpy(app_data_dir, appDataDir.c_str());
        }

        if (whoami == Identity::SETTINGS) {
            Settings::main(env_, app_data_dir, dex);
        } else if (whoami == Identity::SYSTEM_UI) {
            Manager::main(env_, app_data_dir, dex);
        }
    }

    void preServerSpecialize(zygisk::ServerSpecializeArgs *args) override {
        LOGD("preServerSpecialize");

        InitCompanion(true, args->uid);
    }

    void postServerSpecialize(const zygisk::ServerSpecializeArgs *args) override {
        LOGD("postServerSpecialize");

        SystemServer::main(env_, dex);
    }

private:
    zygisk::Api *api_{};
    JNIEnv *env_{};

    Identity whoami = Identity::IGNORE;
    Dex *dex = nullptr;

    void InitCompanion(bool is_system_server, int uid, const char *process_name = nullptr) {
        auto companion = api_->connectCompanion();
        if (companion == -1) {
            LOGE("Zygote: failed to connect to companion");
            return;
        }

        if (is_system_server) {
            write_int(companion, 1);
            whoami = Identity::SYSTEM_SERVER;
        } else {
            write_int(companion, 0);
            write_int(companion, uid);
            write_full(companion, process_name, kProcessNameMax);
            whoami = static_cast<Identity>(read_int(companion));
        }

        if (whoami != Identity::IGNORE) {
            auto fd = recv_fd(companion);
            auto size = (size_t) read_int(companion);

            if (whoami == Identity::SETTINGS) {
                LOGI("Zygote: in Settings");
            } else if (whoami == Identity::SYSTEM_UI) {
                LOGI("Zygote: in SystemUi");
            } else {
                LOGI("Zygote: in SystemServer");
            }

            LOGI("Zygote: dex fd is %d, size is %" PRIdPTR, fd, size);
            dex = new Dex(fd, size);
            close(fd);
        }

        close(companion);
    }
};

static int dex_mem_fd = -1;
static size_t dex_size = 0;
static uid_t manager_uid = -1, settings_uid = -1;
static char manager_process[kProcessNameMax], settings_process[kProcessNameMax];

static void ReadApplicationInfo(const char *package, uid_t &uid, char *process) {
    char buf[PATH_MAX];
    snprintf(buf, PATH_MAX, "/data/adb/modules/%s/%s", ZYGISK_MODULE_ID, package);
    auto file = Buffer(buf);
    auto bytes = file.data();
    auto size = file.size();
    for (int i = 0; i < size; ++i) {
        if (bytes[i] == '\n') {
            memset(process, 0, 256);
            memcpy(process, bytes + i + 1, size - i - 1);
            bytes[i] = 0;
            uid = atoi((char *) bytes);
            break;
        }
    }
}

static bool PrepareCompanion() {
    bool result = false;

    auto path = "/data/adb/modules/" ZYGISK_MODULE_ID "/" DEX_NAME;
    int fd = open(path, O_RDONLY);
    ssize_t size;

    if (fd == -1) {
        PLOGE("open %s", path);
        goto cleanup;
    }

    size = lseek(fd, 0, SEEK_END);
    if (size == -1) {
        PLOGE("lseek %s", path);
        goto cleanup;
    }
    lseek(fd, 0, SEEK_SET);

    LOGD("Companion: dex size is %" PRIdPTR, size);

    dex_mem_fd = CreateSharedMem("sui.dex", size);
    if (dex_mem_fd >= 0) {
        auto addr =(uint8_t *) mmap(nullptr, size, PROT_WRITE, MAP_SHARED, dex_mem_fd, 0);
        if (addr != MAP_FAILED) {
            read_full(fd, addr, size);
            dex_size = size;
            munmap(addr, size);
        }
        SetSharedMemProt(dex_mem_fd, PROT_READ);
    }

    LOGI("Companion: dex fd is %d", dex_mem_fd);

    ReadApplicationInfo(MANAGER_APPLICATION_ID, manager_uid, manager_process);
    ReadApplicationInfo(SETTINGS_APPLICATION_ID, settings_uid, settings_process);

    LOGI("Companion: SystemUI %d %s", manager_uid, manager_process);
    LOGI("Companion: Settings %d %s", settings_uid, settings_process);

    result = true;

    cleanup:
    if (fd != -1) close(fd);

    return result;
}

static void CompanionEntry(int socket) {
    static auto prepare = PrepareCompanion();

    char process_name[kProcessNameMax]{0};
    Identity whoami;

    int is_system_server = read_int(socket) == 1;
    if (is_system_server != 0) {
        whoami = Identity::SYSTEM_SERVER;
    } else {
        int uid = read_int(socket);
        read_full(socket, process_name, kProcessNameMax);

        if (uid == manager_uid && strcmp(process_name, manager_process) == 0) {
            whoami = Identity::SYSTEM_UI;
        } else if (uid == settings_uid && strcmp(process_name, settings_process) == 0) {
            whoami = Identity::SETTINGS;
        } else {
            whoami = Identity::IGNORE;
        }

        write_int(socket, whoami);
    }

    if (whoami != Identity::IGNORE) {
        send_fd(socket, dex_mem_fd);
        write_int(socket, dex_size);
    }

    close(socket);
}

REGISTER_ZYGISK_MODULE(ZygiskModule)

REGISTER_ZYGISK_COMPANION(CompanionEntry)
