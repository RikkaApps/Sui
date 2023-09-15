/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

package rikka.sui.server;

import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_API_VERSION;
import static rikka.shizuku.ShizukuApiConstants.ATTACH_APPLICATION_PACKAGE_NAME;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_PERMISSION_GRANTED;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_PATCH_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_SECONTEXT;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_UID;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SERVER_VERSION;
import static rikka.shizuku.ShizukuApiConstants.BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoHidden;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.rikka.tools.refine.Refine;
import moe.shizuku.server.IShizukuApplication;
import rikka.hidden.compat.ActivityManagerApis;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.UserManagerApis;
import rikka.parcelablelist.ParcelableListSlice;
import rikka.rish.RishConfig;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.server.ClientRecord;
import rikka.shizuku.server.Service;
import rikka.sui.model.AppInfo;
import rikka.sui.server.bridge.BridgeServiceClient;
import rikka.sui.util.Logger;
import rikka.sui.util.MapUtil;
import rikka.sui.util.OsUtils;
import rikka.sui.util.UserHandleCompat;

@OptIn(markerClass = androidx.core.os.BuildCompat.PrereleaseSdkCheck.class)
public class SuiService extends Service<SuiUserServiceManager, SuiClientManager, SuiConfigManager> {

    private static SuiService instance;
    private static String filesPath;

    public static SuiService getInstance() {
        return instance;
    }

    public static void main(String filesPath) {
        LOGGER.i("starting server...");

        RishConfig.setLibraryPath(System.getProperty("sui.library.path"));

        SuiService.filesPath = filesPath;

        Looper.prepareMainLooper();
        new SuiService();
        Looper.loop();

        LOGGER.i("server exited");
        System.exit(0);
    }

    private static final String MANAGER_APPLICATION_ID = "com.android.systemui";
    private static final String SETTINGS_APPLICATION_ID = "com.android.settings";

    private final SuiClientManager clientManager;
    private final SuiConfigManager configManager;
    private final SuiUserServiceManager userServiceManager;
    private final int systemUiUid;
    private final int settingsUid;
    private IShizukuApplication systemUiApplication;

    private final Object managerBinderLock = new Object();
    private final Logger flog = new Logger("Sui", "/cache/sui.log");

