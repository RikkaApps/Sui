package rikka.sui.binder;

import android.os.IBinder;

public interface HookedBinderProxy<T> extends IBinder {

    T getOriginal();

    boolean isTransactionReplaced(int code);
}
