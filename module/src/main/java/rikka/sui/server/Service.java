package rikka.sui.server;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.ArrayMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import hidden.HiddenApiBridge;
import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuApplication;
import moe.shizuku.server.IShizukuService;
import moe.shizuku.server.IShizukuServiceConnection;
import rikka.shizuku.ShizukuApiConstants;
import rikka.sui.model.AppInfo;
import rikka.sui.server.api.RemoteProcessHolder;
import rikka.sui.server.api.SystemService;
import rikka.sui.server.bridge.BridgeServiceClient;
import rikka.sui.server.config.Config;
import rikka.sui.server.config.ConfigManager;
import rikka.sui.server.userservice.UserService;
import rikka.sui.server.userservice.UserServiceRecord;
import rikka.sui.util.OsUtils;
import rikka.sui.util.ParceledListSlice;
import rikka.sui.util.UserHandleCompat;

import static rikka.shizuku.ShizukuApiConstants.ATTACH_REPLY_PERMISSION_GRANTED;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_REPLY_SERVER_SECONTEXT;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_REPLY_SERVER_UID;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_REPLY_SERVER_VERSION;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_REPLY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_COMPONENT;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_DEBUGGABLE;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_PROCESS_NAME;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_TAG;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_VERSION_CODE;
import static rikka.sui.server.ServerConstants.LOGGER;

public class Service extends IShizukuService.Stub {

    private static Service instance;

    public static Service getInstance() {
        return instance;
    }

    public static void main() {
        LOGGER.i("starting server...");

        Looper.prepare();
        new Service();
        Looper.loop();

        LOGGER.i("server exited");
        System.exit(0);
    }

    private static final String MANAGER_APPLICATION_ID = "com.android.systemui";

    private final Map<String, UserServiceRecord> userServiceRecords = Collections.synchronizedMap(new ArrayMap<>());
    private final ClientManager clientManager;
    private final ConfigManager configManager;
    private final int managerUid;
    private IShizukuApplication managerApplication;

    private final IBinder.DeathRecipient managerDeathRecipient = () -> {
        LOGGER.w("manager binder is dead");
        managerApplication = null;
    };

    public Service() {
        Service.instance = this;

        configManager = ConfigManager.getInstance();
        clientManager = ClientManager.getInstance();

        while (true) {
            ApplicationInfo ai = SystemService.getApplicationInfoNoThrow(MANAGER_APPLICATION_ID, 0, 0);
            if (ai != null) {
                managerUid = ai.uid;
                break;
            }

            LOGGER.w("can't find manager package, wait 1s");

            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        LOGGER.i("uid for %s is %d", MANAGER_APPLICATION_ID, managerUid);

        BridgeServiceClient.send(new BridgeServiceClient.Listener() {
            @Override
            public void onSystemServerRestarted() {
                LOGGER.w("system restarted...");
            }

            @Override
            public void onResponseFromBridgeService(boolean response) {
                if (response) {
                    LOGGER.i("send service to bridge");
                } else {
                    LOGGER.w("no response from bridge");
                }
            }
        });
    }

    private void enforceCallingPermission(String func) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid()) {
            return;
        }

        ClientRecord clientRecord = clientManager.findClient(callingUid, callingPid);
        if (clientRecord != null && clientRecord.allowed) {
            return;
        }

        if (clientRecord == null) {
            throw new SecurityException("Permission Denial: " + func + " from pid="
                    + Binder.getCallingPid()
                    + " is not an attached client");
        }

