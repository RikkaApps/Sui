#include <cstring>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/uio.h>
#include <unistd.h>
#include <cerrno>

#include <socket.h>
#include <logging.h>
#include <misc.h>

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
    struct timeval timeout{};
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
