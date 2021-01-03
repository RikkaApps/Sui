#include <cstring>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/uio.h>
#include <unistd.h>
#include <cerrno>

#include "socket.h"

static size_t socket_len(sockaddr_un *sun) {
    if (sun->sun_path[0])
        return sizeof(sa_family_t) + strlen(sun->sun_path) + 1;
    else
        return sizeof(sa_family_t) + strlen(sun->sun_path + 1) + 1;
}

socklen_t setup_sockaddr(sockaddr_un *sun, const char *name) {
    memset(sun, 0, sizeof(*sun));
    sun->sun_family = AF_UNIX;
    strcpy(sun->sun_path + 1, name);
    return socket_len(sun);
}

int set_socket_timeout(int fd, long sec) {
    struct timeval timeout;
    timeout.tv_sec = sec;
    timeout.tv_usec = 0;

    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (char *) &timeout, sizeof(timeout)) < 0) {
        return -1;
    }

    if (setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, (char *) &timeout, sizeof(timeout)) < 0) {
        return -1;
    }
    return 0;
}