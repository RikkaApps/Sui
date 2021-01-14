package rikka.sui.server.config;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Config {

    public static final int LATEST_VERSION = 1;

    public static final int FLAG_ALLOWED = 1 << 1;
    public static final int FLAG_DENIED = 1 << 2;
    public static final int FLAG_HIDDEN = 1 << 3;
    public static final int MASK_PERMISSION = FLAG_ALLOWED | FLAG_DENIED | FLAG_HIDDEN;

    public int version = LATEST_VERSION;

    public List<PackageEntry> packages = new ArrayList<>();

    public static class PackageEntry {

        public final int uid;

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
