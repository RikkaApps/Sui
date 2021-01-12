package rikka.sui.server.userservice;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.Context;
import android.ddm.DdmHandleAppName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import hidden.HiddenApiBridge;
import moe.shizuku.server.IShizukuService;

import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_TOKEN;

@SuppressWarnings("JavaReflectionMemberAccess")
@SuppressLint("DiscouragedPrivateApi")
public class Starter {

    private static final String TAG = "SuiUserServiceStarter";

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String name = null;
        String token = null;
        String pkg = null;
        String cls = null;
        int uid = -1;

        for (String arg : args) {
            if (arg.startsWith("--debug-name=")) {
                name = arg.substring(13);
            } else if (arg.startsWith("--token=")) {
                token = arg.substring(8);
            } else if (arg.startsWith("--package=")) {
                pkg = arg.substring(10);
            } else if (arg.startsWith("--class=")) {
                cls = arg.substring(8);
            } else if (arg.startsWith("--uid=")) {
                uid = Integer.parseInt(arg.substring(6));
            }
        }

        int appId = uid % 100000;
        int userId = uid / 100000;

        Log.i(TAG, String.format("starting service %s/%s...", pkg, cls));

        Looper.prepare();

        IBinder service = null;
        ActivityThread activityThread = HiddenApiBridge.ActivityThread_systemMain();
        Method getSystemContext = ActivityThread.class.getDeclaredMethod("getSystemContext");
        Context systemContext = (Context) getSystemContext.invoke(activityThread);

        DdmHandleAppName.setAppName(name != null ? name : "shizuku_user_service", 0);

        Method createPackageContextAsUser = Context.class.getDeclaredMethod("createPackageContextAsUser", String.class, int.class, UserHandle.class);

        try {
            UserHandle userHandle = HiddenApiBridge.createUserHandle(userId);
            Context context = (Context) createPackageContextAsUser.invoke(systemContext, pkg, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY, userHandle);
            ClassLoader classLoader = context.getClassLoader();
            Class<?> serviceClass = classLoader.loadClass(cls);
            service = (IBinder) serviceClass.newInstance();
        } catch (Throwable tr) {
            Log.w(TAG, String.format("unable to start service %s/%s...", pkg, cls), tr);
            System.exit(1);
        }

        if (!sendBinder(service, token)) {
            System.exit(1);
        }

        Looper.loop();
        System.exit(0);

        Log.i(TAG, String.format("service %s/%s exited", pkg, cls));
    }

    private static final int BRIDGE_TRANSACTION_CODE = ('_' << 24) | ('S' << 16) | ('U' << 8) | 'I';
    private static final String BRIDGE_SERVICE_DESCRIPTOR = "android.app.IActivityManager";
    private static final String BRIDGE_SERVICE_NAME = "activity";
    private static final int BRIDGE_ACTION_GET_BINDER = 2;

    private static IBinder requestBinderFromBridge() {
        IBinder binder = ServiceManager.getService(BRIDGE_SERVICE_NAME);
        if (binder == null) return null;

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(BRIDGE_SERVICE_DESCRIPTOR);
            data.writeInt(BRIDGE_ACTION_GET_BINDER);
            binder.transact(BRIDGE_TRANSACTION_CODE, data, reply, 0);
            reply.readException();
            IBinder received = reply.readStrongBinder();
            if (received != null) {
                return received;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            data.recycle();
            reply.recycle();
        }
        return null;
    }

    private static boolean sendBinder(IBinder binder, String token) {
        IShizukuService shizukuService = IShizukuService.Stub.asInterface(requestBinderFromBridge());
        if (shizukuService == null) {
            return false;
        }

        Bundle data = new Bundle();
        data.putString(USER_SERVICE_ARG_TOKEN, token);
        try {
            shizukuService.attachUserService(binder, data);
        } catch (Throwable e) {
            Log.w(TAG, Log.getStackTraceString(e));
            return false;
        }
        return true;
    }
}
