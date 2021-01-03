#ifndef SOCKET_H
#define SOCKET_H

#define SOCKET_SERVER_ADDRESS "sr-process-handler"

socklen_t setup_sockaddr(struct sockaddr_un *sun, const char *name);
int set_socket_timeout(int fd, long sec);

#endif // SOCKET_H
