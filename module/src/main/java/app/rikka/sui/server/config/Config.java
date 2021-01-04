package app.rikka.sui.server.config;

import android.util.AtomicFile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static app.rikka.sui.server.ServerConstants.LOGGER;

public class Config {

    private static final Gson GSON_IN = new GsonBuilder()
            .create();
    private static final Gson GSON_OUT = new GsonBuilder()
            .setVersion(Config.LATEST_VERSION)
            .create();

    public static final int LATEST_VERSION = 1;

    private static final File FILE = new File("/data/adb/sui/sui.xml");
    private static final AtomicFile ATOMIC_FILE = new AtomicFile(FILE);

    public static Config load() {
        FileInputStream stream;
        try {
            stream = ATOMIC_FILE.openRead();
        } catch (FileNotFoundException e) {
            LOGGER.i("no existing config file " + ATOMIC_FILE.getBaseFile() + "; starting empty");
            return new Config();
        }

        Config config = null;
        try {
            config = GSON_IN.fromJson(new InputStreamReader(stream), Config.class);
        } catch (Throwable tr) {
            LOGGER.w(tr, "load config");
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                LOGGER.w("failed to close: " + e);
            }
        }
        return config;
    }

    public static void write(Config config) {
        synchronized (ATOMIC_FILE) {
            FileOutputStream stream;
            try {
                stream = ATOMIC_FILE.startWrite();
            } catch (IOException e) {
                LOGGER.w("failed to write state: " + e);
                return;
            }

            try {
                String json = GSON_OUT.toJson(config);
                stream.write(json.getBytes());

                ATOMIC_FILE.finishWrite(stream);
                LOGGER.v("config saved");
            } catch (Throwable tr) {
                LOGGER.w(tr, "can't save %s, restoring backup.", ATOMIC_FILE.getBaseFile());
                ATOMIC_FILE.failWrite(stream);
            }
        }
    }

    public static final int FLAG_GRANTED = 1 << 1;
    public static final int FLAG_DENIED = 1 << 2;
    public static final int FLAG_HIDDEN = 1 << 3;

    @SerializedName("version")
    public int version = LATEST_VERSION;

    @SerializedName("packages")
    public List<PackageEntry> packages = new ArrayList<>();

    public static class PackageEntry {

        @SerializedName("uid")
        public int uid;

        @SerializedName("package")
        public int packageName;

        @SerializedName("flags")
        public int flags;
    }

    private Config() {
    }

    public Config(@NonNull List<PackageEntry> packages) {
        this.version = LATEST_VERSION;
        this.packages = packages;
    }

    @Nullable
    public PackageEntry find(int uid) {
        for (Config.PackageEntry entry : packages) {
            if (uid == entry.uid) {
                return entry;
            }
        }
        return null;
    }
}
