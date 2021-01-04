package hidden;

import android.annotation.NonNull;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class HiddenApiBridge {

    public static ApplicationInfo PackageManager_getApplicationInfoAsUser(PackageManager packageManager, @NonNull String packageName, int flags, int userId) throws android.content.pm.PackageManager.NameNotFoundException {
        return packageManager.getApplicationInfoAsUser(packageName, flags, userId);
    }
}
