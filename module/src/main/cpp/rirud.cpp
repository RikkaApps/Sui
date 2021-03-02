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

#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cinttypes>
#include "riru.h"
#include "rirud.h"
#include "logging.h"
#include "socket.h"
#include "misc.h"

#define SOCKET_ADDRESS "rirud"

static const uint32_t ACTION_READ_FILE = 4;

bool rirud::ReadFile(const char *path, char *&bytes, size_t &bytes_size) {
    if (riru_api_version < 10) return false;

    struct sockaddr_un addr{};
    uint32_t path_size = strlen(path);
    int32_t reply;
    int32_t file_size;
    int fd;
    socklen_t socklen;
    uint32_t buffer_size = 1024 * 8;
    bool res = false;

    bytes = nullptr;
    bytes_size = 0;

    if ((fd = socket(PF_UNIX, SOCK_STREAM | SOCK_CLOEXEC, 0)) < 0) {
        PLOGE("socket");
        goto clean;
    }

    socklen = setup_sockaddr(&addr, SOCKET_ADDRESS);

    if (connect(fd, (struct sockaddr *) &addr, socklen) == -1) {
        PLOGE("connect %s", SOCKET_ADDRESS);
        goto clean;
    }

    if (write_full(fd, &ACTION_READ_FILE, sizeof(uint32_t)) != 0
        || write_full(fd, &path_size, sizeof(uint32_t)) != 0
        || write_full(fd, path, path_size) != 0) {
        PLOGE("write %s", SOCKET_ADDRESS);
        goto clean;
    }

    if (read_full(fd, &reply, sizeof(int32_t)) != 0) {
        PLOGE("read %s", SOCKET_ADDRESS);
        goto clean;
    }

    if (reply != 0) {
        LOGE("open %s failed with %d from remote: %s", path, reply, strerror(reply));
        errno = reply;
        goto clean;
    }

    if (read_full(fd, &file_size, sizeof(uint32_t)) != 0) {
        PLOGE("read %s", SOCKET_ADDRESS);
        goto clean;
    }

    LOGD("%s size %d", path, file_size);

    if (file_size > 0) {
        bytes = (char *) malloc(file_size);
        while (file_size > 0) {
            LOGD("attempt to read %d bytes", (int) buffer_size);
            auto read_size = TEMP_FAILURE_RETRY(read(fd, bytes + bytes_size, buffer_size));
            if (read_size == -1) {
                PLOGE("read");
                goto clean;
            }

            file_size -= read_size;
            bytes_size += read_size;
            LOGD("read %d bytes (total %d)", (int) read_size, (int) bytes_size);
        }
        res = true;
    } else if (file_size == 0) {
        while (true) {
            if (bytes == nullptr) {
                bytes = (char *) malloc(buffer_size);
            } else {
                bytes = (char *) realloc(bytes, bytes_size + buffer_size);
            }

            LOGD("attempt to read %d bytes", (int) buffer_size);
            auto read_size = TEMP_FAILURE_RETRY(read(fd, bytes + bytes_size, buffer_size));
            if (read_size == -1) {
                PLOGE("read");
                goto clean;
            }
            if (read_size == 0) {
                res = true;
                goto clean;
            }

            bytes_size += read_size;
            LOGD("read %d bytes (total %d)", (int) read_size, (int) bytes_size);
        }
    }


    clean:
    if (fd != -1) close(fd);
    return res;
}
