package $android.content.pm;

import android.content.pm.ApplicationInfo;

import java.util.List;

public class PackageManager {
    public static int MATCH_UNINSTALLED_PACKAGES;

    public ApplicationInfo getApplicationInfoAsUser(String packageName, int flags, int userId) throws android.content.pm.PackageManager.NameNotFoundException {
        throw new RuntimeException();
    }

    public PackageInfo getPackageInfoAsUser(String packageName, int flags, int userId) throws android.content.pm.PackageManager.NameNotFoundException {
        throw new RuntimeException();
    }

    public List<ApplicationInfo> getInstalledApplicationsAsUser(int flags, int userId) {
        throw new RuntimeException();
    }

    public List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        throw new RuntimeException();
    }
}