    private int waitForPackage(String packageName, boolean forever) {
        int uid;
        while (true) {
            ApplicationInfo ai = PackageManagerApis.getApplicationInfoNoThrow(packageName, 0, 0);
            if (ai != null) {
                uid = ai.uid;
                break;
            }

            LOGGER.w("can't find %s, wait 1s", packageName);

            if (!forever) return -1;

            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        LOGGER.i("uid for %s is %d", packageName, uid);
        return uid;
    }

    public SuiService() {
        super();

        SuiService.instance = this;

        configManager = getConfigManager();
        clientManager = getClientManager();
        userServiceManager = getUserServiceManager();

        systemUiUid = waitForPackage(MANAGER_APPLICATION_ID, true);
        settingsUid = waitForPackage(SETTINGS_APPLICATION_ID, true);

        int gmsUid = waitForPackage("com.google.android.gms", false);
        if (gmsUid != 0) {
            configManager.update(gmsUid, SuiConfig.MASK_PERMISSION, SuiConfig.FLAG_HIDDEN);
        }

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

    @Override
    public SuiUserServiceManager onCreateUserServiceManager() {
        return new SuiUserServiceManager();
    }

    @Override
    public SuiClientManager onCreateClientManager() {
        return new SuiClientManager(getConfigManager());
    }

    @Override
    public SuiConfigManager onCreateConfigManager() {
        return new SuiConfigManager();
    }

    @Override
    public boolean checkCallerManagerPermission(String func, int callingUid, int callingPid) {
        return callingUid == settingsUid || callingUid == systemUiUid;
    }

    @Override
    public boolean checkCallerPermission(String func, int callingUid, int callingPid, @Nullable ClientRecord clientRecord) {
        // Temporary fix for https://github.com/RikkaApps/Sui/issues/35
        if ("transactRemote".equals(func)) {
            SuiConfig.PackageEntry packageEntry = configManager.find(callingUid);
            return packageEntry != null && packageEntry.isAllowed();
        }
        return false;
    }

    @Override
    public void attachApplication(IShizukuApplication application, Bundle args) {
        if (application == null || args == null) {
            return;
        }

        String requestPackageName = args.getString(ATTACH_APPLICATION_PACKAGE_NAME);
        if (requestPackageName == null) {
            return;
        }
        int apiVersion = args.getInt(ATTACH_APPLICATION_API_VERSION, -1);

        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        boolean isManager, isSettings;
        ClientRecord clientRecord = null;

        List<String> packages = PackageManagerApis.getPackagesForUidNoThrow(callingUid);
        if (!packages.contains(requestPackageName)) {
            throw new SecurityException("Request package " + requestPackageName + "does not belong to uid " + callingUid);
        }

        isManager = MANAGER_APPLICATION_ID.equals(requestPackageName);
        isSettings = SETTINGS_APPLICATION_ID.equals(requestPackageName);

        if (isManager) {
            IBinder binder = application.asBinder();
            try {
                binder.linkToDeath(new IBinder.DeathRecipient() {

                    @Override
                    public void binderDied() {
                        flog.w("manager binder is dead, pid=%d", callingPid);

                        synchronized (managerBinderLock) {
                            if (systemUiApplication.asBinder() == binder) {
                                systemUiApplication = null;
                            } else {
                                flog.w("binderDied is called later than the arrival of the new binder ?!");
                            }
                        }

                        binder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                LOGGER.w(e, "attachApplication");
            }

            synchronized (managerBinderLock) {
                systemUiApplication = application;
                flog.i("manager attached: pid=%d", callingPid);
            }
        }

        if (!isManager && !isSettings) {
            if (clientManager.findClient(callingUid, callingPid) != null) {
                throw new IllegalStateException("Client (uid=" + callingUid + ", pid=" + callingPid + ") has already attached");
            }
            synchronized (this) {
                clientRecord = clientManager.addClient(callingUid, callingPid, application, requestPackageName, apiVersion);
            }
            if (clientRecord == null) {
                return;
            }
        }

        int replyServerVersion = ShizukuApiConstants.SERVER_VERSION;
        if (!isManager && !isSettings && apiVersion == -1) {
            // ShizukuBinderWrapper has adapted API v13 in dev.rikka.shizuku:api 12.2.0, however
            // attachApplication in 12.2.0 is still old, so that server treat the client as pre 13.
            // This finally cause transactRemote fails.
            // So we can pass 12 here to pretend we are v12 server.
            replyServerVersion = 12;
        }

        Bundle reply = new Bundle();
        reply.putInt(BIND_APPLICATION_SERVER_UID, OsUtils.getUid());
        reply.putInt(BIND_APPLICATION_SERVER_VERSION, replyServerVersion);
        reply.putString(BIND_APPLICATION_SERVER_SECONTEXT, OsUtils.getSELinuxContext());
        reply.putInt(BIND_APPLICATION_SERVER_VERSION, replyServerVersion);
        reply.putInt(BIND_APPLICATION_SERVER_PATCH_VERSION, ShizukuApiConstants.SERVER_PATCH_VERSION);
        if (!isManager && !isSettings) {
            reply.putBoolean(BIND_APPLICATION_PERMISSION_GRANTED, clientRecord.allowed);
            reply.putBoolean(BIND_APPLICATION_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE, shouldShowRequestPermissionRationale(clientRecord));
        }
        try {
            application.bindApplication(reply);
        } catch (Throwable e) {
            LOGGER.w(e, "attachApplication");
        }
    }

    @Override
    public void showPermissionConfirmation(int requestCode, @NonNull ClientRecord clientRecord, int callingUid, int callingPid, int userId) {
        if (systemUiApplication != null) {
            try {
                systemUiApplication.showPermissionConfirmation(callingUid, callingPid, clientRecord.packageName, requestCode);
            } catch (Throwable e) {
                LOGGER.w(e, "showPermissionConfirmation");
            }
        } else {
            LOGGER.e("manager is null");
        }
    }

    private boolean shouldShowRequestPermissionRationale(ClientRecord record) {
        SuiConfig.PackageEntry entry = configManager.find(record.uid);
        return entry != null && entry.isDenied();
    }

    @Override
    public boolean isHidden(int uid) {
        if (Binder.getCallingUid() != 1000) {
            // only allow to be called by system server
            return false;
        }

        return uid != systemUiUid && uid != settingsUid && configManager.isHidden(uid);
    }

    @Override
    public void dispatchPermissionConfirmationResult(int requestUid, int requestPid, int requestCode, Bundle data) {
        if (Binder.getCallingUid() != systemUiUid) {
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

        List<ClientRecord> records = clientManager.findClients(requestUid);
        if (records.isEmpty()) {
            LOGGER.w("dispatchPermissionConfirmationResult: no client for uid %d was found", requestUid);
        } else {
            for (ClientRecord record : records) {
                record.allowed = allowed;
                if (record.pid == requestPid) {
                    record.dispatchRequestPermissionResult(requestCode, allowed);
                }
            }
        }

        if (!onetime) {
            configManager.update(requestUid, SuiConfig.MASK_PERMISSION, allowed ? SuiConfig.FLAG_ALLOWED : SuiConfig.FLAG_DENIED);
        }
    }

    private int getFlagsForUidInternal(int uid, int mask) {
        SuiConfig.PackageEntry entry = configManager.find(uid);
        if (entry != null) {
            return entry.flags & mask;
        }
        return 0;
    }

    @Override
    public int getFlagsForUid(int uid, int mask) {
        enforceManagerPermission("getFlagsForUid");
        return getFlagsForUidInternal(uid, mask);
    }

    @Override
    public void updateFlagsForUid(int uid, int mask, int value) {
        enforceManagerPermission("updateFlagsForUid");

        int oldValue = getFlagsForUidInternal(uid, mask);
        boolean wasHidden = (oldValue & SuiConfig.FLAG_HIDDEN) != 0;

        configManager.update(uid, mask, value);

        if ((mask & SuiConfig.MASK_PERMISSION) != 0) {
            boolean allowed = (value & SuiConfig.FLAG_ALLOWED) != 0;
            for (ClientRecord record : clientManager.findClients(uid)) {
                record.allowed = allowed;

                if (!allowed || wasHidden) {
                    ActivityManagerApis.forceStopPackageNoThrow(record.packageName, UserHandleCompat.getUserId(record.uid));
                    getUserServiceManager().removeUserServicesForPackage(record.packageName);
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
        } else if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action) && !replacing) {
            Uri uri = intent.getData();
            String packageName = (uri != null) ? uri.getSchemeSpecificPart() : null;
            if (packageName != null) {
                userServiceManager.removeUserServicesForPackage(packageName);
            }
        }
    }

    private ParcelableListSlice<AppInfo> getApplications(int userId) {
        enforceManagerPermission("getApplications");

        List<Integer> users = new ArrayList<>();
        if (userId == -1) {
            users.addAll(UserManagerApis.getUserIdsNoThrow());
        } else {
            users.add(userId);
        }

        Map<String, Boolean> existenceCache = new ArrayMap<>();
        Map<String, Boolean> hasComponentsCache = new ArrayMap<>();

        List<AppInfo> list = new ArrayList<>();
        for (int user : users) {
            for (PackageInfo pi : PackageManagerApis.getInstalledPackagesNoThrow(0x00002000 /*MATCH_UNINSTALLED_PACKAGES*/, user)) {
                if (pi.applicationInfo == null
                        || Refine.<PackageInfoHidden>unsafeCast(pi).overlayTarget != null
                        || (pi.applicationInfo.flags & ApplicationInfo.FLAG_HAS_CODE) == 0)
                    continue;

                int uid = pi.applicationInfo.uid;
                int appId = UserHandleCompat.getAppId(uid);
                if (uid == systemUiUid)
                    continue;

                int flags = getFlagsForUidInternal(uid, SuiConfig.MASK_PERMISSION);
                if (flags == 0 && uid != 2000 && appId < 10000)
                    continue;

                if (flags == 0) {
                    String dataDir;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        dataDir = pi.applicationInfo.deviceProtectedDataDir;
                    } else {
                        dataDir = pi.applicationInfo.dataDir;
                    }

                    boolean hasApk = MapUtil.getOrPut(existenceCache, pi.applicationInfo.sourceDir, () -> new File(pi.applicationInfo.sourceDir).exists());
                    boolean hasData = MapUtil.getOrPut(existenceCache, dataDir, () -> new File(dataDir).exists());

                    // Installed (or hidden): hasApk && hasData
                    // Uninstalled but keep data: !hasApk && hasData
                    // Installed in other users only: hasApk && !hasData
                    if (!(hasApk && hasData)) {
                        LOGGER.v("skip %d:%s: hasApk=%s, hasData=%s", user, pi.packageName, Boolean.toString(hasApk), Boolean.toString(hasData));
                        continue;
                    }

                    boolean hasComponents = MapUtil.getOrPut(hasComponentsCache, pi.packageName, () -> {
                        try {
                            int baseFlags = 0x00000200 /*MATCH_DISABLED_COMPONENTS*/ | 0x00002000 /*MATCH_UNINSTALLED_PACKAGES*/;
                            PackageInfo pi2 = PackageManagerApis.getPackageInfoNoThrow(pi.packageName,
                                    baseFlags | PackageManager.GET_ACTIVITIES | PackageManager.GET_RECEIVERS | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS,
                                    user);
                            if (pi2 == null) {
                                // Exceed binder data transfer limit
                                pi2 = pi;
                                pi2.activities = PackageManagerApis.getPackageInfoNoThrow(pi.packageName, baseFlags | PackageManager.GET_ACTIVITIES, user).activities;
                                pi2.receivers = PackageManagerApis.getPackageInfoNoThrow(pi.packageName, baseFlags | PackageManager.GET_RECEIVERS, user).receivers;
                                pi2.services = PackageManagerApis.getPackageInfoNoThrow(pi.packageName, baseFlags | PackageManager.GET_SERVICES, user).services;
                                pi2.providers = PackageManagerApis.getPackageInfoNoThrow(pi.packageName, baseFlags | PackageManager.GET_PROVIDERS, user).providers;
                            }
                            return pi2.activities != null && pi2.activities.length > 0
                                    || pi2.receivers != null && pi2.receivers.length > 0
                                    || pi2.services != null && pi2.services.length > 0
                                    || pi2.providers != null && pi2.providers.length > 0;
                        } catch (Throwable e) {
                            return true;
                        }
                    });

                    // Packages without components cannot run as themselves
                    if (!hasComponents) {
                        LOGGER.v("skip %d:%s: hasComponents=false", user, pi.packageName);
                        continue;
                    }
                }

                pi.activities = null;
                pi.receivers = null;
                pi.services = null;
                pi.providers = null;

                AppInfo item = new AppInfo();
                item.packageInfo = pi;
                item.flags = flags;
                list.add(item);
            }
        }
        return new ParcelableListSlice<>(list);
    }

    private void showManagement() {
        enforceManagerPermission("showManagement");

        if (systemUiApplication != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken(ShizukuApiConstants.BINDER_DESCRIPTOR);
            try {
                systemUiApplication.asBinder().transact(ServerConstants.BINDER_TRANSACTION_showManagement, data, null, IBinder.FLAG_ONEWAY);
            } catch (Throwable e) {
                LOGGER.w(e, "showPermissionConfirmation");
            } finally {
                data.recycle();
            }
        } else {
            LOGGER.e("manager is null");
        }
    }

    private ParcelFileDescriptor openApk() {
        if (!checkCallerManagerPermission("openApk", Binder.getCallingUid(), Binder.getCallingPid())) {
            LOGGER.w("openApk is allowed to be called only from settings and system ui");
            return null;
        }
        String pathname = filesPath + "/sui.apk";
        try {
            //noinspection OctalInteger
            Os.chmod(pathname, 0655);
        } catch (ErrnoException e) {
            LOGGER.e(e, "Cannot chmod %s", pathname);
        }

        try {
            return ParcelFileDescriptor.open(new File(pathname), ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        //LOGGER.d("transact: code=%d, calling uid=%d", code, Binder.getCallingUid());
        if (code == ServerConstants.BINDER_TRANSACTION_getApplications) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            int userId = data.readInt();
            ParcelableListSlice<AppInfo> result = getApplications(userId);
            reply.writeNoException();
            if (result != null) {
                reply.writeInt(1);
                result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
            } else {
                reply.writeInt(0);
            }
            return true;
        } else if (code == ServerConstants.BINDER_TRANSACTION_showManagement) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            showManagement();
            return true;
        } else if (code == ServerConstants.BINDER_TRANSACTION_openApk) {
            data.enforceInterface(ShizukuApiConstants.BINDER_DESCRIPTOR);
            ParcelFileDescriptor result = openApk();
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

    @Override
    public void exit() {

    }
}
