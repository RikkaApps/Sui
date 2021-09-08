#ifndef _SELINUX_H
#define _SELINUX_H

bool init_selinux();
void freecon(char *con);
int getfilecon_raw(const char *path, char **con);
int setfilecon_raw(const char *path, const char *context);
int selinux_check_access(const char * scon, const char * tcon, const char *tclass, const char *perm, void *auditdata);

#endif // _SELINUX_H
