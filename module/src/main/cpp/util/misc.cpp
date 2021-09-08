/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

#include <cstdio>
#include <cstring>
#include <sys/types.h>
#include <unistd.h>
#include <fcntl.h>
#include <cerrno>
#include <sys/sendfile.h>
#include <sys/stat.h>
#include <cstdlib>
#include <dirent.h>
#include "misc.h"

int mkdirs(const char *pathname, mode_t mode) {
    char *path = strdup(pathname), *p;
    errno = 0;
    for (p = path + 1; *p; ++p) {
        if (*p == '/') {
            *p = '\0';
            if (mkdir(path, mode) == -1) {
                if (errno != EEXIST)
                    return -1;
            }
            *p = '/';
        }
    }
    if (mkdir(path, mode) == -1) {
        if (errno != EEXIST)
            return -1;
    }
    free(path);
    return 0;
}

int ensure_dir(const char *path, mode_t mode) {
    if (access(path, R_OK) == -1)
        return mkdirs(path, mode);

    return 0;
}

int copyfileat(int src_path_fd, const char *src_path, int dst_path_fd, const char *dst_path) {
    int src_fd;
    int dst_fd;
    struct stat stat_buf{};
    int64_t size_remaining;
    size_t count;
    ssize_t result;

    if ((src_fd = openat(src_path_fd, src_path, O_RDONLY)) == -1)
        return -1;

    if (fstat(src_fd, &stat_buf) == -1)
        return -1;

    dst_fd = openat(dst_path_fd, dst_path, O_WRONLY | O_CREAT | O_TRUNC, stat_buf.st_mode);
    if (dst_fd == -1) {
        close(src_fd);
        return -1;
    }

    size_remaining = stat_buf.st_size;
    for (;;) {
        if (size_remaining > 0x7ffff000)
            count = 0x7ffff000;
        else
            count = static_cast<size_t>(size_remaining);

        result = sendfile(dst_fd, src_fd, nullptr, count);
        if (result == -1) {
            close(src_fd);
            close(dst_fd);
            unlink(dst_path);
            return -1;
        }

        size_remaining -= result;
        if (size_remaining == 0) {
            close(src_fd);
            close(dst_fd);
            return 0;
        }
    }
}

int copyfile(const char *src_path, const char *dst_path) {
    return copyfileat(0, src_path, 0, dst_path);
}

ssize_t read_eintr(int fd, void *out, size_t len) {
    ssize_t ret;
    do {
        ret = read(fd, out, len);
    } while (ret < 0 && errno == EINTR);
    return ret;
}

int read_full(int fd, void *out, size_t len) {
    while (len > 0) {
        ssize_t ret = read_eintr(fd, out, len);
        if (ret <= 0) {
            return -1;
        }
        out = (void *) ((uintptr_t) out + ret);
        len -= ret;
    }
    return 0;
}

int write_full(int fd, const void *buf, size_t count) {
    while (count > 0) {
        ssize_t size = write(fd, buf, count < SSIZE_MAX ? count : SSIZE_MAX);
        if (size == -1) {
            if (errno == EINTR)
                continue;
            else
                return -1;
        }

        buf = (const void *) ((uintptr_t) buf + size);
        count -= size;
    }
    return 0;
}

int is_num(const char *s) {
    size_t len = strlen(s);
    for (size_t i = 0; i < len; ++i)
        if (s[i] < '0' || s[i] > '9')
            return 0;
    return 1;
}

void foreach_proc(foreach_proc_function *func) {
    DIR *dir;
    struct dirent *entry;

    if (!(dir = opendir("/proc")))
        return;

    while ((entry = readdir(dir))) {
        if (entry->d_type != DT_DIR) continue;
        if (!is_num(entry->d_name)) continue;
        pid_t pid = atoi(entry->d_name);
        if (func(pid)) break;
    }

    closedir(dir);
}
