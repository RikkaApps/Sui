#ifndef _MISC_H
#define _MISC_H

#include <string>
#include <sys/types.h>

int mkdirs(const char *pathname, mode_t mode);
int ensure_dir(const char *path, mode_t mode);
int copyfileat(int src_path_fd, const char *src_path, int dst_path_fd, const char *dst_path);
int copyfile(const char *src_path, const char *dst_path);
int read_full(int fd, void *buf, size_t count);
int write_full(int fd, const void *buf, size_t count);

using foreach_proc_function = bool(pid_t);
void foreach_proc(foreach_proc_function *func);

#endif // _MISC_H
