package rikka.sui.server;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.ArrayMap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rikka.sui.server.api.RemoteProcessHolder;
import rikka.sui.server.api.SystemService;
import rikka.sui.server.bridge.BridgeServiceClient;
import rikka.sui.server.config.Config;
import rikka.sui.server.config.ConfigManager;
import rikka.sui.server.userservice.ApkChangedObserver;
import rikka.sui.server.userservice.ApkChangedObservers;
import rikka.sui.util.OsUtils;
import rikka.sui.util.UserHandleCompat;
import dalvik.system.PathClassLoader;
import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuClient;
import moe.shizuku.server.IShizukuManager;
import moe.shizuku.server.IShizukuService;
import moe.shizuku.server.IShizukuServiceConnection;

import static rikka.sui.server.ServerConstants.LOGGER;
import static rikka.sui.server.ShizukuApiConstants.ATTACH_REPLY_SERVER_PERMISSION_GRANTED;
import static rikka.sui.server.ShizukuApiConstants.ATTACH_REPLY_SERVER_SECONTEXT;
import static rikka.sui.server.ShizukuApiConstants.ATTACH_REPLY_SERVER_UID;
import static rikka.sui.server.ShizukuApiConstants.ATTACH_REPLY_SERVER_VERSION;
import static rikka.sui.server.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.sui.server.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;
import static rikka.sui.server.ShizukuApiConstants.USER_SERVICE_ARG_COMPONENT;
import static rikka.sui.server.ShizukuApiConstants.USER_SERVICE_ARG_DEBUGGABLE;
import static rikka.sui.server.ShizukuApiConstants.USER_SERVICE_ARG_PROCESS_NAME;
import static rikka.sui.server.ShizukuApiConstants.USER_SERVICE_ARG_TAG;
import static rikka.sui.server.ShizukuApiConstants.USER_SERVICE_ARG_VERSION_CODE;
import static rikka.sui.server.ShizukuApiConstants.USER_SERVICE_TRANSACTION_destroy;

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

    private static final String USER_SERVICE_CMD_DEBUG;

    static {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 30) {
            USER_SERVICE_CMD_DEBUG = "-Xcompiler-option" + " --debuggable" +
                    " -XjdwpProvider:adbconnection" +
                    " -XjdwpOptions:suspend=n,server=y";
        } else if (sdk >= 28) {
            USER_SERVICE_CMD_DEBUG = "-Xcompiler-option" + " --debuggable" +
                    " -XjdwpProvider:internal" +
                    " -XjdwpOptions:transport=dt_android_adb,suspend=n,server=y";
        } else {
            USER_SERVICE_CMD_DEBUG = "-Xcompiler-option" + " --debuggable" +
                    " -agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y";
        }
    }

    private static final String MANAGER_APPLICATION_ID = "com.android.systemui";

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Map<String, UserServiceRecord> userServiceRecords = Collections.synchronizedMap(new ArrayMap<>());
    private final ClientManager clientManager;
    private final ConfigManager configManager;
    private IShizukuManager manager;

    private final IBinder.DeathRecipient managerDeathRecipient = () -> {
        LOGGER.w("manager binder is dead");
        manager = null;
    };

    public Service() {
        Service.instance = this;

        configManager = ConfigManager.getInstance();
        clientManager = ClientManager.getInstance();

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

        if (callingUid == OsUtils.getUid() || callingPid == OsUtils.getPid()) {
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
        enforceCallingPermission("newProcess");

        LOGGER.d("newProcess: uid=%d, cmd=%s, env=%s, dir=%s", Binder.getCallingUid(), Arrays.toString(cmd), Arrays.toString(env), dir);

        Process process;
        try {
            process = Runtime.getRuntime().exec(cmd, env, dir != null ? new File(dir) : null);
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        }

        return new RemoteProcessHolder(process);
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

    private class UserServiceRecord {

        private final DeathRecipient deathRecipient;
        public final boolean standalone;
        public final int versionCode;
        public String token;
        public IBinder service;
        public final ApkChangedObserver apkChangedObserver;
        public final RemoteCallbackList<IShizukuServiceConnection> callbacks = new RemoteCallbackList<>();

        public UserServiceRecord(boolean standalone, int versionCode, String apkPath) {
            this.standalone = standalone;
            this.versionCode = versionCode;
            this.token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
            this.apkChangedObserver = ApkChangedObservers.start(apkPath, () -> {
                LOGGER.v("remove record %s because apk changed", token);
                removeSelf();
            });
            this.deathRecipient = () -> {
                LOGGER.v("binder in service record %s is dead", token);
                removeSelf();
            };
        }

        public void setBinder(IBinder binder) {
            LOGGER.v("binder received for service record %s", token);

            service = binder;

            if (standalone) {
                try {
                    binder.linkToDeath(deathRecipient, 0);
                } catch (Throwable tr) {
                    LOGGER.w(tr, "linkToDeath " + token);
                }
            }

            broadcastBinderReceived();
        }

        public void broadcastBinderReceived() {
            LOGGER.v("broadcast received for service record %s", token);

            int count = callbacks.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    callbacks.getBroadcastItem(i).connected(service);
                } catch (Throwable e) {
                    LOGGER.w("failed to call connected");
                }
            }
            callbacks.finishBroadcast();
        }

        public void broadcastBinderDead() {
            LOGGER.v("broadcast dead for service record %s", token);

            int count = callbacks.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    callbacks.getBroadcastItem(i).dead();
                } catch (Throwable e) {
                    LOGGER.w("failed to call connected");
                }
            }
            callbacks.finishBroadcast();
        }

        private void removeSelf() {
            synchronized (Service.this) {
                removeUserServiceLocked(UserServiceRecord.this);
            }
        }

        public void destroy() {
            if (standalone) {
                unlinkToDeath(deathRecipient, 0);
            } else {
                broadcastBinderDead();
            }

            ApkChangedObservers.stop(apkChangedObserver);

            if (service != null && service.pingBinder()) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(service.getInterfaceDescriptor());
                    service.transact(USER_SERVICE_TRANSACTION_destroy, data, reply, Binder.FLAG_ONEWAY);
                } catch (Throwable e) {
                    LOGGER.w(e, "failed to destroy");
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }

            callbacks.kill();
        }
    }

    private PackageInfo ensureCallingPackageForUserService(String packageName, int appId, int userId) {
        PackageInfo packageInfo = SystemService.getPackageInfoNoThrow(packageName, 0x00002000 /*PackageManager.MATCH_UNINSTALLED_PACKAGES*/, userId);
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
        boolean standalone = processNameSuffix != null;
        String key = packageName + ":" + (tag != null ? tag : className);

        synchronized (this) {
            UserServiceRecord record = getOrCreateUserServiceRecordLocked(key, versionCode, standalone, sourceDir);
            record.callbacks.register(conn);

            if (record.service != null && record.service.pingBinder()) {
                record.broadcastBinderReceived();
            } else {
                Runnable runnable;
                if (standalone) {
                    runnable = () -> startUserServiceNewProcess(key, record.token, packageName, className, processNameSuffix, uid, debug);
                } else {
                    runnable = () -> {
                        /*CancellationSignal cancellationSignal = new CancellationSignal();
                        cancellationSignal.setOnCancelListener(() -> {
                            synchronized (ShizukuService.this) {
                                UserServiceRecord r = getUserServiceRecordLocked(key);
                                if (r != null) {
                                    removeUserServiceLocked(r);
                                    LOGGER.v("remove %s by user", key);
                                }
                            }
                        });

                        startUserServiceLocalProcess(key, record.token, packageName, className, sourceDir, cancellationSignal);*/
                    };
                }
                executor.execute(runnable);
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
            } else if (record.standalone != standalone) {
                LOGGER.v("remove service record %s (%s) because standalone not matched (old=%s, new=%s)", key, record.token, Boolean.toString(record.standalone), Boolean.toString(standalone));
            } else if (record.service == null || !record.service.pingBinder()) {
                LOGGER.v("service in record %s (%s) has dead", key, record.token);
            } else {
                LOGGER.i("found existing service record %s (%s)", key, record.token);
                return record;
            }

            removeUserServiceLocked(record);
        }

        record = new UserServiceRecord(standalone, versionCode, apkPath);
        userServiceRecords.put(key, record);
        LOGGER.i("new service record %s (%s): version=%d, standalone=%s, apk=%s", key, record.token, versionCode, Boolean.toString(standalone), apkPath);
        return record;
    }

    private void startUserServiceLocalProcess(String key, String token, String packageName, String className, String sourceDir, CancellationSignal cancellationSignal) {
        UserServiceRecord record = userServiceRecords.get(key);
        if (record == null || !Objects.equals(token, record.token)) {
            LOGGER.w("unable to find service record %s (%s)", key, token);
            return;
        }

        IBinder service;
        try {
            ClassLoader classLoader = new PathClassLoader(sourceDir, null, ClassLoader.getSystemClassLoader());
            Class<?> serviceClass = Objects.requireNonNull(classLoader.loadClass(className));
            Constructor<?> constructor;

            try {
                constructor = serviceClass.getConstructor(CancellationSignal.class);
                service = (IBinder) constructor.newInstance(cancellationSignal);
            } catch (Throwable e) {
                LOGGER.w("constructor with CancellationSignal not found");
                constructor = serviceClass.getConstructor();
                service = (IBinder) constructor.newInstance();
            }
        } catch (Throwable tr) {
            LOGGER.w(tr, "unable to create service %s/%s", packageName, className);
            return;
        }

        record.setBinder(service);
    }

    private static final String USER_SERVICE_CMD_FORMAT = "(CLASSPATH=/data/local/tmp/shizuku/starter-v%d.dex /system/bin/app_process%s /system/bin " +
            "--nice-name=%s %s " +
            "--token=%s --package=%s --class=%s --uid=%d%s)&";

    private void startUserServiceNewProcess(String key, String token, String packageName, String classname, String processNameSuffix, int callingUid, boolean debug) {
        LOGGER.v("starting process for service record %s (%s)...", key, token);

        String processName = String.format("%s:%s", packageName, processNameSuffix);
        String cmd = String.format(Locale.ENGLISH, USER_SERVICE_CMD_FORMAT,
                ShizukuApiConstants.SERVER_VERSION, debug ? (" " + USER_SERVICE_CMD_DEBUG) : "",
                processName, "moe.shizuku.starter.ServiceStarter",
                token, packageName, classname, callingUid, debug ? (" " + "--debug-name=" + processName) : "");

        Process process;
        int exitCode;
        try {
            process = Runtime.getRuntime().exec("sh");
            OutputStream os = process.getOutputStream();
            os.write(cmd.getBytes());
            os.flush();
            os.close();

            exitCode = process.waitFor();
        } catch (Throwable e) {
            throw new IllegalStateException(e.getMessage());
        }

        if (exitCode != 0) {
            throw new IllegalStateException("sh exited with " + exitCode);
        }
    }

    @Override
    public void sendUserService(IBinder binder, Bundle options) {
        Objects.requireNonNull(binder, "binder is null");
        String token = Objects.requireNonNull(options.getString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN), "token is null");

        synchronized (this) {
            sendUserServiceLocked(binder, token);
        }
    }

    @Override
    public void attachClient(IShizukuClient client, String requestPackageName) {
        if (client == null || requestPackageName == null) {
            return;
        }

        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();

        List<String> packages = SystemService.getPackagesForUidNoThrow(callingUid);
        if (!packages.contains(requestPackageName)) {
            throw new SecurityException("Request package " + requestPackageName + "does not belong to uid " + callingUid);
        }

        if (clientManager.findClient(callingUid, callingPid) != null) {
            throw new IllegalStateException("Client (uid=" + callingUid + ", pid=" + callingPid + ") has already attached");
        }

        ClientRecord clientRecord;
        synchronized (this) {
            clientRecord = clientManager.addClient(callingUid, callingPid, client, requestPackageName);
        }
        if (clientRecord == null) {
            return;
        }

        Bundle reply = new Bundle();
        reply.putInt(ATTACH_REPLY_SERVER_UID, OsUtils.getUid());
        reply.putInt(ATTACH_REPLY_SERVER_VERSION, ShizukuApiConstants.SERVER_VERSION);
        reply.putString(ATTACH_REPLY_SERVER_SECONTEXT, OsUtils.getSELinuxContext());
        reply.putBoolean(ATTACH_REPLY_SERVER_PERMISSION_GRANTED, clientRecord.allowed);
        try {
            client.onClientAttached(reply);
        } catch (Throwable e) {
            LOGGER.w(e, "onClientAttached");
        }
    }

    @Override
    public void requestPermission(int requestCode) {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();

        if (callingUid == OsUtils.getUid() || callingPid == OsUtils.getPid()) {
            return;
        }

        ClientRecord clientRecord = clientManager.findClient(callingUid, callingPid);
        if (clientRecord == null) {
            throw new IllegalStateException("Not an attached client");
        }

        if (clientRecord.allowed) {
            clientRecord.callOnRequestPermissionResult(requestCode, true);
            return;
        }

        Config.PackageEntry entry = configManager.find(callingUid);
        if (entry != null && entry.isDenied()) {
            clientRecord.callOnRequestPermissionResult(requestCode, false);
            return;
        }

        if (manager != null) {
            try {
                manager.showPermissionConfirmation(callingUid, callingPid, clientRecord.packageName, requestCode);
            } catch (RemoteException e) {
                LOGGER.w(e, "showPermissionConfirmation");
            }
        } else {
            LOGGER.e("manager is null");
        }
    }

    @Override
    public boolean isHidden(int uid) {
        if (Binder.getCallingUid() != 1000) {
            // only allow to be called by system server
            return false;
        }

        List<String> packages = SystemService.getPackagesForUidNoThrow(uid);
        if (packages.contains(MANAGER_APPLICATION_ID)) {
            return false;
        }

        return configManager.isHidden(uid);
    }

    @Override
    public void attachManager(IShizukuManager manager) {
        int callingUid = Binder.getCallingUid();

        List<String> packages = SystemService.getPackagesForUidNoThrow(callingUid);
        if (!packages.contains(MANAGER_APPLICATION_ID)) {
            LOGGER.w("attachManager called not from manager package");
            return;
        }

        if (manager == null) {
            return;
        }

        try {
            manager.asBinder().linkToDeath(managerDeathRecipient, 0);
        } catch (RemoteException e) {
            LOGGER.w(e, "attachManager");
        }
        this.manager = manager;
    }

    @Override
    public void onPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, Bundle data) {
        int callingUid = Binder.getCallingUid();

        List<String> packages = SystemService.getPackagesForUidNoThrow(callingUid);
        if (!packages.contains(MANAGER_APPLICATION_ID)) {
            LOGGER.w("onPermissionConfirmationResult called not from manager package");
            return;
        }

        if (data == null) {
            return;
        }

        boolean allowed = data.getBoolean(REQUEST_PERMISSION_REPLY_ALLOWED);
        boolean onetime = data.getBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME);

        LOGGER.i("onPermissionConfirmationResult: uid=%d, pid=%d, requestCode=%d, allowed=%s, onetime=%s",
                requestUid, requestPid, requestCode, Boolean.toString(allowed), Boolean.toString(onetime));

        ClientRecord clientRecord = clientManager.findClient(requestUid, requestPid);
        if (clientRecord == null) {
            LOGGER.w("onPermissionConfirmationResult: client (uid=%d, pid=%d) not found", requestUid, requestPid);
        } else {
            clientRecord.allowed = allowed;
            clientRecord.callOnRequestPermissionResult(requestCode, allowed);
        }

        if (!onetime) {
            configManager.update(requestUid, allowed ? Config.FLAG_ALLOWED : Config.FLAG_DENIED);
        }
    }

    private void sendUserServiceLocked(IBinder binder, String token) {
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
        record.setBinder(binder);
    }

    @Override
    public void onPackageChanged(Intent intent) {
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

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        //LOGGER.d("transact: code=%d, calling uid=%d", code, Binder.getCallingUid());
        if (code == ShizukuApiConstants.BINDER_TRANSACTION_transact) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            transactRemote(data, reply, flags);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }
}
