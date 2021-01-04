package moe.shizuku.server;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuClient;
import moe.shizuku.server.IShizukuManager;
import moe.shizuku.server.IShizukuServiceConnection;

interface IShizukuService {

    IRemoteProcess newProcess(in String[] cmd, in String[] env, in String dir) = 7;

    String getSystemProperty(in String name, in String defaultValue) = 9;

    void setSystemProperty(in String name, in String value) = 10;

    int addUserService(in IShizukuServiceConnection conn, in Bundle args) = 11;

    int removeUserService(in IShizukuServiceConnection conn, in Bundle args) = 12;

    // ----------------------------

    void sendUserService(in IBinder binder, in Bundle options) = 101;

    oneway void onPackageChanged(in Intent intent) = 102;

    // ----------------------------

    void attachClient(in IShizukuClient client, String requestPackageName) = 10000;

    void requestPermission(int requestCode) = 10001;

    boolean isHidden(int uid) = 10002;

    void attachManager(in IShizukuManager manager) = 10003;

    oneway void onPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, in Bundle data) = 10004;
 }
