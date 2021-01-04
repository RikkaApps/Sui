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

    public static final int LATEST_VERSION = 1;

    public static final int FLAG_ALLOWED = 1 << 1;
    public static final int FLAG_DENIED = 1 << 2;
    public static final int FLAG_HIDDEN = 1 << 3;

    @SerializedName("version")
    public int version = LATEST_VERSION;

    @SerializedName("packages")
    public List<PackageEntry> packages = new ArrayList<>();

    public static class PackageEntry {

        @SerializedName("uid")
        public final int uid;

        @SerializedName("flags")
        public int flags;

        public PackageEntry(int uid, int flags) {
            this.uid = uid;
            this.flags = flags;
        }

        public boolean isAllowed() {
            return (flags & FLAG_ALLOWED) != 0;
        }

        public boolean isDenied() {
            return (flags & FLAG_DENIED) != 0;
        }

        public boolean isHidden() {
            return (flags & FLAG_HIDDEN) != 0;
        }
    }

    public Config() {
    }

    public Config(@NonNull List<PackageEntry> packages) {
        this.version = LATEST_VERSION;
        this.packages = packages;
    }
}
