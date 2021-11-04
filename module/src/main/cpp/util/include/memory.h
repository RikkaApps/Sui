#ifndef MEMORY_H
#define MEMORY_H

int CreateSharedMem(const char *name, size_t size);
int SetSharedMemProt(int fd, int prot);

#endif
