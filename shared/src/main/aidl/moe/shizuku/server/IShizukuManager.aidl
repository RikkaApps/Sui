package moe.shizuku.server;

interface IShizukuManager {

    void showPermissionConfirmation(int requestUid, int requestPid, in String requestPackageName, int requestCode) = 1;
}