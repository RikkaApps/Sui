package android.content.pm;


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
}
