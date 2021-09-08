#include <cstring>
#include <cerrno>
#include <unistd.h>
#include <syscall.h>
#include <malloc.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <sys/xattr.h>

#define INITCONTEXTLEN 255

void freecon(char *con) {
    free(con);
}

int getfilecon_raw(const char *path, char **context) {
    char *buf;
    ssize_t size;
    ssize_t ret;

    size = INITCONTEXTLEN + 1;
    buf = static_cast<char *>(malloc(size));
    if (!buf)
        return -1;
    memset(buf, 0, size);

    ret = getxattr(path, XATTR_NAME_SELINUX, buf, size - 1);
    if (ret < 0 && errno == ERANGE) {
        char *newbuf;

        size = getxattr(path, XATTR_NAME_SELINUX, nullptr, 0);
        if (size < 0)
            goto out;

        size++;
        newbuf = static_cast<char *>(realloc(buf, size));
        if (!newbuf)
            goto out;

        buf = newbuf;
        memset(buf, 0, size);
        ret = getxattr(path, XATTR_NAME_SELINUX, buf, size - 1);
    }
    out:
    if (ret == 0) {
        /* Re-map empty attribute values to errors. */
        errno = ENOTSUP;
        ret = -1;
    }
    if (ret < 0)
        free(buf);
    else
        *context = buf;
    return ret;
}

int setfilecon_raw(const char *path, const char *context) {
    int rc = setxattr(path, XATTR_NAME_SELINUX, context, strlen(context) + 1, 0);
    if (rc < 0 && errno == ENOTSUP) {
        char *ccontext = nullptr;
        int err = errno;
        if ((getfilecon_raw(path, &ccontext) >= 0) &&
            (strcmp(context, ccontext) == 0)) {
            rc = 0;
        } else {
            errno = err;
        }
        freecon(ccontext);
    }
    return rc;
}

using selinux_check_access_t = int(const char *, const char *, const char *, const char *, void *);
selinux_check_access_t *selinux_check_access_func = nullptr;

int selinux_check_access(const char *scon, const char *tcon, const char *tclass, const char *perm, void *auditdata) {
    if (selinux_check_access_func) {
        return selinux_check_access_func(scon, tcon, tclass, perm, auditdata);
    }
    return 0;
}

#ifdef __LP64__
static constexpr const char *libselinux = "/system/lib64/libselinux.so";
#else
static constexpr const char *libselinux = "/system/lib/libselinux.so";
#endif

bool init_selinux() {
    if (access(libselinux, F_OK) != 0) {
        return false;
    }

    void *handle = dlopen(libselinux, RTLD_LAZY | RTLD_LOCAL);
    if (!handle) return false;

    selinux_check_access_func = (selinux_check_access_t *) dlsym(handle, "selinux_check_access");
    return selinux_check_access_func != nullptr;
}
