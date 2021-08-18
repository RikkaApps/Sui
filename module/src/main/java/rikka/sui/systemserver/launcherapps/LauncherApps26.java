package rikka.sui.systemserver.launcherapps;

import android.annotation.TargetApi;
import android.content.pm.ILauncherApps;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.RemoteException;

import java.util.Set;

import rikka.sui.binder.HookedBinderProxy;
import rikka.sui.binder.Transaction;
import rikka.sui.binder.TransactionCodeExporter;

@TargetApi(26)
class LauncherApps26 extends ILauncherApps.Stub implements HookedBinderProxy<ILauncherApps> {

    private static final Set<Integer> CODES = TransactionCodeExporter.exportAll(ILauncherApps.Stub.class, LauncherApps26.class);

    private final ILauncherApps mOriginal;

    public LauncherApps26(ILauncherApps original) {
        mOriginal = original;
    }

    @Override
    public ILauncherApps getOriginal() {
        return mOriginal;
    }

    @Override
    public boolean isTransactionReplaced(int code) {
        return CODES.contains(code);
    }

    @Transaction
    @Override
    public boolean startShortcut(String callingPackage, String packageName, String id, Rect sourceBounds, Bundle startActivityOptions, int userId) throws RemoteException {
        LauncherAppsFuncs.StartShortcut func = new LauncherAppsFuncs.StartShortcut(getOriginal(), callingPackage, packageName, null, id, sourceBounds, startActivityOptions, userId);
        LauncherAppsWrapper.startShortcut(func);
        return func.result;
    }
}
