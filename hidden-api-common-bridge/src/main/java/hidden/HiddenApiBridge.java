package hidden;

import android.annotation.NonNull;
import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;

public class HiddenApiBridge {

    public static ApplicationInfo PackageManager_getApplicationInfoAsUser(PackageManager packageManager, @NonNull String packageName, int flags, int userId) throws android.content.pm.PackageManager.NameNotFoundException {
        return packageManager.getApplicationInfoAsUser(packageName, flags, userId);
    }

    public static UserHandle createUserHandle(int userId) {
        return new UserHandle(userId);
    }

    public static ActivityThread ActivityThread_systemMain() {
        return ActivityThread.systemMain();
    }
}
