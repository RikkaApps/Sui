package app.rikka.sui.server.config;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.Nullable;

public class ConfigManager {

    private static ConfigManager instance;

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private static final long WRITE_DELAY = 10 * 1000;

    private final HandlerThread handlerThread = new HandlerThread("worker");
    private final Handler handler = new Handler(handlerThread.getLooper());

    private final Runnable mWriteRunner = new Runnable() {

        @Override
        public void run() {
            Config.write(config);
        }
    };

    private final Config config;

    private ConfigManager() {
        this.config = Config.load();
    }

    private void scheduleWriteLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(mWriteRunner)) {
                return;
            }
        } else {
            handler.removeCallbacks(mWriteRunner);
        }
        handler.postDelayed(mWriteRunner, WRITE_DELAY);
    }

    private Config.PackageEntry findLocked(int uid) {
        for (Config.PackageEntry entry : config.packages) {
            if (uid == entry.uid) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public Config.PackageEntry find(int uid) {
        synchronized (this) {
            return findLocked(uid);
        }
    }

    public void update(int uid, int flags) {
        synchronized (this) {
            Config.PackageEntry entry = findLocked(uid);
            if (entry == null) {
                entry = new Config.PackageEntry(uid, flags);
                config.packages.add(entry);
            } else {
                entry.flags = flags;
            }
            scheduleWriteLocked();
        }
    }

    public void remove(int uid) {
        synchronized (this) {
            Config.PackageEntry entry = findLocked(uid);
            if (entry == null) {
                return;
            }
            config.packages.remove(entry);
        }
        scheduleWriteLocked();
        return;
    }

    public boolean isHidden(int uid) {
        Config.PackageEntry entry = find(uid);
        if (entry == null) {
            return false;
        }
        return (entry.flags & Config.FLAG_HIDDEN) != 0;
    }
}