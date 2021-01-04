package app.rikka.sui.server;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import app.rikka.sui.server.config.Config;
import moe.shizuku.server.IShizukuClient;

import static app.rikka.sui.server.ServerConstants.LOGGER;

public class ClientManager {

    private static ClientManager instance;

    public static ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    private static final long WRITE_DELAY = 10 * 1000;

    private final Config config;
    private final HandlerThread handlerThread = new HandlerThread("worker");
    private final Handler handler = new Handler(handlerThread.getLooper());

    private final Runnable mWriteRunner = new Runnable() {

        @Override
        public void run() {
            Config.write(config);
        }
    };

    private final List<ClientRecord> clientRecords = Collections.synchronizedList(new ArrayList<>());

    private ClientManager() {
        this.config = Config.load();
    }

    private void scheduleWriteConfig() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(mWriteRunner)) {
                return;
            }
        } else {
            handler.removeCallbacks(mWriteRunner);
        }
        handler.postDelayed(mWriteRunner, WRITE_DELAY);
    }

    public boolean isHidden(int uid) {
        Config.PackageEntry entry = config.find(uid);
        if (entry == null) {
            return false;
        }
        return (entry.flags & Config.FLAG_HIDDEN) != 0;
    }

    public ClientRecord findClient(int uid, int pid) {
        for (ClientRecord clientRecord : clientRecords) {
            if (clientRecord.pid == pid && clientRecord.uid == uid) {
                return clientRecord;
            }
        }
        return null;
    }

    public void addClient(int uid, int pid, IShizukuClient client, String packageName) {
        ClientRecord clientRecord = new ClientRecord(uid, pid, client, packageName);

        Config.PackageEntry entry = config.find(uid);
        if (entry != null && (entry.flags & Config.FLAG_GRANTED) != 0) {
            clientRecord.permissionGranted = true;
        }

        IBinder binder = client.asBinder();
        try {
            binder.linkToDeath((IBinder.DeathRecipient) () -> clientRecords.remove(clientRecord), 0);
        } catch (RemoteException e) {
            LOGGER.w(e, "addClient: linkToDeath failed");
            return;
        }

        clientRecords.add(clientRecord);
    }

    public void setPermissionForClient() {

    }
}


