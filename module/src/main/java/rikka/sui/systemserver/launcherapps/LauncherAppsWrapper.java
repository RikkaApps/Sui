package rikka.sui.systemserver.launcherapps;

import static rikka.sui.systemserver.SystemServerConstants.LOGGER;

import android.content.pm.ILauncherApps;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import rikka.sui.binder.HookedBinderProxy;
import rikka.sui.binder.IBinderWrapper;
import rikka.sui.server.ServerConstants;
import rikka.sui.shortcut.ShortcutConstants;
import rikka.sui.systemserver.BridgeService;

public class LauncherAppsWrapper extends IBinderWrapper {

    private static HookedBinderProxy<?> create(IBinder binder) {
        if (Build.VERSION.SDK_INT < 26) {
            return null;
        }

        ILauncherApps launcherApps = ILauncherApps.Stub.asInterface(binder);
        if (launcherApps == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            return new LauncherApps30(launcherApps);
        } else {
            return new LauncherApps26(launcherApps);
        }
    }

    private final HookedBinderProxy<?> proxyBinder;

    public LauncherAppsWrapper(IBinder original) {
        super(original);
        proxyBinder = create(original);
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
        if (proxyBinder == null || !proxyBinder.isTransactionReplaced(code)) {
            return false;
        }

        proxyBinder.transact(code, data, reply, flags);
        return true;
    }

    protected static void startShortcut(LauncherAppsFuncs.StartShortcut func) throws RemoteException {
        if (!"com.android.settings".equals(func.packageName)
                || !ShortcutConstants.SHORTCUT_ID.equals(func.id)) {
            func.call();
            return;
        }

        func.consume(true);

        if (BridgeService.get() != null) {
            Parcel data = Parcel.obtain();
            try {
                data.writeInterfaceToken("moe.shizuku.server.IShizukuService");
                try {
                    BridgeService.get().asBinder().transact(ServerConstants.BINDER_TRANSACTION_showManagement, data, null, IBinder.FLAG_ONEWAY);
                } catch (Throwable e) {
                    LOGGER.w(e, "showManagement");
                }
            } finally {
                data.recycle();
            }
        }
    }
}
