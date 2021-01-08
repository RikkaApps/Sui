package moe.shizuku.server;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuApplication;
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

    void attachApplication(in IShizukuApplication application, String requestPackageName) = 10000;

    void requestPermission(int requestCode) = 10001;

    boolean isHidden(int uid) = 10002;

    oneway void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, in Bundle data) = 10003;

    void updateFlagsForUid(int uid, int mask, int value) = 10004;
 }
