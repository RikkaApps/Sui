package moe.shizuku.server;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuClient;
import moe.shizuku.server.IShizukuServiceConnection;

interface IShizukuService {

    int getVersion() = 2;

    int getUid() = 3;

    /* Check permission at remote process. Uncessary for Sui. */
    int checkPermission(String permission) = 4;

    IRemoteProcess newProcess(in String[] cmd, in String[] env, in String dir) = 7;

    String getSELinuxContext() = 8;

    String getSystemProperty(in String name, in String defaultValue) = 9;

    void setSystemProperty(in String name, in String value) = 10;

    int addUserService(in IShizukuServiceConnection conn, in Bundle args) = 11;

    int removeUserService(in IShizukuServiceConnection conn, in Bundle args) = 12;

    // ----------------------------

    void exit() = 100;

    void sendUserService(in IBinder binder, in Bundle options) = 101;

    oneway void packageChanged(in Intent intent) = 102;

    // ----------------------------

    void attachClientProcess(in IShizukuClient client, String requestPackageName) = 10000;

    void requestPermission(int requestCode) = 10001;

    boolean isHidden(int uid) = 10002;
 }
