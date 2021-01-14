package android.content.pm;


import java.util.List;

public abstract class PackageManager {

    public static class NameNotFoundException extends Exception {
        public NameNotFoundException() {
            throw new RuntimeException();
        }

        public NameNotFoundException(String name) {
            throw new RuntimeException();
        }
    }

    public ApplicationInfo getApplicationInfoAsUser(String packageName, int flags, int userId) throws NameNotFoundException {
        throw new RuntimeException();
    }

    public PackageInfo getPackageInfoAsUser(String packageName, int flags, int userId) throws NameNotFoundException {
        throw new RuntimeException();
    }

    public List<ApplicationInfo> getInstalledApplicationsAsUser(int flags, int userId) {
        throw new RuntimeException();
    }

    public List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId) {
        throw new RuntimeException();
    }
}
