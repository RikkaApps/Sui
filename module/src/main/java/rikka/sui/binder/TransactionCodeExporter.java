package rikka.sui.binder;

import static rikka.sui.systemserver.SystemServerConstants.LOGGER;

import android.content.AttributionSource;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import rikka.sui.util.BuildUtils;

public class TransactionCodeExporter {

    private final Object mProxy;
    private final ExporterBinder mExporterBinder;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Set<Integer> exportAll(String name, Function<IBinder, Object> proxyCreator, Class<?> methodsClass) {
        try {
            TransactionCodeExporter exporter = new TransactionCodeExporter(proxyCreator);
            return exporter.exportAll(methodsClass, name);
        } catch (Throwable e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public static Set<Integer> exportAll(Class<?> stubClass, Class<?> methodsClass) {
        try {
            TransactionCodeExporter exporter = new TransactionCodeExporter(stubClass);
            return exporter.exportAll(methodsClass, stubClass.getName());
        } catch (Throwable e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public TransactionCodeExporter(Class<?> cls) throws ReflectiveOperationException {
        Method asInterfaceMethod = cls.getDeclaredMethod("asInterface", IBinder.class);
        mExporterBinder = new ExporterBinder();
        mProxy = asInterfaceMethod.invoke(null, mExporterBinder);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public TransactionCodeExporter(Function<IBinder, Object> proxyCreator) {
        mExporterBinder = new ExporterBinder();
        mProxy = proxyCreator.apply(mExporterBinder);
    }

    public Set<Integer> exportAll(Class<?> methodsClass, String name) {
        Set<Integer> codes = new HashSet<>();
        for (Method method : methodsClass.getDeclaredMethods()) {
            if (method.getAnnotation(Transaction.class) != null) {
                int transactCode = export(method.getName(), method.getReturnType(), method.getParameterTypes());
                if (transactCode != -1) {
                    codes.add(transactCode);
                    LOGGER.v("transact code for %s#%s is %d", name, method.getName(), transactCode);
                } else {
                    LOGGER.w("transact code for %s#%s not found", name, method.getName());
                }
            }
        }
        return codes;
    }

    public int export(String name, Class<?> returnType, Class<?>... argTypes) {
        Method m;
        try {
            m = mProxy.getClass().getDeclaredMethod(name, argTypes);
            if (!Objects.equals(returnType, m.getReturnType())) {
                return -1;
            }
        } catch (NoSuchMethodException e) {
            LOGGER.w(e, "export %s", name);
            return -1;
        }
        return export(m);
    }

    private int export(Method method) {
        mExporterBinder.clearLastTransactCode();
        Object[] args = buildArgs(method);
        try {
            method.invoke(mProxy, args);
        } catch (IllegalAccessException e) {
            LOGGER.w(e, "export %s", method);
        } catch (InvocationTargetException e) {
            LOGGER.w(e.getCause(), "export %s", method);
        }
        return mExporterBinder.getLastTransactCode();
    }

    private static Object[] buildArgs(Method method) {
        ArrayList<Object> result = new ArrayList<>();
        for (Class<?> c : method.getParameterTypes()) {
            if (c == int.class) {
                result.add(0);
            } else if (c == long.class) {
                result.add(0L);
            } else if (c == float.class) {
                result.add(0f);
            } else if (c == double.class) {
                result.add(0d);
            } else if (c == char.class) {
                result.add((char) 0);
            } else if (c == byte.class) {
                result.add((byte) 0);
            } else if (c == Uri.class) {
                // ContentProviderProxy does not accept null Uri
                result.add(Uri.fromParts("example", "example", null));
            } else if (c == ContentValues.class) {
                // ContentProviderProxy does not accept null ContentValues
                result.add(new ContentValues());
            } else if (c == ArrayList.class) {
                result.add(new ArrayList<>());
            } else if (BuildUtils.atLeast31() && c == AttributionSource.class) {
                result.add(new AttributionSource.Builder(0).setPackageName("example").setAttributionTag("example").build());
            } else {
                result.add(null);
            }
        }
        return result.toArray();
    }

    private static class ExporterBinder implements IBinder {

        private int lastTransactCode;

        private int getLastTransactCode() {
            return lastTransactCode;
        }

        private void clearLastTransactCode() {
            this.lastTransactCode = -1;
        }

        @Override
        public String getInterfaceDescriptor() {
            return "";
        }

        @Override
        public boolean pingBinder() {
            return false;
        }

        @Override
        public boolean isBinderAlive() {
            return false;
        }

        @Override
        public IInterface queryLocalInterface(String descriptor) {
            return null;
        }

        @Override
        public void dump(FileDescriptor fileDescriptor, String[] strings) throws RemoteException {

        }

        @Override
        public void dumpAsync(FileDescriptor fileDescriptor, String[] strings) throws RemoteException {

        }

        @Override
        public boolean transact(int code, Parcel data, Parcel reply, int flags) {
            lastTransactCode = code;
            return true;
        }

        @Override
        public void linkToDeath(DeathRecipient deathRecipient, int i) throws RemoteException {

        }

        @Override
        public boolean unlinkToDeath(DeathRecipient deathRecipient, int i) {
            return false;
        }
    }
}
