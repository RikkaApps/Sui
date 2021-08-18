package rikka.sui.binder;

import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.io.FileDescriptor;

public class IBinderWrapper implements IBinder {

    private final IBinder mOriginal;

    public IBinderWrapper(IBinder original) {
        this.mOriginal = original;
    }

    public IBinder getOriginal() {
        return mOriginal;
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
        return mOriginal.transact(code, data, reply, flags);
    }

    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return mOriginal.getInterfaceDescriptor();
    }

    @Override
    public boolean pingBinder() {
        return mOriginal.pingBinder();
    }

    @Override
    public boolean isBinderAlive() {
        return mOriginal.isBinderAlive();
    }

    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return mOriginal.queryLocalInterface(descriptor);
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, String[] args) throws RemoteException {
        mOriginal.dump(fd, args);
    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, String[] args) throws RemoteException {
        mOriginal.dumpAsync(fd, args);
    }

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
        mOriginal.linkToDeath(recipient, flags);
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return mOriginal.unlinkToDeath(recipient, flags);
    }
}
