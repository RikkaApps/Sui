#ifndef SOCKET_H
#define SOCKET_H

socklen_t setup_sockaddr(struct sockaddr_un *sun, const char *name);
int set_socket_timeout(int fd, long sec);
int send_fd(int sockfd, int fd);
int recv_fd(int sockfd);
int read_int(int fd);
void write_int(int fd, int val);
#endif // SOCKET_H