        if (!clientRecord.allowed) {
            throw new SecurityException("Permission Denial: " + func + " from pid="
                    + Binder.getCallingPid()
                    + " requires permission");
        }
    }

    private ClientRecord requireClient(int callingUid, int callingPid) {
        return requireClient(callingUid, callingPid, false);
    }

    private ClientRecord requireClient(int callingUid, int callingPid, boolean requiresPermission) {
        ClientRecord clientRecord = clientManager.findClient(callingUid, callingPid);
        if (clientRecord == null) {
            LOGGER.w("Caller (uid %d, pid %d) is not an attached client", callingUid, callingPid);
            throw new IllegalStateException("Not an attached client");
        }
        if (requiresPermission && !clientRecord.allowed) {
            throw new SecurityException("Caller has no permission");
        }
        return clientRecord;
    }

    private void transactRemote(Parcel data, Parcel reply, int flags) throws RemoteException {
        enforceCallingPermission("transactRemote");

        IBinder targetBinder = data.readStrongBinder();
        int targetCode = data.readInt();

        LOGGER.d("transact: uid=%d, descriptor=%s, code=%d", Binder.getCallingUid(), targetBinder.getInterfaceDescriptor(), targetCode);
        Parcel newData = Parcel.obtain();
        try {
            newData.appendFrom(data, data.dataPosition(), data.dataAvail());
        } catch (Throwable tr) {
            LOGGER.w(tr, "appendFrom");
            return;
        }
        try {
            long id = Binder.clearCallingIdentity();
            targetBinder.transact(targetCode, newData, reply, flags);
            Binder.restoreCallingIdentity(id);
        } finally {
            newData.recycle();
        }
    }

    @Override
    public IRemoteProcess newProcess(String[] cmd, String[] env, String dir) throws RemoteException {
        ClientRecord record = requireClient(Binder.getCallingUid(), Binder.getCallingPid(), true);

        LOGGER.d("newProcess: uid=%d, cmd=%s, env=%s, dir=%s", Binder.getCallingUid(), Arrays.toString(cmd), Arrays.toString(env), dir);

        Process process;
        try {
            process = Runtime.getRuntime().exec(cmd, env, dir != null ? new File(dir) : null);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }

        return new RemoteProcessHolder(process, record.client.asBinder());
    }

    @Override
    public String getSystemProperty(String name, String defaultValue) throws RemoteException {
        enforceCallingPermission("getSystemProperty");

        try {
            return SystemProperties.get(name, defaultValue);
        } catch (Throwable tr) {
            throw new RemoteException(tr.getMessage());
        }
    }

    @Override
    public void setSystemProperty(String name, String value) throws RemoteException {
        enforceCallingPermission("setSystemProperty");

        try {
            SystemProperties.set(name, value);
        } catch (Throwable tr) {
            throw new RemoteException(tr.getMessage());
        }
    }

    private PackageInfo ensureCallingPackageForUserService(String packageName, int appId, int userId) {
        PackageInfo packageInfo = SystemService.getPackageInfoNoThrow(packageName, 0, userId);
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            throw new SecurityException("unable to find package " + packageName);
        }
        if (UserHandleCompat.getAppId(packageInfo.applicationInfo.uid) != appId) {
            throw new SecurityException("package " + packageName + " is not owned by " + appId);
        }
        return packageInfo;
    }

    @Override
    public int removeUserService(IShizukuServiceConnection conn, Bundle options) {
        enforceCallingPermission("removeUserService");

        ComponentName componentName = Objects.requireNonNull(options.getParcelable(USER_SERVICE_ARG_COMPONENT), "component is null");

        int uid = Binder.getCallingUid();
        int appId = UserHandleCompat.getAppId(uid);
        int userId = UserHandleCompat.getUserId(uid);

        String packageName = componentName.getPackageName();
        ensureCallingPackageForUserService(packageName, appId, userId);

        String className = Objects.requireNonNull(componentName.getClassName(), "class is null");
        String tag = options.getString(USER_SERVICE_ARG_TAG);
        String key = packageName + ":" + (tag != null ? tag : className);

        synchronized (this) {
            UserServiceRecord record = getUserServiceRecordLocked(key);
            if (record == null) return 1;
            removeUserServiceLocked(record);
        }
        return 0;
    }

    private void removeUserServiceLocked(UserServiceRecord record) {
        if (userServiceRecords.values().remove(record)) {
            record.destroy();
        }
    }

    @Override
    public int addUserService(IShizukuServiceConnection conn, Bundle options) {
        enforceCallingPermission("addUserService");

        Objects.requireNonNull(conn, "connection is null");
        Objects.requireNonNull(options, "options is null");

        int uid = Binder.getCallingUid();
        int appId = UserHandleCompat.getAppId(uid);
        int userId = UserHandleCompat.getUserId(uid);

        ComponentName componentName = Objects.requireNonNull(options.getParcelable(USER_SERVICE_ARG_COMPONENT), "component is null");
        String packageName = Objects.requireNonNull(componentName.getPackageName(), "package is null");
        PackageInfo packageInfo = ensureCallingPackageForUserService(packageName, appId, userId);

        String className = Objects.requireNonNull(componentName.getClassName(), "class is null");
        String sourceDir = Objects.requireNonNull(packageInfo.applicationInfo.sourceDir, "apk path is null");
        String nativeLibraryDir = packageInfo.applicationInfo.nativeLibraryDir;

        int versionCode = options.getInt(USER_SERVICE_ARG_VERSION_CODE, 1);
        String tag = options.getString(USER_SERVICE_ARG_TAG);
        String processNameSuffix = options.getString(USER_SERVICE_ARG_PROCESS_NAME);
        boolean debug = options.getBoolean(USER_SERVICE_ARG_DEBUGGABLE, false);
        boolean standalone = true;
        String key = packageName + ":" + (tag != null ? tag : className);

        synchronized (this) {
            UserServiceRecord record = getOrCreateUserServiceRecordLocked(key, versionCode, standalone, sourceDir);
            record.callbacks.register(conn);

            if (record.binder != null && record.binder.pingBinder()) {
                record.broadcastBinderReceived();
            } else if (!record.startScheduled) {
                record.startScheduled = true;
                UserService.schedule(record, key, record.token, packageName, className, processNameSuffix, uid, debug);
            }
            return 0;
        }
    }

    private UserServiceRecord getUserServiceRecordLocked(String key) {
        return userServiceRecords.get(key);
    }

    private UserServiceRecord getOrCreateUserServiceRecordLocked(String key, int versionCode, boolean standalone, String apkPath) {
        UserServiceRecord record = getUserServiceRecordLocked(key);
        if (record != null) {
            if (record.versionCode != versionCode) {
                LOGGER.v("remove service record %s (%s) because version code not matched (old=%d, new=%d)", key, record.token, record.versionCode, versionCode);
            } else if (record.binder == null || !record.binder.pingBinder()) {
                LOGGER.v("service in record %s (%s) has dead", key, record.token);
            } else {
                LOGGER.i("found existing service record %s (%s)", key, record.token);
                return record;
            }

            removeUserServiceLocked(record);
        }

        record = new UserServiceRecord(standalone, versionCode);
        userServiceRecords.put(key, record);
        LOGGER.i("new service record %s (%s): version=%d, standalone=%s, apk=%s", key, record.token, versionCode, Boolean.toString(standalone), apkPath);
        return record;
    }

    private void attachUserServiceLocked(IBinder binder, String token) {
        Map.Entry<String, UserServiceRecord> entry = null;
        for (Map.Entry<String, UserServiceRecord> e : userServiceRecords.entrySet()) {
            if (e.getValue().token.equals(token)) {
                entry = e;
                break;
            }
        }

        if (entry == null) {
            throw new IllegalArgumentException("unable to find token " + token);
        }

        LOGGER.v("received binder for service record %s", token);

        UserServiceRecord record = entry.getValue();
        record.binder = binder;
        record.broadcastBinderReceived();
        record.deathRecipient = () -> {
            LOGGER.v("binder in service record %s is dead", token);

            synchronized (Service.this) {
                removeUserServiceLocked(record);
            }
        };

        try {
            binder.linkToDeath(record.deathRecipient, 0);
        } catch (Throwable e) {
            LOGGER.w(e, "linkToDeath " + token);
        }
    }

    @Override
    public void attachUserService(IBinder binder, Bundle options) {
        Objects.requireNonNull(binder, "binder is null");
        String token = Objects.requireNonNull(options.getString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN), "token is null");

        synchronized (this) {
            attachUserServiceLocked(binder, token);
        }
    }

    @Override
    public void attachApplication(IShizukuApplication application, String requestPackageName) {
        if (application == null || requestPackageName == null) {
            return;
        }

        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        boolean isManager;
        ClientRecord clientRecord = null;

        List<String> packages = SystemService.getPackagesForUidNoThrow(callingUid);
        if (!packages.contains(requestPackageName)) {
            throw new SecurityException("Request package " + requestPackageName + "does not belong to uid " + callingUid);
        }

        isManager = MANAGER_APPLICATION_ID.equals(requestPackageName);
        if (isManager) {
            try {
                application.asBinder().linkToDeath(managerDeathRecipient, 0);
            } catch (RemoteException e) {
                LOGGER.w(e, "attachApplication");
            }
            managerApplication = application;
        }

        if (!isManager) {
            if (!isManager && clientManager.findClient(callingUid, callingPid) != null) {
                throw new IllegalStateException("Client (uid=" + callingUid + ", pid=" + callingPid + ") has already attached");
            }
            synchronized (this) {
                clientRecord = clientManager.addClient(callingUid, callingPid, application, requestPackageName);
            }
            if (clientRecord == null) {
                return;
            }
        }

        Bundle reply = new Bundle();
        reply.putInt(ATTACH_REPLY_SERVER_UID, OsUtils.getUid());
        reply.putInt(ATTACH_REPLY_SERVER_VERSION, ShizukuApiConstants.SERVER_VERSION);
        reply.putString(ATTACH_REPLY_SERVER_SECONTEXT, OsUtils.getSELinuxContext());
        if (!isManager) {
            reply.putBoolean(ATTACH_REPLY_PERMISSION_GRANTED, clientRecord.allowed);
            reply.putBoolean(ATTACH_REPLY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE, shouldShowRequestPermissionRationale(clientRecord));
        }
        try {
            application.bindApplication(reply);
        } catch (Throwable e) {
            LOGGER.w(e, "attachApplication");
        }
    }

    @Override
    public void requestPermission(int requestCode) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid() || callingPid == OsUtils.getPid()) {
            return;
        }

        ClientRecord clientRecord = requireClient(callingUid, callingPid);

        if (clientRecord.allowed) {
            clientRecord.dispatchRequestPermissionResult(requestCode, true);
            return;
        }

        Config.PackageEntry entry = configManager.find(callingUid);
        if (entry != null && entry.isDenied()) {
            clientRecord.dispatchRequestPermissionResult(requestCode, false);
            return;
        }

        if (managerApplication != null) {
            try {
                managerApplication.showPermissionConfirmation(callingUid, callingPid, clientRecord.packageName, requestCode);
            } catch (Throwable e) {
                LOGGER.w(e, "showPermissionConfirmation");
            }
        } else {
            LOGGER.e("manager is null");
        }
    }

    @Override
    public boolean checkSelfPermission() {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid() || callingPid == OsUtils.getPid()) {
            return true;
        }

        return requireClient(callingUid, callingPid).allowed;
    }

    private boolean shouldShowRequestPermissionRationale(ClientRecord record) {
        Config.PackageEntry entry = configManager.find(record.uid);
        return entry != null && entry.isDenied();
    }

    @Override
    public boolean shouldShowRequestPermissionRationale() {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid() || callingPid == OsUtils.getPid()) {
            return true;
        }

        return shouldShowRequestPermissionRationale(requireClient(callingUid, callingPid));
    }

    @Override
    public boolean isHidden(int uid) {
        if (Binder.getCallingUid() != 1000) {
            // only allow to be called by system server
            return false;
        }

        return uid != managerUid && configManager.isHidden(uid);
    }

    @Override
    public void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, Bundle data) {
        if (Binder.getCallingUid() != managerUid) {
            LOGGER.w("dispatchPermissionConfirmationResult is allowed to be called only from the manager");
            return;
        }

        if (data == null) {
            return;
        }

        boolean allowed = data.getBoolean(REQUEST_PERMISSION_REPLY_ALLOWED);
        boolean onetime = data.getBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME);

        LOGGER.i("dispatchPermissionConfirmationResult: uid=%d, pid=%d, requestCode=%d, allowed=%s, onetime=%s",
                requestUid, requestPid, requestCode, Boolean.toString(allowed), Boolean.toString(onetime));

        ClientRecord clientRecord = clientManager.findClient(requestUid, requestPid);
        if (clientRecord == null) {
            LOGGER.w("dispatchPermissionConfirmationResult: client (uid=%d, pid=%d) not found", requestUid, requestPid);
        } else {
            clientRecord.allowed = allowed;
            clientRecord.dispatchRequestPermissionResult(requestCode, allowed);
        }

        if (!onetime) {
            configManager.update(requestUid, Config.MASK_PERMISSION, allowed ? Config.FLAG_ALLOWED : Config.FLAG_DENIED);
        }
    }

    private int getFlagsForUidInternal(int uid, int mask) {
        Config.PackageEntry entry = configManager.find(uid);
        if (entry != null) {
            return entry.flags & mask;
        }
        return 0;
    }

    @Override
    public int getFlagsForUid(int uid, int mask) {
        if (Binder.getCallingUid() != managerUid) {
            LOGGER.w("updateFlagsForUid is allowed to be called only from the manager");
            return 0;
        }
        return getFlagsForUidInternal(uid, mask);
    }

    @Override
    public void updateFlagsForUid(int uid, int mask, int value) {
        if (Binder.getCallingUid() != managerUid) {
            LOGGER.w("updateFlagsForUid is allowed to be called only from the manager");
            return;
        }

        configManager.update(uid, mask, value);

        if ((mask & Config.MASK_PERMISSION) != 0) {
            boolean allowed = (value & Config.FLAG_ALLOWED) != 0;
            for (ClientRecord record : clientManager.findClients(uid)) {
                if (allowed) {
                    record.allowed = allowed;
                } else {
                    record.allowed = false;
                    SystemService.forceStopPackageNoThrow(record.packageName, UserHandleCompat.getUserId(record.uid));
                }
            }
        }
    }

    @Override
    public void dispatchPackageChanged(Intent intent) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000 && callingUid != 0) {
            return;
        }
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        if (Intent.ACTION_PACKAGE_REMOVED.equals(action) && uid > 0 & !replacing) {
            LOGGER.i("uid %d is removed", uid);
            configManager.remove(uid);
        }
    }

    private ParceledListSlice<AppInfo> getApplications(int userId) {
        if (Binder.getCallingUid() != managerUid) {
            LOGGER.w("getApplications is allowed to be called only from the manager");
            return null;
        }

        List<Integer> users = new ArrayList<>();
        if (userId == -1) {
            users.addAll(SystemService.getUserIdsNoThrow());
        } else {
            users.add(userId);
        }

        List<AppInfo> list = new ArrayList<>();
        for (int user : users) {
            for (PackageInfo pi : SystemService.getInstalledPackagesNoThrow(0x00002000 /*MATCH_UNINSTALLED_PACKAGES*/, user)) {
                if (pi.applicationInfo == null || HiddenApiBridge.PackageInfo_overlayTarget(pi) != null)
                    continue;

                int uid = pi.applicationInfo.uid;
                if (uid == managerUid)
                    continue;

                int flags = getFlagsForUid(pi.applicationInfo.uid, Config.MASK_PERMISSION);
                if (flags == 0
                        && pi.applicationInfo.uid != 2000
                        && UserHandleCompat.getAppId(pi.applicationInfo.uid) < 10000)
                    continue;

                AppInfo item = new AppInfo();
                item.packageInfo = pi;
                item.flags = flags;
                list.add(item);
            }
        }
        return new ParceledListSlice<>(list);
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        //LOGGER.d("transact: code=%d, calling uid=%d", code, Binder.getCallingUid());
        if (code == ShizukuApiConstants.BINDER_TRANSACTION_transact) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            transactRemote(data, reply, flags);
            return true;
        } else if (code == ServerConstants.BINDER_TRANSACTION_getApplications) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            int userId = data.readInt();
            ParceledListSlice<AppInfo> result = getApplications(userId);
            reply.writeNoException();
            if (result != null) {
                reply.writeInt(1);
                result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
            } else {
                reply.writeInt(0);
            }
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    // pre-v10 APIs is not supported by Sui
    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public int getUid() {
        return 0;
    }

    @Override
    public String getSELinuxContext() {
        return null;
    }

    @Override
    public int checkPermission(String permission) {
        return 0;
    }

    @Override
    public void exit() {

    }
}
